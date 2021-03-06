package it.unibz.inf.ontop.protege.gui.component;

/*
 * #%L
 * ontop-protege
 * %%
 * Copyright (C) 2009 - 2013 KRDB Research Centre. Free University of Bozen Bolzano.
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

import it.unibz.inf.ontop.model.IriConstants;
import it.unibz.inf.ontop.model.term.functionsymbol.Predicate;
import it.unibz.inf.ontop.protege.gui.IconLoader;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.util.List;

import static it.unibz.inf.ontop.model.OntopModelSingletons.TYPE_FACTORY;

public class DataTypeComboBox extends JComboBox {

	private static final long serialVersionUID = 1L;

	private static final Predicate[] SUPPORTED_DATATYPES = getQuestDataTypePredicates();
	
	public DataTypeComboBox() {
		super(SUPPORTED_DATATYPES);
		setRenderer(new DataTypeRenderer());
		setPreferredSize(new Dimension(130, 23));
		setSelectedIndex(-1);
	}
	
	private static Predicate[] getQuestDataTypePredicates() {
		
		List<Predicate> prediacteList = TYPE_FACTORY.getDatatypePredicates();
		
		int length = prediacteList.size() + 1;
		Predicate[] dataTypes = new Predicate[length];
		dataTypes[0] = null;
		System.arraycopy(prediacteList.toArray(), 0, dataTypes, 1, prediacteList.size());
		return dataTypes;
	}

	class DataTypeRenderer extends BasicComboBoxRenderer {
		
		private static final long serialVersionUID = 1L;

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if (value == null) {
				setText("<Undefined data type>");
				setIcon(null);
			} else {
				if (value instanceof Predicate) {
					Predicate item = (Predicate) value;
					String name = item.toString();
					if (name.contains(IriConstants.NS_XSD)) {
						name = name.replace(IriConstants.NS_XSD, "xsd:");
					} else if (name.contains(IriConstants.NS_RDFS)) {
						name = name.replace(IriConstants.NS_RDFS, "rdfs:");
					}
					setText(name);
					setIcon(IconLoader.getImageIcon("images/datarange.png"));
				}
			}
			return this;
		}
	}
}
