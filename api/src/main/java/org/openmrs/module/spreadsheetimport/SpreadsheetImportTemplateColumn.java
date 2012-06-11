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

import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings("rawtypes")
public class SpreadsheetImportTemplateColumn implements Comparable {
	
	Integer id;
	
	SpreadsheetImportTemplate template;
	
	String name;
	
	String tableDotColumn;
	
	Integer datasetIdx;
	
	Integer importIdx;
	
	Boolean disallowDuplicateValue;
	
	Set<SpreadsheetImportTemplateColumnPrespecifiedValue> columnPrespecifiedValues = new TreeSet<SpreadsheetImportTemplateColumnPrespecifiedValue>();
	
	Set<SpreadsheetImportTemplateColumnColumn> columnColumnsImportBefore = new TreeSet<SpreadsheetImportTemplateColumnColumn>();
	
	Set<SpreadsheetImportTemplateColumnColumn> columnColumnsImportAfter = new TreeSet<SpreadsheetImportTemplateColumnColumn>();
	
	public SpreadsheetImportTemplateColumn() {
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
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getTableDotColumn() {
		return tableDotColumn;
	}
	
	public void setTableDotColumn(String tableDotColumn) {
		this.tableDotColumn = tableDotColumn;
	}
	
	public Integer getDatasetIdx() {
		return datasetIdx;
	}
	
	public void setDatasetIdx(Integer datasetIdx) {
		this.datasetIdx = datasetIdx;
	}
	
	public Integer getImportIdx() {
		return importIdx;
	}
	
	public void setImportIdx(Integer importIdx) {
		this.importIdx = importIdx;
	}
	
	public Boolean getDisallowDuplicateValue() {
		return disallowDuplicateValue;
	}
	
	public void setDisallowDuplicateValue(Boolean disallowDuplicateValue) {
		this.disallowDuplicateValue = disallowDuplicateValue;
	}
	
	public Set<SpreadsheetImportTemplateColumnPrespecifiedValue> getColumnPrespecifiedValues() {
		return columnPrespecifiedValues;
	}
	
	public void setColumnPrespecifiedValues(Set<SpreadsheetImportTemplateColumnPrespecifiedValue> columnPrespecifiedValues) {
		this.columnPrespecifiedValues = columnPrespecifiedValues;
	}
	
	public Set<SpreadsheetImportTemplateColumnColumn> getColumnColumnsImportBefore() {
		return columnColumnsImportBefore;
	}
	
	public void setColumnColumnsImportBefore(Set<SpreadsheetImportTemplateColumnColumn> columnColumnsImportBefore) {
		this.columnColumnsImportBefore = columnColumnsImportBefore;
	}
	
	public Set<SpreadsheetImportTemplateColumnColumn> getColumnColumnsImportAfter() {
		return columnColumnsImportAfter;
	}
	
	public void setColumnColumnsImportAfter(Set<SpreadsheetImportTemplateColumnColumn> columnColumnsImportAfter) {
		this.columnColumnsImportAfter = columnColumnsImportAfter;
	}
	
	public int compareTo(Object arg0) {
		SpreadsheetImportTemplateColumn that = (SpreadsheetImportTemplateColumn) arg0;
		if (getId() == null) {
			return 1;
		} else if (that.getId() == null) {
			return -1;
		} else {
			return getId().compareTo(that.getId());
		}
	}
	
	public String getTableName() {
		int idx = tableDotColumn.indexOf('.');
		return tableDotColumn.substring(0, idx);
	}
	
	public String getColumnName() {
		int idx = tableDotColumn.indexOf('.');
		return tableDotColumn.substring(idx + 1);
	}
	
	/**
	 * Retrieve pretty description of dbTableDotColumn
	 * 
	 * @return
	 */
	public String getData() {
		return DatabaseBackend.makePrettyTableDotColumn(tableDotColumn);
	}
	
	/**
	 * Used during import.
	 */
	Object value;
	
	String generatedKey;
	
	public Object getValue() {
		return value;
	}
	
	public void setValue(Object value) {
		this.value = value;
	}
	
	public String getGeneratedKey() {
		return generatedKey;
	}
	
	public void setGeneratedKey(String generatedKey) {
		this.generatedKey = generatedKey;
	}
	
	public String toString() {
		return "name=" + name + ", tableDotColumn=" + tableDotColumn + ", datasetIdx=" + (datasetIdx==null ? "-1" : datasetIdx.toString()) + ", importIdx=" + (importIdx==null ? "-1" : importIdx.toString()) + ", disallowDuplicate=" + (disallowDuplicateValue==null ? "false" : disallowDuplicateValue.toString()); 
	}
}
