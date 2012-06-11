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

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.SessionFactory;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.spreadsheetimport.service.SpreadsheetImportService;
import org.springframework.beans.factory.annotation.Autowired;

public class SpreadsheetImportTemplate {

	@Autowired
	private SessionFactory sessionFactory;
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	Integer id;
	
	String name;
	
	String description;
	
	// Citigo addition starts
	boolean encounter;
	
	String targetForm;
	// Citigo addition ends
	
	Date created;
	
	Date modified;
	
	User creator;
	
	User modifiedBy;
	
	private String test;

	private Map<UniqueImport, Set<SpreadsheetImportTemplateColumn>> rowDataTemp = null;
	
	Set<SpreadsheetImportTemplateColumn> columns = new TreeSet<SpreadsheetImportTemplateColumn>();
	
	Set<SpreadsheetImportTemplatePrespecifiedValue> prespecifiedValues = new TreeSet<SpreadsheetImportTemplatePrespecifiedValue>();
	
	public SpreadsheetImportTemplate() {
	}
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(final String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(final String description) {
		this.description = description;
	}
	
	// Citigo addition starts
	
	public boolean isEncounter() {
		return encounter;
	}

	public void setEncounter(boolean encounter) {
		this.encounter = encounter;
	}

	public String getTargetForm() {
		return targetForm;
	}

	public void setTargetForm(String targetForm) {
		this.targetForm = targetForm;
	}
	
	// Citigo addition ends

	public Date getCreated() {
		return created;
	}
	
	public void setCreated(final Date created) {
		this.created = created;
	}
	
	public Date getModified() {
		return modified;
	}
	
	public void setModified(final Date modified) {
		this.modified = modified;
	}
	
	public User getCreator() {
		return creator;
	}
	
	public void setCreator(final User creator) {
		this.creator = creator;
	}
	
	public User getModifiedBy() {
		return modifiedBy;
	}
	
	public void setModifiedBy(final User modifiedBy) {
		this.modifiedBy = modifiedBy;
	}
	
	public Set<SpreadsheetImportTemplateColumn> getColumns() {
		return columns;
	}
	
	public void setColumns(Set<SpreadsheetImportTemplateColumn> columns) {
		this.columns = columns;
	}
	
	public Set<SpreadsheetImportTemplatePrespecifiedValue> getPrespecifiedValues() {
		return prespecifiedValues;
	}
	
	public void setPrespecifiedValues(Set<SpreadsheetImportTemplatePrespecifiedValue> prespecifiedValues) {
		this.prespecifiedValues = prespecifiedValues;
	}
	
	//
	// Helpers
	//
	
	public void clearPrespecifiedValues() {
		for (SpreadsheetImportTemplatePrespecifiedValue prespecifiedValue : prespecifiedValues) {
			prespecifiedValue.getColumnPrespecifiedValues().clear();
		}
		for (SpreadsheetImportTemplateColumn column : columns) {
			column.getColumnPrespecifiedValues().clear();
		}
		prespecifiedValues.clear();
	}
	
	public void clearColumnColumns() {
		for (SpreadsheetImportTemplateColumn column : columns) {
			column.getColumnColumnsImportBefore().clear();
			column.getColumnColumnsImportAfter().clear();
		}
	}
	
	public List<String> getColumnNamesAsList() {
		List<String> result = new ArrayList<String>();
		for (SpreadsheetImportTemplateColumn column : columns) {
			result.add(column.getName());
		}
		return result;
	}
	
	public Map<String, Set<UniqueImport>> getMapOfColumnTablesToUniqueImportSet() {
		Map<String, Set<UniqueImport>> result = new HashMap<String, Set<UniqueImport>>();
		
		for (SpreadsheetImportTemplateColumn column : getColumns()) {
			String key = column.getTableName();
			UniqueImport uniqueImport = new UniqueImport(column);
			if (result.containsKey(key)) {
				result.get(key).add(uniqueImport);
			} else {
				Set<UniqueImport> set = new TreeSet<UniqueImport>();
				set.add(uniqueImport);
				result.put(key, set);
			}
		}
		
		return result;
	}
	
	public Map<UniqueImport, Set<SpreadsheetImportTemplateColumn>> getMapOfUniqueImportToColumnSet() {
		Map<UniqueImport, Set<SpreadsheetImportTemplateColumn>> result = new TreeMap<UniqueImport, Set<SpreadsheetImportTemplateColumn>>();
		
		for (SpreadsheetImportTemplateColumn column : getColumns()) {
			UniqueImport key = new UniqueImport(column);
			if (result.containsKey(key)) {
				result.get(key).add(column);
			} else {
				Set<SpreadsheetImportTemplateColumn> set = new TreeSet<SpreadsheetImportTemplateColumn>();
				set.add(column);
				result.put(key, set);
			}
		}
		
		return result;
	}
	
	private class SpreadsheetImportTemplateColumnComparatorSortByImportIdx implements Comparator<SpreadsheetImportTemplateColumn> {
		
		public int compare(SpreadsheetImportTemplateColumn o1, SpreadsheetImportTemplateColumn o2) {
			if (o1 != null && o2 != null && o1.getImportIdx() != null && o2.getImportIdx() != null) {
				return o1.getImportIdx().compareTo(o2.getImportIdx());
			}
			return 0;
		}
	}
	
	private Set<SpreadsheetImportTemplateColumn> getColumnsSortedByImportIdx() {
		Set<SpreadsheetImportTemplateColumn> result = new TreeSet<SpreadsheetImportTemplateColumn>(
		        new SpreadsheetImportTemplateColumnComparatorSortByImportIdx());
		result.addAll(getColumns());
		return result;
	}
	
	public Map<UniqueImport, Set<SpreadsheetImportTemplateColumn>> getMapOfUniqueImportToColumnSetSortedByImportIdx() {
		Map<UniqueImport, Set<SpreadsheetImportTemplateColumn>> result = new LinkedHashMap<UniqueImport, Set<SpreadsheetImportTemplateColumn>>();
		
		for (SpreadsheetImportTemplateColumn column : getColumnsSortedByImportIdx()) {
			UniqueImport key = new UniqueImport(column);
			if (result.containsKey(key)) {
				result.get(key).add(column);
			} else {
				Set<SpreadsheetImportTemplateColumn> set = new TreeSet<SpreadsheetImportTemplateColumn>();
				set.add(column);
				result.put(key, set);
			}
		}
		
		return result;
	}

	public String getTest() {
		return test;
	}

	public void setTest(String test) {
		this.test = test;
	}

	public Map<UniqueImport, Set<SpreadsheetImportTemplateColumn>> getRowDataTemp() {
		return rowDataTemp;
	}

	public void setRowDataTemp(Map<UniqueImport, Set<SpreadsheetImportTemplateColumn>> rowDataTemp) {
		this.rowDataTemp = rowDataTemp;
	}
}
