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

/**
 *
 */
@SuppressWarnings("rawtypes")
public class SpreadsheetImportTemplateColumnPrespecifiedValue implements Comparable {
	
	Integer id;
	
	SpreadsheetImportTemplateColumn column;
	
	SpreadsheetImportTemplatePrespecifiedValue prespecifiedValue;
	
	String columnName;

	public SpreadsheetImportTemplateColumnPrespecifiedValue() {
	}	
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public SpreadsheetImportTemplateColumn getColumn() {
		return column;
	}
	
	public void setColumn(SpreadsheetImportTemplateColumn column) {
		this.column = column;
	}
	
	public SpreadsheetImportTemplatePrespecifiedValue getPrespecifiedValue() {
		return prespecifiedValue;
	}
	
	public void setPrespecifiedValue(SpreadsheetImportTemplatePrespecifiedValue prespecifiedValue) {
		this.prespecifiedValue = prespecifiedValue;
	}
	
	public String getColumnName() {
		return columnName;
	}
	
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public int compareTo(Object arg0) {
		SpreadsheetImportTemplateColumnPrespecifiedValue that = (SpreadsheetImportTemplateColumnPrespecifiedValue) arg0;
		if (getId() == null) {
			return 1;
		} else if (that.getId() == null) {
			return -1;
		} else {
			return getId().compareTo(that.getId());
		}
	}
	
	public String toString() {
		String s = "columnName=" + columnName;
		if (prespecifiedValue != null)
			s += ", prespecifiedvalue={" + prespecifiedValue.toString() + "}";
		return s;
	}

}
