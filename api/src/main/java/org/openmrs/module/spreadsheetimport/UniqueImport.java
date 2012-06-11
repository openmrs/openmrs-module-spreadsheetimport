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
public class UniqueImport implements Comparable {
	
	String tableName;
	
	Integer datasetIdx;
	
	public UniqueImport(String table, Integer datasetIdx) {
		super();
		this.tableName = table;
		this.datasetIdx = datasetIdx;
	}
	
	public UniqueImport(SpreadsheetImportTemplateColumn column) {
		this(column.getTableName(), column.getDatasetIdx());
	}
	
	public String getTableName() {
		return tableName;
	}
	
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
	public Integer getDatasetIdx() {
		return datasetIdx;
	}
	
	public void setDatasetIdx(Integer datasetIdx) {
		this.datasetIdx = datasetIdx;
	}
	
	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof UniqueImport) {
			UniqueImport that = (UniqueImport) arg0;
			if (that.tableName.equals(tableName)) {
				if (datasetIdx == null && that.datasetIdx == null) {
					return true;
				} else if (datasetIdx == null || that.datasetIdx == null) {
					return false;
				} else if (datasetIdx.equals(that.datasetIdx)) {
					return true;
				} else {
					return false;
				}
			}
		}
		return super.equals(arg0);
	}
	
	@Override
	public int hashCode() {
		int result = 1;
		result = result * 31 + (tableName == null ? 0 : tableName.hashCode());
		result = result * 31 + (datasetIdx == null ? 0 : datasetIdx.hashCode());
		return result;
	}
	
	public int compareTo(Object o) {
		UniqueImport that = (UniqueImport) o;
		if (that.tableName.equals(this.tableName)) {
			if (this.datasetIdx == null && that.datasetIdx == null) {
				return 0;
			} else if (this.datasetIdx == null && that.datasetIdx != null) {
				return -1;
			} else if (this.datasetIdx != null && that.datasetIdx == null) {
				return 1;
			} else {
				return this.datasetIdx.compareTo(that.datasetIdx);
			}
		} else {
			return this.tableName.compareTo(that.tableName);
		}
	}
	
	public String toString() {
		return "tableName=" + tableName + ", datasetIdx=" + (datasetIdx==null ? "-1" : datasetIdx.toString());
	}
}
