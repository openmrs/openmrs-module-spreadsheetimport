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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.spreadsheetimport.objects.NameValue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class DatabaseBackend {
	
	/** Logger for this class and subclasses */
	protected static final Log log = LogFactory.getLog(SpreadsheetImportUtil.class);
	
	/**
	 * Make name pretty
	 */
	public static String makePrettyName(String name) {
		String result = "";
		boolean capitalizeNext = true;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (c == '_') {
				result += " ";
				capitalizeNext = true;
			} else if (capitalizeNext) {
				result += Character.toUpperCase(c);
				capitalizeNext = false;
			} else {
				result += c;
			}
		}
		return result;
	}
	
	/**
	 * Convert table_name.column_name to Table Name: Column Name
	 */
	public static String makePrettyTableDotColumn(String tableDotColumn) {
		int index = tableDotColumn.indexOf(".");
		String table = tableDotColumn.substring(0, index);
		String column = tableDotColumn.substring(index + 1);
		return makePrettyName(table) + ": " + makePrettyName(column);
	}
	
	/**
	 * Map: key = tableName.column, value = Table: Column
	 */
	private static Map<String, String> tableColumnMap = null;
	
	private static Map<String, List<String>> tableColumnListMap = null;
	
	public static Map<String, List<String>> getTableColumnListMap() throws Exception {
		if (tableColumnListMap == null)
			reverseEngineerDatabaseTable();
		return tableColumnListMap;
	}
	
	public static Map<String, String> getTableColumnMap() throws Exception {
		if (tableColumnMap == null)
			reverseEngineerDatabaseTable();
		return tableColumnMap;
	}
	
	private static void reverseEngineerDatabaseTable() throws Exception {
		tableColumnMap = new TreeMap<String, String>();
		tableColumnListMap = new TreeMap<String, List<String>>();
		Connection conn = null;
		Exception exception = null;
		try {
			// Connect to db
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			
			Properties p = Context.getRuntimeProperties();
			String url = p.getProperty("connection.url");
			
			conn = DriverManager.getConnection(url, p.getProperty("connection.username"),
			    p.getProperty("connection.password"));
			
			// All tables
			DatabaseMetaData dmd = conn.getMetaData();
			ResultSet rs = dmd.getTables(null, null, "", null);
			while (rs.next()) {
				String tableName = rs.getString("TABLE_NAME");
				
				// All columns
				List<String> columnNames = new ArrayList<String>();
				ResultSet rsColumns = dmd.getColumns(null, null, tableName, "");
				while (rsColumns.next()) {
					columnNames.add(rsColumns.getString("COLUMN_NAME"));
				}
				rsColumns.close();								
						
//				// Remove imported keys
				ResultSet rsImportedKeys = dmd.getImportedKeys(null, null, tableName);
				while (rsImportedKeys.next()) {
					String columnName = rsImportedKeys.getString("FKCOLUMN_NAME");
					if (columnNames.contains(columnName) && "obs".equalsIgnoreCase(tableName) && !"value_coded".equalsIgnoreCase(columnName)) { // hack: only allow obs.value_coded to go through
						columnNames.remove(columnName);
					}
				}
				rsImportedKeys.close();
				
				List<String> clonedColumns = new ArrayList<String>();
				clonedColumns.addAll(columnNames);				

				// Add to map
				for (String columnName : clonedColumns) {					
					String tableDotColumn = tableName + "." + columnName;
					tableColumnMap.put(tableDotColumn, makePrettyTableDotColumn(tableDotColumn));
				}
				
				// Remove primary key
				ResultSet rsPrimaryKeys = dmd.getPrimaryKeys(null, null, tableName);
				while (rsPrimaryKeys.next()) {
					String columnName = rsPrimaryKeys.getString("COLUMN_NAME");
					if (columnNames.contains(columnName)) {
						columnNames.remove(columnName);
					}
				}
				rsPrimaryKeys.close();
				
				tableColumnListMap.put(tableName, columnNames);

			}
		}
		catch (Exception e) {
			log.debug(e.toString());
			exception = e;
		}
		finally {
			if (conn != null) {
				try {
					conn.close();
				}
				catch (Exception e) {}
			}
		}
		
		if (exception != null) {
			throw exception;
		}
	}
	
	
	
	public static List<NameValue> getMapNameToAllowedValue(String tableName) throws Exception {
		List<NameValue> retVal = new ArrayList<NameValue>();
		
//		Map<String, String> result = new LinkedHashMap<String, String>();
		Connection conn = null;
		Statement s = null;
		Exception exception = null;
		try {
			// Connect to db
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			
			Properties p = Context.getRuntimeProperties();
			String url = p.getProperty("connection.url");
			
			conn = DriverManager.getConnection(url, p.getProperty("connection.username"),
			    p.getProperty("connection.password"));
			
			s = conn.createStatement();
			
			// Primary key
			String primaryKey = tableName + "_id"; // Guess
			DatabaseMetaData dmd = conn.getMetaData();
			ResultSet rsPrimaryKeys = dmd.getPrimaryKeys(null, null, tableName);
			if (rsPrimaryKeys.next()) {
				primaryKey = rsPrimaryKeys.getString("COLUMN_NAME");
			}
			rsPrimaryKeys.close();
			
			ResultSet rs = null;

			// Try if table is person, then look for person_name
			if ("person".equals(tableName)) {
				try {
					rs = s.executeQuery("SELECT CONCAT(given_name, ' ', family_name) name,  `person_name`.`person_id` primary_key FROM  `users` INNER JOIN `person_name` on `users`.`person_id` = `person_name`.`person_id` INNER JOIN `user_role` on `users`.`user_id` = `user_role`.`user_id` WHERE `user_role`.`role` = 'Provider'");
				}
				catch (Exception e) {
					log.debug(e.toString());
				}
			}

			// Try if table is concept
			// concept_name TODO: provide a means to filter only concepts to be used. This slows down page rendering for a large concept dictionary
			if (rs == null && "concept".equals(tableName)) {
				try {
					rs = s.executeQuery("select concept_id, concept_id from concept_name where locale='en' and concept_name_type='FULLY_SPECIFIED' and concept_id in(select distinct concept_id from obs) order by concept_id asc");
				}
				catch (Exception e) {
					log.debug(e.toString());
				}
			}
						
			// Try 1: name field in tableName
			if (rs == null) {
				try {
					rs = s.executeQuery("select name, " + primaryKey + " from " + tableName + " order by name");
				}
				catch (Exception e) {
					log.debug(e.toString());
				}			
			}
			
			// Try 2: name field in table_name
			if (rs == null) {
				try {
					rs = s.executeQuery("select name, " + primaryKey + " from " + tableName + "_name order by name");
				}
				catch (Exception e) {
					log.debug(e.toString());
				}
			}						
			
			// Try 3: just use table_id as both key and value
			if (rs == null) {
				rs = s.executeQuery("select " + primaryKey + ", " + primaryKey + " from " + tableName);
			}
			
			while (rs.next()) {
				NameValue nameValue = new NameValue();
				nameValue.setName(rs.getString(1));
				nameValue.setValue(rs.getString(2));
				retVal.add(nameValue);
			}
			rs.close();
		}
		catch (Exception e) {
			log.debug(e.toString());
			exception = e;
		}
		finally {
			if (s != null) {
				try {
					s.close();
				}
				catch (Exception e) {}
			}
			if (conn != null) {
				try {
					conn.close();
				}
				catch (Exception e) {}
			}
		}
		
		if (exception != null) {
			throw exception;
		} else {
			return retVal;
		}
	}
	
	public static Map<String, String> getMapOfImportedKeyTableNameToColumnNamesForTable(String tableName) throws Exception {
		Map<String, String> result = new HashMap<String, String>();
		Connection conn = null;
		Exception exception = null;
		try {
			// Connect to db
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			
			Properties p = Context.getRuntimeProperties();
			String url = p.getProperty("connection.url");
			
			conn = DriverManager.getConnection(url, p.getProperty("connection.username"),
			    p.getProperty("connection.password"));
			
			// Not NULLable columns
			DatabaseMetaData dmd = conn.getMetaData();
			List<String> columnNames = new ArrayList<String>();
			ResultSet rsColumns = dmd.getColumns(null, null, tableName, "");
			while (rsColumns.next()) {
				if (!rsColumns.getString("IS_NULLABLE").equals("YES")) {
					columnNames.add(rsColumns.getString("COLUMN_NAME"));
				}
			}
			rsColumns.close();
			
			// Imported keys
			ResultSet rsImportedKeys = dmd.getImportedKeys(null, null, tableName);
			while (rsImportedKeys.next()) {
				String columnName = rsImportedKeys.getString("FKCOLUMN_NAME");
				if (columnNames.contains(columnName)) {
					result.put(rsImportedKeys.getString("PKTABLE_NAME"), columnName);
				}
			}
			rsImportedKeys.close();
		}
		catch (Exception e) {
			log.debug(e.toString());
			exception = e;
		}
		finally {
			if (conn != null) {
				try {
					conn.close();
				}
				catch (Exception e) {}
			}
		}
		
		if (exception != null) {
			throw exception;
		} else {
			return result;
		}
	}
	
	public static String importData(Map<UniqueImport, Set<SpreadsheetImportTemplateColumn>> rowData,
	                              String encounterDate, String patientId,
	                              boolean rollbackTransaction) throws Exception {
		Connection conn = null;
		Statement s = null;
		Exception exception = null;
		String sql = null;
		
		String encounterId = null;
		try {
			
			// Connect to db
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			
			Properties p = Context.getRuntimeProperties();
			String url = p.getProperty("connection.url");
			
			conn = DriverManager.getConnection(url, p.getProperty("connection.username"),
			    p.getProperty("connection.password"));
			
			conn.setAutoCommit(false);
			
			s = conn.createStatement();
			
			List<String> importedTables = new ArrayList<String>();

			// Import
			for (UniqueImport uniqueImport : rowData.keySet()) {

				String tableName = uniqueImport.getTableName();

				//System.out.println("Table to process ============> : " + tableName);

				if (tableName.equals("patient_identifier") || tableName.equals("person_name")) {
					continue;
				}
				boolean isEncounter = "encounter".equals(tableName);
				boolean isPerson = "person".equals(tableName);
				boolean isPatientIdentifier = "patient_identifier".equals(tableName);
				boolean isPersonAttribute = "person_attribute".equals(tableName);
				boolean isObservation = "obs".equals(tableName);
				
				boolean skip = false;


				/*Set<SpreadsheetImportTemplateColumn> columns = rowData.get(uniqueImport);
				for (SpreadsheetImportTemplateColumn column : columns) {
					column.setGeneratedKey(patientId);
				}*/
				
				// SPECIAL TREATMENT
				// for encounter, if the data is available in the row, it means we're UPDATING observations for an EXISTING encounter, so we don't have to create encounter
				// otherwise, we need to create a new encounter				
				if (isEncounter) {
					Set<SpreadsheetImportTemplateColumn> columnSet = rowData.get(uniqueImport);
					for (SpreadsheetImportTemplateColumn column : columnSet) {
						Object columnValue = column.getValue();
						if (!columnValue.equals("") && columnValue != null) {
							column.setGeneratedKey(columnValue.toString());
							skip = true;
							importedTables.add("encounter"); // fake as just imported encounter
							break;
						}
					}
					if (skip)
						continue;
				}
				
				// SPECIAL TREATMENT
				// for observation, if the data to be inserted is empty, then simply skip
				if (isObservation) {
					Set<SpreadsheetImportTemplateColumn> columnSet = rowData.get(uniqueImport);
					for (SpreadsheetImportTemplateColumn column : columnSet) {
						Object columnValue = column.getValue();
						if (columnValue.equals("")) {							
							skip = true;
							importedTables.add("observation"); // fake as just imported observation, not meaningful, just for consistency purpose
							break;
						}
					}
					if (skip)
						continue;
				}
				
				 
				if (isPerson) {
					boolean isIdentifierExist = false;
					
					// SPECIAL TREATMENT 1
					// if the patient_identifier.identifier is specified and it is linked to a person, then use that person instead
					// note: patient.patient_id == person.person_id (http://forum.openmrs.org/viewtopic.php?f=2&t=436)				
					UniqueImport patientIdentifier = new UniqueImport("patient_identifier", null);
					if (rowData.containsKey(patientIdentifier)) {
						Set<SpreadsheetImportTemplateColumn> patientIdentifierColumns = rowData.get(patientIdentifier);
						for (SpreadsheetImportTemplateColumn patientIdentifierColumn : patientIdentifierColumns) {
							String columnName = patientIdentifierColumn.getColumnName();
							if ("identifier".equals(columnName)) {
								isIdentifierExist = true;

								String patientIdentifierString = "";
								if (patientIdentifierColumn.getValue() instanceof Integer) {
									Integer val = (Integer) patientIdentifierColumn.getValue();
									patientIdentifierString = String.valueOf(val.intValue());
								} else if (patientIdentifierColumn.getValue() instanceof Double) {
									Double val = (Double) patientIdentifierColumn.getValue();
									patientIdentifierString = String.valueOf(val.intValue());
								} else {
									patientIdentifierString = patientIdentifierColumn.getValue().toString();
								}
								
								sql = "select patient_id from patient_identifier where identifier = " + patientIdentifierString + " and identifier_type=16";
								
								System.out.println("Searching for existing patient of id " + patientIdentifierString);
								
								ResultSet rs = s.executeQuery(sql);
								if (rs.next()) {
									patientId = rs.getString(1);
									
									System.out.println("Found patient with patient_id = " + patientId);

									// no need to insert person, use the found patient_id as person_id
									// TODO: can this be modified to allow for inserting multiple identifiers?
									Set<SpreadsheetImportTemplateColumn> columnSet = rowData.get(uniqueImport);
									for (SpreadsheetImportTemplateColumn column : columnSet) {
										column.setGeneratedKey(patientId);
									}
											
									importedTables.add("person"); // fake as just imported person
									importedTables.add("patient"); // fake as just imported patient
									importedTables.add("patient_identifier"); // fake as just imported patient_identifier
									importedTables.add("person_name"); // fake as just imported person_name
									importedTables.add("person_address"); // fake as just imported person_address
									
									skip = true;
								}
								rs.close();
								break;								
							}
						}
					}
					if (skip)
						continue;
					
					// now, if we proceed to this point, it means patient identifier, if exists, does not match, and in that case, no point to match with person name
					
					// SPECIAL TREATMENT 2
					// if first name, last name, middle name, gender, and birthdate match existing record, then use that record instead
					UniqueImport personName = new UniqueImport("person_name", null);
					if (rowData.containsKey(personName) && !isIdentifierExist) {						
						Set<SpreadsheetImportTemplateColumn> personNameColumns = rowData.get(personName);
						
						// getting gender, birthdate from person
						Object gender = null;
						Object birthdate = null;
						for (SpreadsheetImportTemplateColumn personColumn : rowData.get(uniqueImport)) {
							String columnName = personColumn.getColumnName();
							if ("birth_date".equals(columnName))
								birthdate = personColumn.getValue();
							if ("gender".equals(columnName))
								gender = personColumn.getValue();
						}
						
						// getting first name, last name, middle name from person
						Object givenName = null;
						Object familyName = null;
						Object middleName = null;
						for (SpreadsheetImportTemplateColumn personNameColumn : personNameColumns) {
							String columnName = personNameColumn.getColumnName();
							if ("given_name".equals(columnName))
								givenName = personNameColumn.getValue();
							if ("family_name".equals(columnName))
								familyName = personNameColumn.getValue();
							if ("middle_name".equals(columnName))
								middleName = personNameColumn.getValue();
						}
						
											
						// find matching person name
						sql = "select person.person_id from person_name join person where gender " + (gender == null || gender.equals("") ? "is NULL" : "= " + gender) + " and birthdate " + (birthdate == null || birthdate.equals("") ? "is NULL" : "= " + birthdate) + " and given_name " + (givenName == null || givenName.equals("") ? "is NULL" : "= " + givenName) + " and family_name " + (familyName == null || familyName.equals("") ? "is NULL" : "= " + familyName) + " and middle_name " + (middleName == null || middleName.equals("") ? "is NULL" : "= " + middleName);
						ResultSet rs = s.executeQuery(sql);
						String personId = null;
						if (rs.next()) {
							// matched => no need to insert person, use the found patient_id as person_id
							Set<SpreadsheetImportTemplateColumn> columnSet = rowData.get(uniqueImport);
							for (SpreadsheetImportTemplateColumn column : columnSet) {
								column.setGeneratedKey(personId);// TODO: check to see if this will not always be NULL. It hasn't been set anywhere
							}

							importedTables.add("person"); // fake as just imported person
							importedTables.add("patient"); // fake as just imported patient
							importedTables.add("person_name"); // fake as just imported person_name
							importedTables.add("person_address"); // fake as just imported person_address
							
							skip = true;

						}
					}					
					if (skip)
						continue;
				}				
				
				if (isPatientIdentifier && importedTables.contains("patient_identifier"))
					continue;								
				
				// Data from columns
				Set<SpreadsheetImportTemplateColumn> columnSet = rowData.get(uniqueImport);
				String columnNames = "";
				String columnValues = "";
				Set<SpreadsheetImportTemplateColumnPrespecifiedValue> columnPrespecifiedValueSet = null;
				Set<SpreadsheetImportTemplateColumnColumn> columnColumnsImportBefore = null;
				boolean isFirst = true;
				for (SpreadsheetImportTemplateColumn column : columnSet) {
					// special treatment for encounter: simply ignore this loop since we don't want to insert encounter_id
					if (isEncounter) {
						columnPrespecifiedValueSet = column.getColumnPrespecifiedValues();
						columnColumnsImportBefore = column.getColumnColumnsImportBefore();
						
						// inject date_created
						columnNames += "date_created";
						columnValues += "now()";
						
						// find encounter_datetime based on observation date time
						java.sql.Date encounterDatetime = new java.sql.Date(System.currentTimeMillis());
						Set<UniqueImport> uniqueImports = rowData.keySet();

						if (encounterDate != null && !encounterDate.equals("")) {
							columnNames += ", encounter_datetime";
							columnValues += ",'" + encounterDate + "'";
						} else {
							for (UniqueImport u : uniqueImports) {
								if ("obs".equals(u.getTableName())) {
									Set<SpreadsheetImportTemplateColumn> obsColumns = rowData.get(u);
									for (SpreadsheetImportTemplateColumn obsColumn : obsColumns) {
										if ("obs_datetime".equals(obsColumn.getColumnName())) {
											String obsColumnValue = obsColumn.getValue().toString();
											obsColumnValue = obsColumnValue.substring(1, obsColumnValue.length()-1);
											Date obsColumnValueDate = java.sql.Date.valueOf(obsColumnValue);
											if (obsColumnValueDate.before(encounterDatetime))
												encounterDatetime = obsColumnValueDate;
										}
									}
								}
							}

							columnNames += ", encounter_datetime";
							columnValues += ",'" + encounterDatetime.toString() + "'";
						}

						
						isFirst = false;
						break;
					}					
					
					// Check for duplicates
					if (column.getDisallowDuplicateValue()) {
						String ColValue = "";
						if (column.getValue() instanceof Integer) {
							Integer val = (Integer) column.getValue();
							ColValue = String.valueOf(val.intValue());
						} else if (column.getValue() instanceof Double) {
							Double val = (Double) column.getValue();
							ColValue = String.valueOf(val.intValue());
						} else {
							ColValue = column.getValue().toString();
						}

						sql = "select " + column.getColumnName() + " from " + column.getTableName() + " where " + column.getColumnName() + " = " + ColValue;
						if (log.isDebugEnabled()) {
							log.debug(sql);
							//System.out.println(sql);
						}	
						ResultSet rs = s.executeQuery(sql);
						boolean foundDuplicate = rs.next();
						rs.close();
						if (foundDuplicate) {
							throw new SpreadsheetImportDuplicateValueException(column);
						}
					}
					
					if (isFirst) {
						// Should be same for all columns in unique import
						columnPrespecifiedValueSet = column.getColumnPrespecifiedValues();
						columnColumnsImportBefore = column.getColumnColumnsImportBefore();
						isFirst = false;
					} else {
						columnNames += ",";
						columnValues += ",";
					}
					columnNames += column.getColumnName();
					columnValues += column.getValue().toString();
					
				}
							
				// Data from pre-specified values
				for (SpreadsheetImportTemplateColumnPrespecifiedValue columnPrespecifiedValue : columnPrespecifiedValueSet) {
					if (isFirst)
						isFirst = false;
					else {
						columnNames += ",";
						columnValues += ",";					
					}

					if (columnPrespecifiedValue.getColumnName() != null && columnPrespecifiedValue.getColumnName().equals("patient_id")) {
						columnNames += "patient_id";
						columnValues += patientId;
					} else if (columnPrespecifiedValue.getColumnName() != null && columnPrespecifiedValue.getColumnName().equals("person_id")) {
						columnNames += "person_id";
						columnValues += patientId;
					} else {
						columnNames += columnPrespecifiedValue.getColumnName();
						columnValues += columnPrespecifiedValue.getPrespecifiedValue().getValue();
					}

					//System.out.println("Here in add columns: " + columnPrespecifiedValue.getColumnName() + ", " + columnPrespecifiedValue.getPrespecifiedValue().getValue());

				}
			
				// Data from columns import before
				if (!columnColumnsImportBefore.isEmpty()) {
					
					// Set up
					Map<String, String> mapPrimaryKeyColumnNameToGeneratedKey = new HashMap<String, String>();
					for (SpreadsheetImportTemplateColumnColumn columnColumn : columnColumnsImportBefore) {
						String primaryKeyColumnName = columnColumn.getColumnName();
						String columnGeneratedKey = columnColumn.getColumnImportFirst().getGeneratedKey();
						
						if (mapPrimaryKeyColumnNameToGeneratedKey.containsKey(primaryKeyColumnName)) {
							String mapGeneratedKey = mapPrimaryKeyColumnNameToGeneratedKey.get(primaryKeyColumnName);
							if (!mapGeneratedKey.equals(columnGeneratedKey)) {
								throw new SpreadsheetImportUnhandledCaseException();
							}
						} else {
							mapPrimaryKeyColumnNameToGeneratedKey.put(primaryKeyColumnName, columnGeneratedKey);
						}
						
						// TODO: I believe patient and person are only tables with this relationship, if not, then this
						// needs to be generalized
						if (primaryKeyColumnName.equals("patient_id") &&
							importedTables.contains("person") &&
							!importedTables.contains("patient")) {

							sql = "insert into patient (patient_id, creator,date_created) values (" + columnGeneratedKey + ", " + Context.getAuthenticatedUser().getId()+ ", now()" + ")";
							if (log.isDebugEnabled()) {
								log.debug(sql);
							}	
							s.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
//							ResultSet rs = s.getGeneratedKeys();
//							rs.next();
//							if (!columnGeneratedKey.equals(rs.getString(1))) {
//								throw new SpreadsheetImportUnhandledCaseException();
//							}
							importedTables.add("patient");
						}
					}
					
					// Add columns
					for (String columnName : mapPrimaryKeyColumnNameToGeneratedKey.keySet()) {
						if (isFirst)
							isFirst = false;
						else {
							columnNames += ",";
							columnValues += ",";							
						}
						columnNames += columnName;
						columnValues += mapPrimaryKeyColumnNameToGeneratedKey.get(columnName);

					}
					
				}
				
				// SPECIAL TREATMENT: if this is observation, then check for column obs_datetime. If not available, then use current time
				if (isObservation) {
					if (encounterDate != null && !encounterDate.equals("")) {
						columnNames += ",obs_datetime";
						columnValues += ",'" + encounterDate + "'";
					} else {
						boolean hasDatetime = false;
						for (SpreadsheetImportTemplateColumn column : columnSet) {
							if ("obs_datetime".equals(column.getColumnName())) {
								hasDatetime = true;
								break;
							}
						}
						if (!hasDatetime) {
							columnNames += ",obs_datetime";
							columnValues += ",now()";
						}
					}
					columnNames += ", date_created";
					columnValues += ",now()";
				}
				
				// SPECIAL TREATMENT: if this is patient identifier, then set location_id to NULL, to avoid CONSTRAINT `patient_identifier_ibfk_2` FOREIGN KEY (`location_id`) REFERENCES `location` (`location_id`))
				if (isPatientIdentifier) {
					columnNames += ", location_id";
					columnValues += ", NULL";
				}
							
				// creator
				columnNames += ",creator";
				columnValues += "," + Context.getAuthenticatedUser().getId();
				
				// uuid
				DatabaseMetaData dmd = conn.getMetaData();
				ResultSet rsColumns = dmd.getColumns(null, null, uniqueImport.getTableName(), "uuid");
				if (rsColumns.next()) {
					columnNames += ",uuid";
					columnValues += ",uuid()";
				}
				// add date created
				ResultSet dateCreatedColumn = dmd.getColumns(null, null, uniqueImport.getTableName(), "date_created");
				if (dateCreatedColumn.next() && !columnNames.contains("date_created")) {
					columnNames += ",date_created";
					columnValues += ",now()";
				}
				rsColumns.close();

				// attempt to add visit
				if (isEncounter && patientId != null) {
					String encStartDatetime = encounterDate.concat(" ").concat("00:00:00");
					String encEndDatetime = encounterDate.concat(" ").concat("23:59:59");
					String getVisitQry = "SELECT visit_id from visit where date_started BETWEEN ':startDatetime' and ':endDatetime' and patient_id=:patientID";
					getVisitQry = getVisitQry.replace(":startDatetime", encStartDatetime);
					getVisitQry = getVisitQry.replace(":endDatetime", encEndDatetime);
					getVisitQry = getVisitQry.replace(":patientID", patientId);
					String existingVisitId = null;
					//System.out.println("Get visit query: " + getVisitQry);

					ResultSet rs = s.executeQuery(getVisitQry);
					if (rs.next()) {
						existingVisitId = rs.getString(1);
						rs.close();
					} else {
						String insertVisitQry = "insert into visit(patient_id, visit_type_id, date_started, date_stopped, creator, date_created, uuid) \n" +
								"    values(:patientID,1,':dateStarted',':dateStopped',:creator,now(),uuid())";
						//replace date started string
						insertVisitQry = insertVisitQry.replace(":patientID", patientId);
						insertVisitQry = insertVisitQry.replace(":dateStarted", encStartDatetime);
						insertVisitQry = insertVisitQry.replace(":dateStopped", encEndDatetime);
						insertVisitQry = insertVisitQry.replace(":creator", Context.getAuthenticatedUser().getId().toString());
						//System.out.println("Insert visit query: " + insertVisitQry);

						s.executeUpdate(insertVisitQry, Statement.RETURN_GENERATED_KEYS);
						ResultSet visitRs = s.getGeneratedKeys();
						visitRs.next();
						existingVisitId = visitRs.getString(1);
						visitRs.close();


					}
					if (existingVisitId != null) {
						//System.out.println("Adding visit columns to query ");
						columnNames += ",visit_id ";
						columnValues += "," + existingVisitId + " ";
					}

				}

				// Insert tableName
				sql = "insert into " + uniqueImport.getTableName() + " (" + columnNames + ")" + " values ("
				        + columnValues + ")";

				// attempt to replace 'NULL' with NULL
				sql = sql.replace("'NULL'", "NULL");
				//replace empty space with ',NULL,'
				sql = sql.replace(",,",",NULL,");
				System.out.println("Query to execute: " + sql);
				if (log.isDebugEnabled()) {
					log.debug(sql);					
				}
			
				s.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
				ResultSet rs = s.getGeneratedKeys();
				rs.next();
				for (SpreadsheetImportTemplateColumn column : columnSet) {
					column.setGeneratedKey(rs.getString(1));
				}
				// SPECIAL TREATMENT: update Encounter ID back to the Excel file by returning it to the caller
				if (isEncounter)
					encounterId = rs.getString(1);
				rs.close();				
				
				importedTables.add(uniqueImport.getTableName());
			}
		} catch (SQLSyntaxErrorException e) {
			e.printStackTrace();
			throw new SpreadsheetImportSQLSyntaxException(sql, e.getMessage());
		} catch (Exception e) {
			log.debug(e.toString());
			e.printStackTrace();
			exception = e;
			throw new SpreadsheetImportSQLSyntaxException(sql, e.getMessage()); // TODO: for web debug purpose only, should comment out later
		}
		finally {
			if (s != null) {
				try {
					s.close();
				}
				catch (Exception e) {}
			}
			if (conn != null) {
				if (rollbackTransaction) {
					conn.rollback();
				} else {
					conn.commit();
				}
				try {
					conn.close();
				}
				catch (Exception e) {}
			}
		}
		
		if (exception != null) {
			throw exception;
		}
		
		return encounterId;
	}
	
	public static void validateData(Map<UniqueImport, Set<SpreadsheetImportTemplateColumn>> rowData) throws SQLException, SpreadsheetImportTemplateValidationException {
		Connection conn = null;
		Statement s = null;
		String sql = null;
		SQLException exception = null;
		ResultSet rs = null;
		
		try {
			
			// Connect to db
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			
			Properties p = Context.getRuntimeProperties();
			String url = p.getProperty("connection.url");
			
			conn = DriverManager.getConnection(url, p.getProperty("connection.username"),
			    p.getProperty("connection.password"));
						
			s = conn.createStatement();
			
			for (UniqueImport uniqueImport : rowData.keySet()) {
				if ("obs".equals(uniqueImport.getTableName())) {
					Set<SpreadsheetImportTemplateColumn> obsColumns = rowData.get(uniqueImport);
					for (SpreadsheetImportTemplateColumn obsColumn : obsColumns) {
						String columnName = obsColumn.getColumnName();
						String conceptId = getPrespecifiedConceptIdFromObsColumn(obsColumn);
						if (conceptId == null)
							 throw new SpreadsheetImportTemplateValidationException("no prespecified concept ID");

						if ("value_coded".equals(columnName)) {
							// skip if empty
							if (obsColumn.getValue().equals(""))
								continue;
							
							// verify the answers are the concepts which are possible answers							
							//sql = "select answer_concept from concept_answer join concept_name on concept_answer.answer_concept = concept_name.concept_id where concept_name.name = '" + obsColumn.getValue() + "' and concept_answer.concept_id = '" + conceptId + "'";
							//TODO: any concept should be permitted as answer to a question. Commenting the parts below for now
							/*sql = "select answer_concept from concept_answer where answer_concept = '" + obsColumn.getValue() + "' and concept_id = '" + conceptId + "'";
							rs = s.executeQuery(sql);
							if (!rs.next()) {
								sql = "select name from concept_name where locale='en' and concept_id = " + conceptId;
								rs = s.executeQuery(sql);
								rs.next();
								String conceptName = rs.getString(1);
								throw new SpreadsheetImportTemplateValidationException("invalid concept answer for the prespecified concept ID " + conceptName);
							}*/
						} else if ("value_text".equals(columnName)) {
							// skip if empty
							if (obsColumn.getValue().equals(""))
								continue;
							
							// verify the number of characters is less than the allowed length							
						} else if ("value_numeric".equals(columnName)) {
							// skip if empty
							if (obsColumn.getValue().equals(""))
								continue;
							
							// verify it's within the range specified in the concept definition
							sql = "select COALESCE (hi_normal, hi_absolute, hi_critical) hi_absolute, low_absolute from concept_numeric where concept_id = '" + conceptId + "'";
							rs = s.executeQuery(sql);
							if (!rs.next())
								throw new SpreadsheetImportTemplateValidationException("prespecified concept ID " + conceptId + " is not a numeric concept");
							double hiAbsolute = rs.getDouble(1);
							double lowAbsolute = rs.getDouble(2);
							double value = 0.0;
							try {
								value = Double.parseDouble(obsColumn.getValue().toString());
							} catch (NumberFormatException nfe) {
								throw new SpreadsheetImportTemplateValidationException("concept value is not a number");
							}
							// TODO: find the best way to validate this
							/*if (hiAbsolute >= 0 && hiAbsolute < value || lowAbsolute >= 0 && lowAbsolute > value) {
								System.out.println("Range: hiAbsolute: " + hiAbsolute + ", lowAbsolute: " + lowAbsolute);
								System.out.println("out of range concept: " + conceptId);
								throw new SpreadsheetImportTemplateValidationException("concept value " + value + " of column " + columnName + " is out of range " + lowAbsolute + " - " + hiAbsolute);
							}*/
						} else if ("value_datetime".equals(columnName) || "obs_datetime".equals(columnName)) {
							// skip if empty
							if (obsColumn.getValue().equals("") || obsColumn.getValue().equals(null) || obsColumn.getValue().equals("null") || obsColumn.getValue().equals("NULL"))
								continue;
							
							// verify datetime is defined and it can not be in the future
							String value = obsColumn.getValue().toString();
							System.out.println("Date value: " + value);
							String date = value + " 00:00:00"; // this is required for timestamp
							/*if (Timestamp.valueOf(value).after(new Timestamp(System.currentTimeMillis())))
								throw new SpreadsheetImportTemplateValidationException("date is in the future");*/
						}
					}
				} else if ("patient_identifier".equals(uniqueImport.getTableName())) {
					Set<SpreadsheetImportTemplateColumn> piColumns = rowData.get(uniqueImport);
					for (SpreadsheetImportTemplateColumn piColumn : piColumns) {
						String columnName = piColumn.getColumnName();
						if (!"identifier".equals(columnName))
							continue;
						
						String pitId = getPrespecifiedPatientIdentifierTypeIdFromPatientIdentifierColumn(piColumn);
						if (pitId == null)
							 throw new SpreadsheetImportTemplateValidationException("no prespecified patient identifier type ID");
						
						sql = "select format from patient_identifier_type where patient_identifier_type_id = " + pitId;
						//System.out.println("Identifier sql: " + sql);
						rs = s.executeQuery(sql);
						if (!rs.next())
							throw new SpreadsheetImportTemplateValidationException("invalid prespecified patient identifier type ID");
						
						String format = rs.getString(1);
						if (format != null && format.trim().length() != 0 && piColumn.getValue() != null && !piColumn.getValue().equals("") ) {
							// detect if value is numeric and try formatting the cell value to string
							String value = "";
							if (piColumn.getValue() instanceof Integer) {
								Integer val = (Integer) piColumn.getValue();
								value = String.valueOf(val);
							} else if (piColumn.getValue() instanceof Double) {
								Double val = (Double) piColumn.getValue();
								value = String.valueOf(val.intValue());
							} else {
								value = piColumn.getValue().toString();
							}
							Pattern pattern = Pattern.compile(format);
							Matcher matcher = pattern.matcher(value);
							if (!matcher.matches()) {
								throw new SpreadsheetImportTemplateValidationException("Patient ID does not conform to the specified patient identifier type format");
							}
						}
					}
				}
			}
		} catch (SQLException e) {
			log.debug(e.toString());
			exception = e;
		} catch (IllegalAccessException e) {
			log.debug(e.toString());
		} catch (InstantiationException e) {
			log.debug(e.toString());
		} catch (ClassNotFoundException e) {
			log.debug(e.toString());
		} finally {
			if (rs != null)
				try {
					rs.close();
				}
				catch (SQLException e) {}
			if (s != null) {
				try {
					s.close();
				}
				catch (SQLException e) {}
			}
			if (conn != null) {
				try {
					conn.close();
				}
				catch (SQLException e) {}
			}
		}
		
		if (exception != null) {
			throw exception;
		}
		
	}
	
	public static Locale getCurrentUserLocale() {				
		Connection conn = null;
		Statement s = null;
		String language = "en_GB";
		
		try {
			
			// Connect to db
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			
			Properties p = Context.getRuntimeProperties();
			String url = p.getProperty("connection.url");
			
			conn = DriverManager.getConnection(url, p.getProperty("connection.username"),
			    p.getProperty("connection.password"));
			
			Integer userId = Context.getAuthenticatedUser().getId();
			s = conn.createStatement();
			ResultSet rs = s.executeQuery("select property_value from user_property where user_id=" + userId.toString());
			if (rs.next())
				language = rs.getString(1);
		} catch (Exception e) {
			log.debug(e.toString());
		}
		
		Locale locale = new Locale(language);
		return locale;
	}
	
	private static String getPrespecifiedConceptIdFromObsColumn(SpreadsheetImportTemplateColumn obsColumn) {
		Set<SpreadsheetImportTemplateColumnPrespecifiedValue> prespecifiedColumns = obsColumn.getColumnPrespecifiedValues();
		for (SpreadsheetImportTemplateColumnPrespecifiedValue prespecifiedColumn : prespecifiedColumns) {
			String prespecifiedColumnName = prespecifiedColumn.getColumnName();
			if ("concept_id".equals(prespecifiedColumnName))
				return prespecifiedColumn.getPrespecifiedValue().getValue();
		}
		return null;
	}
	
	private static String getPrespecifiedPatientIdentifierTypeIdFromPatientIdentifierColumn(SpreadsheetImportTemplateColumn piColumn) {
		Set<SpreadsheetImportTemplateColumnPrespecifiedValue> prespecifiedColumns = piColumn.getColumnPrespecifiedValues();
		for (SpreadsheetImportTemplateColumnPrespecifiedValue prespecifiedColumn : prespecifiedColumns) {
			String prespecifiedColumnName = prespecifiedColumn.getColumnName();
			if ("identifier_type".equals(prespecifiedColumnName))
				return prespecifiedColumn.getPrespecifiedValue().getValue();
		}
		return null;
	}
}
