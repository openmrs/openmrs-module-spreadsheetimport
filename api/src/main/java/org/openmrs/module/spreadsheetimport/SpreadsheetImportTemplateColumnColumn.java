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
public class SpreadsheetImportTemplateColumnColumn implements Comparable {
	
	Integer id;
	
	SpreadsheetImportTemplateColumn columnImportFirst;
	
	SpreadsheetImportTemplateColumn columnImportNext;
	
	String columnName;
	
	public SpreadsheetImportTemplateColumnColumn() {
	}
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public SpreadsheetImportTemplateColumn getColumnImportFirst() {
		return columnImportFirst;
	}
	
	public void setColumnImportFirst(SpreadsheetImportTemplateColumn columnImportFirst) {
		this.columnImportFirst = columnImportFirst;
	}
	
	public SpreadsheetImportTemplateColumn getColumnImportNext() {
		return columnImportNext;
	}
	
	public void setColumnImportNext(SpreadsheetImportTemplateColumn columnImportNext) {
		this.columnImportNext = columnImportNext;
	}
	
	public String getColumnName() {
		return columnName;
	}
	
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	
	public int compareTo(Object arg0) {
		SpreadsheetImportTemplateColumnColumn that = (SpreadsheetImportTemplateColumnColumn) arg0;
		if (getId() == null) {
			return 1;
		} else if (that.getId() == null) {
			return -1;
		} else {
			return getId().compareTo(that.getId());
		}
	}
	
	public String toString() {
		String s = "columnName="+columnName;
		if (columnImportFirst != null)
			s += ", columnImportFirst={" + columnImportFirst.toString() + "}";
		if (columnImportNext != null)
			s += ", columnImportNext={" + columnImportNext.toString() + "}";
		return s;		
	}
}
