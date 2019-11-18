/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.spreadsheetimport;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.openmrs.module.spreadsheetimport.objects.NameValue;

@SuppressWarnings("rawtypes")
public class SpreadsheetImportTemplatePrespecifiedValue implements Comparable {
	
	Integer id;
	
	SpreadsheetImportTemplate template;
	
	String tableDotColumn;
	
	String value;
	
	Set<SpreadsheetImportTemplateColumnPrespecifiedValue> columnPrespecifiedValues = new TreeSet<SpreadsheetImportTemplateColumnPrespecifiedValue>();
	
	public SpreadsheetImportTemplatePrespecifiedValue() {
	}
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public SpreadsheetImportTemplate getTemplate() {
		return template;
	}
	
	public void setTemplate(SpreadsheetImportTemplate template) {
		this.template = template;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getTableDotColumn() {
		return tableDotColumn;
	}
	
	public void setTableDotColumn(String tableDotColumn) {
		this.tableDotColumn = tableDotColumn;
	}
	
	public Set<SpreadsheetImportTemplateColumnPrespecifiedValue> getColumnPrespecifiedValues() {
		return columnPrespecifiedValues;
	}
	
	public void setColumnPrespecifiedValues(Set<SpreadsheetImportTemplateColumnPrespecifiedValue> columnPrespecifiedValues) {
		this.columnPrespecifiedValues = columnPrespecifiedValues;
	}

	public int compareTo(Object arg0) {
		SpreadsheetImportTemplatePrespecifiedValue that = (SpreadsheetImportTemplatePrespecifiedValue) arg0;
		if (getId() == null) {
			return 1;
		} else if (that.getId() == null) {
			return -1;
		} else {
			return getId().compareTo(that.getId());
		}
	}
	
	public Set<SpreadsheetImportTemplateColumn> getColumns() {
		Set<SpreadsheetImportTemplateColumn> result = new TreeSet<SpreadsheetImportTemplateColumn>();
		for (SpreadsheetImportTemplateColumnPrespecifiedValue cpv : columnPrespecifiedValues) {
			result.add(cpv.getColumn());
		}
		return result;
	}

	/**
	 * Pretty tableName associated with this pre-specified value
	 */
	public String getPrettyTableName() {
		int idx = tableDotColumn.indexOf('.');
		String table = tableDotColumn.substring(0, idx);
		return DatabaseBackend.makePrettyName(table);
	}

	/**
	 * Map of name to idValue for tableName
	 * 
	 * @throws Exception
	 */
	public List<NameValue> getMapNameToAllowedValue() throws Exception {
		int idx = tableDotColumn.indexOf('.');
		String table = tableDotColumn.substring(0, idx);
		return DatabaseBackend.getMapNameToAllowedValue(table);
	}
	
	public String toString() {
		return "tableDotColumn=" + tableDotColumn + ", value=" + value;
	}
}
