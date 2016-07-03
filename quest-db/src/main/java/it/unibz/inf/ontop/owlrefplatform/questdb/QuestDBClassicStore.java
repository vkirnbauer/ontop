package it.unibz.inf.ontop.owlrefplatform.questdb;

/*
 * #%L
 * ontop-quest-db
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import it.unibz.inf.ontop.model.OBDAException;
import it.unibz.inf.ontop.model.OBDAModel;
import it.unibz.inf.ontop.ontology.OntologyVocabulary;
import it.unibz.inf.ontop.owlapi.OWLAPIABoxIterator;
import it.unibz.inf.ontop.owlapi.OWLAPITranslatorUtility;
import it.unibz.inf.ontop.owlrefplatform.core.IQuest;
import it.unibz.inf.ontop.owlrefplatform.core.IQuestConnection;
import it.unibz.inf.ontop.owlrefplatform.core.QuestConstants;
import it.unibz.inf.ontop.owlrefplatform.core.QuestPreferences;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.Dataset;
import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.BasicParserSettings;
import org.openrdf.rio.helpers.RDFHandlerBase;

import it.unibz.inf.ontop.model.impl.OBDAVocabulary;
import it.unibz.inf.ontop.ontology.Assertion;
import it.unibz.inf.ontop.ontology.Ontology;
import it.unibz.inf.ontop.ontology.OntologyFactory;
import it.unibz.inf.ontop.ontology.impl.OntologyFactoryImpl;
import it.unibz.inf.ontop.owlrefplatform.core.abox.QuestMaterializer;
import it.unibz.inf.ontop.owlrefplatform.core.abox.RDBMSSIRepositoryManager;
import it.unibz.inf.ontop.owlrefplatform.core.execution.SIQuestStatement;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * An instance of Store that encapsulates all the functionality needed for a
 * "classic" store.
 */
public class QuestDBClassicStore extends QuestDBAbstractStore {

	// TODO all this needs to be refactored later to allow for transactions,
	// autocommit enable/disable, clients ids, etc

	private static final long serialVersionUID = 2495624993519521937L;

	private static Logger log = LoggerFactory.getLogger(QuestDBClassicStore.class);

	protected transient OWLOntologyManager man = OWLManager.createOWLOntologyManager();

	private OntologyFactory ofac = OntologyFactoryImpl.getInstance();
	
	private Set<OWLOntology> closure;

	private IQuest questInstance;

	public QuestDBClassicStore(String name, java.net.URI tboxFile, QuestPreferences config) throws Exception {
		super(name, config);
		Ontology tbox = readOntology(tboxFile.toASCIIString());
		setup(tbox, config);
	}
	
	public QuestDBClassicStore(String name, String tboxFile, QuestPreferences config) throws Exception {
		super(name, config);
		Ontology tbox = null;
		if (tboxFile == null) {
			OntologyVocabulary voc = ofac.createVocabulary();
			tbox = ofac.createOntology(voc);
		} else {
			tbox = readOntology(tboxFile);
		}
		setup(tbox, config);
	}
	
	private Ontology readOntology(String tboxFile) throws Exception {
		File f = new File(tboxFile);
		OWLOntologyIRIMapper iriMapper = new AutoIRIMapper(f.getParentFile(), false);
		man.addIRIMapper(iriMapper);
		
		OWLOntology owlontology = null;
		if (tboxFile.contains("file:")) {
			owlontology = man.loadOntologyFromOntologyDocument(new URL(tboxFile).openStream());
		} else {
			owlontology = man.loadOntologyFromOntologyDocument(new File(tboxFile));
		}
		closure = man.getImportsClosure(owlontology);
		return OWLAPITranslatorUtility.mergeTranslateOntologies(closure);
	}


	public QuestDBClassicStore(String name, Dataset data, QuestPreferences config) throws Exception {
		super(name, config);
		Ontology tbox = getTBox(data);
		setup(tbox, config);
	}

	
	private void setup(Ontology onto, QuestPreferences config) throws Exception {
		if (config == null) {
			Properties p = new Properties();
			p.setProperty(QuestPreferences.ABOX_MODE, QuestConstants.CLASSIC);
			config = new QuestPreferences(p);
		}

		if (!config.getProperty(QuestPreferences.ABOX_MODE).equals(QuestConstants.CLASSIC)) {
			throw new IllegalArgumentException("A classic repository must be created with the CLASSIC flag in the configuration.");
		}
		createInstance(onto, config);
	}

