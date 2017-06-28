package it.unibz.inf.ontop.mapping.transf;

import it.unibz.inf.ontop.mapping.Mapping;
import it.unibz.inf.ontop.model.DBMetadata;
import it.unibz.inf.ontop.ontology.ImmutableOntologyVocabulary;
import it.unibz.inf.ontop.owlrefplatform.core.dagjgrapht.TBoxReasoner;
import it.unibz.inf.ontop.pivotalrepr.tools.ExecutorRegistry;

public interface MissingDatatypeMappingExpander {

    Mapping inferMissingDatatypes(Mapping mapping, TBoxReasoner tBox, ImmutableOntologyVocabulary
            vocabulary, DBMetadata dbMetadata, ExecutorRegistry executorRegistry);
}