	private void createInstance(Ontology tbox, QuestPreferences config) throws Exception {
        questInstance = getComponentFactory().create(tbox, null, null, config);

		questInstance.setupRepository();
		
		final boolean bObtainFromOntology = config.getBoolean(QuestPreferences.OBTAIN_FROM_ONTOLOGY);
		final boolean bObtainFromMappings = config.getBoolean(QuestPreferences.OBTAIN_FROM_MAPPINGS);
		IQuestConnection conn = questInstance.getNonPoolConnection();
        //TODO: avoid this cast
		SIQuestStatement st = conn.createSIStatement();
		if (bObtainFromOntology) {
			// Retrieves the ABox from the ontology file.
			log.debug("Loading data from Ontology into the database");
			OWLAPIABoxIterator aBoxIter = new OWLAPIABoxIterator(closure, questInstance.getVocabulary());
			int count = st.insertData(aBoxIter, 5000, 500);
			log.debug("Inserted {} triples from the ontology.", count);
		}
		if (bObtainFromMappings) {
			// Retrieves the ABox from the target database via mapping.
			log.debug("Loading data from Mappings into the database");
			OBDAModel obdaModelForMaterialization = questInstance.getOBDAModel();
			obdaModelForMaterialization.getOntologyVocabulary().merge(tbox.getVocabulary());
			
			QuestMaterializer materializer = new QuestMaterializer(obdaModelForMaterialization, false);
			Iterator<Assertion> assertionIter = materializer.getAssertionIterator();
			int count = st.insertData(assertionIter, 5000, 500);
			materializer.disconnect();
			log.debug("Inserted {} triples from the mappings.", count);
		}
//		st.createIndexes();
		st.close();
		if (!conn.getAutoCommit())
			conn.commit();
		
		//questInstance.updateSemanticIndexMappings();

		log.debug("Store {} has been created successfully", name);
	}

	public void saveState(String storePath) throws IOException {
		// NO-OP
	}

	public QuestDBClassicStore restore(String storePath) throws IOException {	
		return this;
	}

	@Override
	public QuestPreferences getPreferences() {
		return questInstance.getPreferences();
	}

	public IQuestConnection getQuestConnection() {
		IQuestConnection conn = null;
		try {
			conn = questInstance.getConnection();
		} catch (OBDAException e) {
			e.printStackTrace();
		}
		return conn;
	}

	private Ontology getTBox(Dataset dataset) throws Exception {
		// Merge default and named graphs to filter duplicates
		Set<URI> graphURIs = new HashSet<>();
		graphURIs.addAll(dataset.getDefaultGraphs());
		graphURIs.addAll(dataset.getNamedGraphs());

		OntologyVocabulary vb = ofac.createVocabulary();

		for (URI graphURI : graphURIs) {
			Ontology o = getOntology(graphURI, graphURI);
			vb.merge(o.getVocabulary());
			
			// TODO: restore copying ontology axioms (it was copying from result into result, at least since July 2013)
			
			//for (SubPropertyOfAxiom ax : result.getSubPropertyAxioms()) 
			//	result.add(ax);
			//for (SubClassOfAxiom ax : result.getSubClassAxioms()) 
			//	result.add(ax);	
		}
		Ontology result = ofac.createOntology(vb);

		return result;
	}

	private Ontology getOntology(URI graphURI, Resource context) throws Exception {
		RDFFormat rdfFormat = Rio.getParserFormatForFileName(graphURI.toString(), RDFFormat.TURTLE);
		RDFParser rdfParser = Rio.createParser(rdfFormat, ValueFactoryImpl.getInstance());
		ParserConfig config = rdfParser.getParserConfig();

		// To emulate DatatypeHandling.IGNORE 
		config.addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES);
		config.addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
		config.addNonFatalError(BasicParserSettings.NORMALIZE_DATATYPE_VALUES);
//		rdfParser.setVerifyData(false);
//		rdfParser.setDatatypeHandling(DatatypeHandling.IGNORE);
//		rdfParser.setPreserveBNodeIDs(true);

		RDFTBoxReader reader = new RDFTBoxReader();
		rdfParser.setRDFHandler(reader);

		URL graphURL = new URL(graphURI.toString());
		InputStream in = graphURL.openStream();
		try {
			rdfParser.parse(in, graphURI.toString());
		} finally {
			in.close();
		}
		return reader.getOntology();
	}

	public class RDFTBoxReader extends RDFHandlerBase {
		private OntologyFactory ofac = OntologyFactoryImpl.getInstance();
		private OntologyVocabulary vb = ofac.createVocabulary();

		public Ontology getOntology() {
			return ofac.createOntology(vb);
		}

		@Override
		public void handleStatement(Statement st) throws RDFHandlerException {
			URI pred = st.getPredicate();
			Value obj = st.getObject();
			if (obj instanceof Literal) {
				String dataProperty = pred.stringValue();
				vb.createDataProperty(dataProperty);
			} 
			else if (pred.stringValue().equals(OBDAVocabulary.RDF_TYPE)) {
				String className = obj.stringValue();
				vb.createClass(className);
			} 
			else {
				String objectProperty = pred.stringValue();
				vb.createObjectProperty(objectProperty);
			}

		/* Roman 10/08/15: recover?
			Axiom axiom = getTBoxAxiom(st);
			ontology.addAssertionWithCheck(axiom);
		*/
		}

	}

	/**
	 * TODO: Import from V1. Keep it?
	 */
	public RDBMSSIRepositoryManager getSemanticIndexRepository() {
		return questInstance.getOptionalSemanticIndexRepository().get();
	}

}
