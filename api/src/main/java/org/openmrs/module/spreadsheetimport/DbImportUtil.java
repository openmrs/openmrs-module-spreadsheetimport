/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 * <p>
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 * <p>
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.spreadsheetimport;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.GenericValidator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.Provider;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.ProviderService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.spreadsheetimport.service.SpreadsheetImportService;
import org.openmrs.util.OpenmrsUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

/**
 *
 */
public class DbImportUtil {

    public static Map<String, Properties> migrationProgressMap = new LinkedHashMap<String, Properties>();

    static String GP_MIGRATION_CONFIG_DIR = "spreadsheetimport.migrationConfigDirectory";

    /** Logger for this class and subclasses */
    protected static final Log log = LogFactory.getLog(SpreadsheetImportUtil.class);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static void updateMigrationProgressMapProperty(String dataset, String key, String value) {
        if (migrationProgressMap.get(dataset) != null) {
            migrationProgressMap.get(dataset).setProperty(key, value);
        } else {
            Properties p = new Properties();
            p.setProperty(key, value);
            migrationProgressMap.put(dataset, p);
        }

    }


    /**
     * Resolve template dependencies: 1. Generate pre-specified values which are necessary for
     * template to be imported. 2. Create import indices which describe the order in which columns
     * must be imported. 3. Generated dependencies between columns being imported and other columns
     * which be must imported first.
     *
     * @param template
     * @throws Exception
     */
    public static void resolveTemplateDependencies(SpreadsheetImportTemplate template) throws Exception {

        Set<SpreadsheetImportTemplatePrespecifiedValue> prespecifiedValues = new TreeSet<SpreadsheetImportTemplatePrespecifiedValue>();

        Map<String, Set<UniqueImport>> mapTnToUi = template.getMapOfColumnTablesToUniqueImportSet();
        Map<UniqueImport, Set<SpreadsheetImportTemplateColumn>> mapUiToCs = template.getMapOfUniqueImportToColumnSet();

        List<String> tableNamesSortedByImportIdx = new ArrayList<String>();

//		// special treatment: when there's a reference to person_id, but
//		//  1) the current table is not encounter and
//		//  2) there's no column of table person to be added
//		// then we should still add a person implicitly. This person record will use all default values
//		boolean hasToAddPerson = false;
//		for (UniqueImport key : mapUiToCs.keySet()) {
//			String tableName = key.getTableName();
//			if (!("encounter".equals(tableName) || mapTnToUi.keySet().contains("person"))) {
//				hasToAddPerson = true;
//				break;
//			}
//		}
//		if (hasToAddPerson) {
//			UniqueImport ui = new UniqueImport("person", new Integer(-1));
//			mapTnToUi.put("person", new TreeSet<UniqueImport>());
//			mapUiToCs.put(ui, new TreeSet<SpreadsheetImportTemplateColumn>());
//		}

        // Find requirements
        for (UniqueImport key : mapUiToCs.keySet()) {
            String tableName = key.getTableName();

            Map<String, String> mapIkTnToCn = DatabaseBackend.getMapOfImportedKeyTableNameToColumnNamesForTable(tableName);

            if ("patient_identifier".equals(tableName))
                mapIkTnToCn.put("patient", "patient_id");

            // encounter_id is optional, so it won't be part of mapIkTnToCn
            // if we need to create new encounter for this row, then force it to be here
            if (template.isEncounter() && "obs".equals(tableName))
                mapIkTnToCn.put("encounter", "encounter_id");

            // we need special treatment for provider_id of Encounter
            // provider_id is of type person, but the meaning is different. During import, reference to person is considered patient,
            // but for provider_id of Encounter, it refers to a health practitioner
            if ("encounter".equals(tableName)) {
//				mapIkTnToCn.put("person", "provider_id"); 			// UPDATE: provider_id is no longer a foreign key for encounter
                mapIkTnToCn.put("location", "location_id");
                mapIkTnToCn.put("form", "form_id");

//				// if this is an encounter-based import, then pre-specify the form_id for the encounter
//				// 1. search for encounter column
//				SpreadsheetImportTemplateColumn encounterColumn = mapUiToCs.get(key).iterator().next();
//				// 2. prespecify form
//				SpreadsheetImportTemplatePrespecifiedValue v = new SpreadsheetImportTemplatePrespecifiedValue();
//				v.setTemplate(template);
//				v.setTableDotColumn("form.form_id");
//				v.setValue(template.getTargetForm());
//				SpreadsheetImportTemplateColumnPrespecifiedValue cpv = new SpreadsheetImportTemplateColumnPrespecifiedValue();
//				cpv.setColumn(encounterColumn);
//				cpv.setPrespecifiedValue(v);
//				prespecifiedValues.add(v);
            }

            // Ignore users tableName
            mapIkTnToCn.remove("users");

            for (String necessaryTableName : mapIkTnToCn.keySet()) {

                String necessaryColumnName = mapIkTnToCn.get(necessaryTableName);

                // TODO: I believe patient and person are only tables with this relationship, if not, then this
                // needs to be generalized
                if (necessaryTableName.equals("patient") &&
                        !mapTnToUi.containsKey("patient") &&
                        mapTnToUi.containsKey("person")) {
                    necessaryTableName = "person";
                }

                if (mapTnToUi.containsKey(necessaryTableName) && !("encounter".equals(tableName) && ("provider_id".equals(necessaryColumnName)))) {

                    // Not already imported? Add
                    if (!tableNamesSortedByImportIdx.contains(necessaryTableName)) {
                        tableNamesSortedByImportIdx.add(necessaryTableName);
                    }

                    // Add column dependencies
                    // TODO: really _table_ dependencies - for simplicity only use _first_ column
                    // of each unique import
                    Set<SpreadsheetImportTemplateColumn> columnsImportFirst = new TreeSet<SpreadsheetImportTemplateColumn>();
                    for (UniqueImport uniqueImport : mapTnToUi.get(necessaryTableName)) {
                        // TODO: hacky cast
                        columnsImportFirst.add(((TreeSet<SpreadsheetImportTemplateColumn>) mapUiToCs.get(uniqueImport)).first());
                    }
                    for (SpreadsheetImportTemplateColumn columnImportNext : mapUiToCs.get(key)) {
                        for (SpreadsheetImportTemplateColumn columnImportFirst : columnsImportFirst) {
                            SpreadsheetImportTemplateColumnColumn cc = new SpreadsheetImportTemplateColumnColumn();
                            cc.setColumnImportFirst(columnImportFirst);
                            cc.setColumnImportNext(columnImportNext);
                            cc.setColumnName(necessaryColumnName);
                            columnImportNext.getColumnColumnsImportBefore().add(cc);
                        }
                    }

                } else {

                    // Add pre-specified value
                    SpreadsheetImportTemplatePrespecifiedValue v = new SpreadsheetImportTemplatePrespecifiedValue();
                    v.setTemplate(template);
                    v.setTableDotColumn(necessaryTableName + "." + necessaryTableName + "_id");
                    for (SpreadsheetImportTemplateColumn column : mapUiToCs.get(key)) {
                        SpreadsheetImportTemplateColumnPrespecifiedValue cpv = new SpreadsheetImportTemplateColumnPrespecifiedValue();
                        cpv.setColumn(column);
                        cpv.setPrespecifiedValue(v);


//						System.out.println("SpreadsheetImportUtils: " + v.getTableDotColumn() + " ==> " + v.getValue());

                        cpv.setColumnName(necessaryColumnName);
                        v.getColumnPrespecifiedValues().add(cpv);
                    }
                    prespecifiedValues.add(v);
                }
            }

            // Add this tableName if not already added
            if (!tableNamesSortedByImportIdx.contains(tableName)) {
                tableNamesSortedByImportIdx.add(tableName);
            }
        }

        // Add all pre-specified values
        template.getPrespecifiedValues().addAll(prespecifiedValues);

        // Set column import indices based on tableNameSortedByImportIdx
        int importIdx = 0;
        for (String tableName : tableNamesSortedByImportIdx) {
            for (UniqueImport uniqueImport : mapTnToUi.get(tableName)) {
                for (SpreadsheetImportTemplateColumn column : mapUiToCs.get(uniqueImport)) {
                    column.setImportIdx(importIdx);
                    importIdx++;
                }
            }
        }
    }

    private static String toString(List<String> list) {
        String result = "";
        for (int i = 0; i < list.size(); i++) {
            if (list.size() == 2 && i == 1) {
                result += " and ";
            } else if (list.size() > 2 && i == list.size() - 1) {
                result += ", and ";
            } else if (i != 0) {
                result += ", ";
            }
            result += list.get(i);
        }
        return result;
    }

    public static String importTemplate(SpreadsheetImportTemplate template,
                                        List<String> messages, boolean rollbackTransaction, String mainPtIdType, String groupedObsConfigFile, String migrationDatabase) throws Exception {
        MysqlDataSource dataSource = null;
        Connection conn = null;
        Statement s = null;
        String sql = null;
        List<String> columnNames = new Vector<String>();

        Map<Integer, String> tableToTemplateMap = DbImportUtil.reverseMapKeyValues(getTemplateDatasetMap());
        String tableToProcess = tableToTemplateMap.get(template.getId());


        try {

            //System.out.println("Attempting to read from the migration database!");


            Properties p = Context.getRuntimeProperties();
            String url = p.getProperty("connection.url");

            dataSource = new MysqlDataSource();
            dataSource.setURL(url);
            dataSource.setUser(p.getProperty("connection.username"));
            dataSource.setPassword(p.getProperty("connection.password"));

            conn = dataSource.getConnection();

            //conn.setAutoCommit(false);

            s = conn.createStatement();

            DatabaseMetaData dmd = conn.getMetaData();
            ResultSet rsColumns = dmd.getColumns(migrationDatabase, null, tableToProcess, null);

            while (rsColumns.next()) {
                String colName = rsColumns.getString("COLUMN_NAME");
                columnNames.add(colName);
            }

            rsColumns.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (log.isDebugEnabled()) {
            log.debug("Column names: " + columnNames.toString());
        }

        // Required column names
        List<String> columnNamesOnlyInTemplate = new Vector<String>();
        columnNamesOnlyInTemplate.addAll(template.getColumnNamesAsList());
        columnNamesOnlyInTemplate.removeAll(columnNames);
        if (columnNamesOnlyInTemplate.isEmpty() == false) {
            messages.add("required column names not present: " + toString(columnNamesOnlyInTemplate));
            return null;
        }

        // Extra column names?
        List<String> columnNamesOnlyInSheet = new Vector<String>();
        columnNamesOnlyInSheet.addAll(columnNames);

        columnNamesOnlyInSheet.removeAll(template.getColumnNamesAsList());

        // AO
        // remove the extra encounter date column from validation warning
        if (columnNamesOnlyInSheet.contains("Encounter_Date")) {
            columnNamesOnlyInSheet.remove("Encounter_Date");
        }

        if (columnNamesOnlyInSheet.isEmpty() == false) {
            //messages.add("Extra column names present, these will not be processed: " + toString(columnNamesOnlyInSheet));
        }

        // Process rows
        String tableName = tableToTemplateMap.get(template.getId());

        String query = "select * from :migrationDatabase.:tableName";
        query = query.replace(":migrationDatabase", migrationDatabase);
        query = query.replace(":tableName", tableName);
        ResultSet rs = s.executeQuery(query);

        // load json config for dataset
        List<GroupedObservations> gObs = null;
        if (groupedObsConfigFile != null && StringUtils.isNotBlank(groupedObsConfigFile)) {
            gObs = DbImportUtil.getGroupedDatasetConfigForTemplate(groupedObsConfigFile);
        }
        int recordCount = 0;

        if (rs.next() == false) {
            System.out.println("Empty dataset. Will skip processing");
            return "Empty dataset. Will skip processing";
        } else {
            do {

                boolean rowHasData = false;
                // attempt to process the extra encounter_datetime
                String encounterDateColumn = "Encounter_Date";
                String mainIdentifierColumn = "Person_Id";
                String internalIdColumn = "patient_id";
                String patientIdColVal = rs.getString(mainIdentifierColumn);
                String internalIdVal = rs.getString(internalIdColumn);
                String patientId = rs.getString(internalIdColumn);
                //String patientId = null;


                // ==========================
            /*Statement getPatientSt = conn.createStatement();
            String patientIdsql = "select patient_id from patient_identifier where identifier = " + patientIdColVal + " and identifier_type=" + mainPtIdType;


            ResultSet getPatientRs = getPatientSt.executeQuery(patientIdsql);
            if (getPatientRs.next()) {
                patientId = getPatientRs.getString(1);

            }
            if (getPatientRs != null) {
                getPatientRs.close();
            }*/

                // ==========================
                String rowEncDate = null;
                int encDateColumnIdx = columnNames.indexOf(encounterDateColumn);

                if (encDateColumnIdx >= 0) {
                    if (rs.getDate(encounterDateColumn) != null) {
                        java.util.Date encDate = rs.getDate(encounterDateColumn);
                        rowEncDate = DATE_FORMAT.format(encDate);// "'" + new java.sql.Timestamp(encDate.getTime()).toString() + "'";

                    }

                }
                Map<UniqueImport, Set<SpreadsheetImportTemplateColumn>> rowData = template
                        .getMapOfUniqueImportToColumnSetSortedByImportIdx();

                for (UniqueImport uniqueImport : rowData.keySet()) {
                    Set<SpreadsheetImportTemplateColumn> columnSet = rowData.get(uniqueImport);
                    for (SpreadsheetImportTemplateColumn column : columnSet) {

                        Object value = null;

                        if (GenericValidator.isInt(rs.getString(column.getName()))) {
                            value = rs.getInt(column.getName());
                        } else if (GenericValidator.isFloat(rs.getString(column.getName()))) {
                            value = rs.getDouble(column.getName());
                        } else if (GenericValidator.isDouble(rs.getString(column.getName()))) {
                            value = rs.getDouble(column.getName());
                        } else if (GenericValidator.isDate(rs.getString(column.getName()), Context.getLocale())) {
                            java.util.Date date = rs.getDate(rs.getString(column.getName()));
                            value = "'" + new java.sql.Timestamp(date.getTime()).toString() + "'";
                        } else {
                            value = rs.getString(column.getName());
                            if (value != null && !value.equals("")) {
                                value = "'" + rs.getString(column.getName()) + "'";
                            }
                        }
                        // check for empty cell (new Encounter)
                        if (value == null) {
                            rowHasData = true;
                            column.setValue("");
                            continue;
                        }

                        if (value != null) {
                            rowHasData = true;
                            column.setValue(value);
                        } else {
                            column.setValue("");
                        }
                    }
                }

                for (UniqueImport uniqueImport : rowData.keySet()) {
                    Set<SpreadsheetImportTemplateColumn> columnSet = rowData.get(uniqueImport);
                    boolean isFirst = true;
                    for (SpreadsheetImportTemplateColumn column : columnSet) {

                        if (isFirst) {
                            // Should be same for all columns in unique import
//							System.out.println("SpreadsheetImportUtil.importTemplate: column.getColumnPrespecifiedValues(): " + column.getColumnPrespecifiedValues().size());
                            if (column.getColumnPrespecifiedValues().size() > 0) {
                                Set<SpreadsheetImportTemplateColumnPrespecifiedValue> columnPrespecifiedValueSet = column.getColumnPrespecifiedValues();
                                for (SpreadsheetImportTemplateColumnPrespecifiedValue columnPrespecifiedValue : columnPrespecifiedValueSet) {
//									System.out.println(columnPrespecifiedValue.getPrespecifiedValue().getValue());
                                }
                            }
                        }
                    }
                }


                /**
                 * Extract values of grouped observations here
                 */
                if (gObs != null) {
                    for (GroupedObservations gO : gObs) {
                        boolean groupHasData = false;
                        for (Map.Entry<String, DatasetColumn> e : gO.getDatasetColumns().entrySet()) {
                            String k = e.getKey();
                            DatasetColumn v = e.getValue();

                            Object value = null;

                            if (GenericValidator.isInt(rs.getString(k))) {
                                value = rs.getInt(k);
                            } else if (GenericValidator.isFloat(rs.getString(k))) {
                                value = rs.getDouble(k);
                            } else if (GenericValidator.isDouble(rs.getString(k))) {
                                value = rs.getDouble(k);
                            } else if (GenericValidator.isDate(rs.getString(k), Context.getLocale())) {
                                java.util.Date date = rs.getDate(rs.getString(k));
                                value = "'" + new java.sql.Timestamp(date.getTime()).toString() + "'";
                            } else {
                                value = rs.getString(k);
                                if (value != null && StringUtils.isNotBlank(value.toString())) {
                                    value = "'" + rs.getString(k) + "'";
                                }
                            }

                            if (value != null && StringUtils.isNotBlank(value.toString())) {
                                v.setValue(value.toString());
                                if (!groupHasData) {
                                    groupHasData = true;
                                    gO.setHasData(true);
                                }
                            }
                        }
                        gO.setHasData(groupHasData);

                    }
                }

                if (rowHasData && StringUtils.isNotBlank(patientId)) {
                    Exception exception = null;
                    try {
                        //DatabaseBackend.validateData(rowData);
                        String encounterId = DatabaseBackend.importData(rowData, rowEncDate, patientId, gObs, rollbackTransaction, conn);
                        recordCount++;
                        DbImportUtil.updateMigrationProgressMapProperty(template.getName(), "processedCount", String.valueOf(recordCount));


                    /*if (recordCount == 1) {
                        System.out.println(new Date().toString() + ":: Completed processing record 1 ::  for template " + template.getName());
                    } else if (recordCount%1000 == 0) {
                        System.out.println(new Date().toString() + ":: Completed processing record :: " + recordCount + " for template " + template.getName());
                    }*/
                    /*if (encounterId != null) {
                        for (UniqueImport uniqueImport : rowData.keySet()) {
                            Set<SpreadsheetImportTemplateColumn> columnSet = rowData.get(uniqueImport);
                            for (SpreadsheetImportTemplateColumn column : columnSet) {
                                //Write generated encounter_id in the Encounter ID column
                                if ("encounter".equals(column.getTableName())) {
                                    System.out.println("New encounter: " + encounterId);
                                }
                            }
                        }
                    }*/
                    } catch (SpreadsheetImportTemplateValidationException e) {
                        messages.add("Validation failed: " + e.getMessage());
                        return null;
                    } catch (SpreadsheetImportDuplicateValueException e) {
                        messages.add("found duplicate value for column " + e.getColumn().getName() + " with value " + e.getColumn().getValue());
                        return null;
                    } catch (SpreadsheetImportSQLSyntaxException e) {
                        e.printStackTrace();
                        messages.add("SQL syntax error: \"" + e.getSqlErrorMessage() + "\".<br/>Attempted SQL Statement: \"" + e.getSqlStatement() + "\"");
                        return null;
                    } catch (Exception e) {
                        exception = e;
                    }
                    if (exception != null) {
                        throw exception;
                    }
                }

            } while (rs.next());
        }

        try {
            if (conn != null) {
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        return "Successful import";
    }

    /**
     * Processor for KenyaEMR demographics.
     * @param messages
     * @param migrationDatabase
     * @return
     */
    public static String processDemographicsDataset(List<String> messages, String migrationDatabase) {

        //TODO: provide this mapping in a json document that can be modified outside of code
        /**
         * compose mapping template for the demographics metadata
         */
        long startTime = System.nanoTime();

        String NEXT_OF_KIN_ADDRESS = "7cf22bec-d90a-46ad-9f48-035952261294";
        String NEXT_OF_KIN_CONTACT = "342a1d39-c541-4b29-8818-930916f4c2dc";
        String NEXT_OF_KIN_NAME = "830bef6d-b01f-449d-9f8d-ac0fede8dbd3";
        String NEXT_OF_KIN_RELATIONSHIP = "d0aa9fd1-2ac5-45d8-9c5e-4317c622c8f5";
        String SUBCHIEF_NAME = "40fa0c9c-7415-43ff-a4eb-c7c73d7b1a7a";
        String TELEPHONE_CONTACT = "b2c38640-2603-4629-aebd-3b54f33f1e3a";
        String EMAIL_ADDRESS = "b8d0b331-1d2d-4a9a-b741-1816f498bdb6";
        String ALTERNATE_PHONE_CONTACT = "94614350-84c8-41e0-ac29-86bc107069be";
        String NEAREST_HEALTH_CENTER = "27573398-4651-4ce5-89d8-abec5998165c";
        String GUARDIAN_FIRST_NAME = "8caf6d06-9070-49a5-b715-98b45e5d427b";
        String GUARDIAN_LAST_NAME = "0803abbd-2be4-4091-80b3-80c6940303df";

        String CWC_NUMBER = "1dc8b419-35f2-4316-8d68-135f0689859b";
        String DISTRICT_REGISTRATION_NUMBER = "d8ee3b8c-a8fc-4d6b-af6a-9423be5f8906";
        String HEI_UNIQUE_NUMBER = "0691f522-dd67-4eeb-92c8-af5083baf338";
        String NATIONAL_ID = "49af6cdc-7968-4abb-bf46-de10d7f4859f";
        String UNIQUE_PATIENT_NUMBER = "05ee9cf4-7242-4a17-b4d4-00f707265c8a";
        String IQCARE_PERSON_PK = "b3d6de9f-f215-4259-9805-8638c887e46b"; // this should be retired once migration is complete

        List<String> identifierTypeList = Arrays.asList(UNIQUE_PATIENT_NUMBER, NATIONAL_ID, IQCARE_PERSON_PK);


        String COL_IQCARE_PERSON_PK = "Person_Id";
        String COL_FIRST_NAME = "First_Name";
        String COL_MIDDLE_NAME = "Middle_Name";
        String COL_LAST_NAME = "Last_Name";
        String COL_SEX = "Sex";
        String COL_DOB = "DOB";
        String COL_COUNTY = "County";
        String COL_SUB_COUNTY = "Sub_county";
        String COL_WARD = "Ward";
        String COL_VILLAGE = "Village";
        String COL_LAND_MARK = "Landmark";
        String COL_NEAREST_HEALTH_CENTER = "Nearest_Health_Centre";
        String COL_POSTAL_ADDRESS = "Postal_Address";
        String COL_UPN = "UPN";
        String COL_CLINIC_NUMBER = "Patient_clinic_number";
        String COL_SOURCE_ID = "UPN";
        String COL_NATIONAL_ID = "National_id_no";
        String COL_DEAD = "Dead";
        String COL_DEATH_DATE = "Death_date";
        String COL_PHONE_NO = "Phone_number";
        String COL_ALTERNATE_PHONE_NO = "Alternate_Phone_number";
        String COL_EMAIL = "Email_address";
        String COL_MARITAL_STATUS = "Marital_status";
        String COL_OCCUPATION = "Occupation";
        String COL_EDUCATION_LEVEL = "Education_level";


        Map<String, String> columnMap = new HashMap<String, String>(); // stores table.column mapping for template columns
        Map<String, String> columnConfigMap = new HashMap<String, String>();// stores type uuid

        columnMap.put("First_Name", "person_name.given_name");
        columnMap.put("Middle_Name", "person_name.middle_name");
        columnMap.put("Last_Name", "person_name.family_name");
        columnMap.put("DOB", "birthdate");
        columnMap.put("Sex", "gender");

        // identifiers
        columnMap.put("UPN", "patient_identifier.identifier");
        columnMap.put("National_id_no", "patient_identifier.identifier");

        columnConfigMap.put("UPN", UNIQUE_PATIENT_NUMBER);
        columnConfigMap.put("National_id_no", NATIONAL_ID);


        // addresses
        columnMap.put("Postal_Address", "person_address.address1");
        columnMap.put("County", "person_address.county_district");
        columnMap.put("Sub_county", "person_address.state_province");
        columnMap.put("Ward", "person_address.address4");
        columnMap.put("Village", "person_address.city_village");
        columnMap.put("Landmark", "person_address.address2");

        //death status
        columnMap.put("Dead", "dead");
        columnMap.put("Death_date", "person.dead");

        // person attributes
        columnMap.put("Phone_number", "person_attribute.value");
        columnMap.put("Alternate_Phone_number", "person_attribute.value");
        columnMap.put("Email_address", "person_attribute.value");
        columnMap.put("Nearest_Health_Center", "person_attribute.value");

        columnConfigMap.put("Phone_number", TELEPHONE_CONTACT);
        columnConfigMap.put("Alternate_Phone_number", ALTERNATE_PHONE_CONTACT);
        columnConfigMap.put("Email_address", EMAIL_ADDRESS);
        columnConfigMap.put("Nearest_Health_Center", NEAREST_HEALTH_CENTER);


        // obs
        columnMap.put("Marital_status", "obs.value_coded");
        columnMap.put("Occupation", "obs.value_coded");
        columnMap.put("Education_level", "obs.value_coded");

        columnConfigMap.put("Marital_status", "1054");
        columnConfigMap.put("Occupation", "1542");
        columnConfigMap.put("Education_level", "1712");


        Connection conn = null;
        Statement s = null;
        Integer upnIdType = null;
        Integer natIdIdType = null;
        Integer iqCarePkType = null;


        try {

            // Connect to db
            Class.forName("com.mysql.jdbc.Driver").newInstance();

            Properties p = Context.getRuntimeProperties();
            String url = p.getProperty("connection.url");

            conn = DriverManager.getConnection(url, p.getProperty("connection.username"),
                    p.getProperty("connection.password"));
            conn.setAutoCommit(false);

            String sqlIQCareNumberStr = "select patient_identifier_type_id from patient_identifier_type where uuid='" + IQCARE_PERSON_PK + "'";
            ResultSet rsIqCare = null;
            try {
                Statement sGetIQCareType = conn.createStatement();
                rsIqCare = sGetIQCareType.executeQuery(sqlIQCareNumberStr);
                if (rsIqCare.next()) {
                    iqCarePkType = rsIqCare.getInt(1);
                    rsIqCare.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // get UPN encounter type
            String sqlUpnStr = "select patient_identifier_type_id from patient_identifier_type where uuid='" + UNIQUE_PATIENT_NUMBER + "'";
            ResultSet rs1 = null;
            try {
                Statement sGetUPNType = conn.createStatement();
                rs1 = sGetUPNType.executeQuery(sqlUpnStr);
                if (rs1.next()) {
                    upnIdType = rs1.getInt(1);
                    rs1.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // get National ID encounter type
            String sqlNatIdStr = "select patient_identifier_type_id from patient_identifier_type where uuid='" + NATIONAL_ID + "'";
            ResultSet rs2 = null;
            try {
                Statement sGetNatIdType = conn.createStatement();
                rs2 = sGetNatIdType.executeQuery(sqlNatIdStr);
                if (rs2.next()) {
                    natIdIdType = rs2.getInt(1);
                    rs2.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }


            s = conn.createStatement();

            String query = "select * from :migrationDatabase.tr_demographics";
            query = query.replace(":migrationDatabase", migrationDatabase);

            ResultSet rs = s.executeQuery(query);
            int recordCount = 0;

            while (rs.next()) {
                String sql = null;

                String encounterId = null;
                Integer patientId = null;

                String fName = null;
                String mName = null;
                String lName = null;
                Date dob = null;

                String sex = null;
                String county = null;
                String subCounty = null;
                String ward = null;
                String village = null;
                String landMark = null;
                String postalAddress = null;
                String nearestHealthCenter = null;
                String upn = null;
                String nationalId = null;
                String iqCarePersonId = null;


                // extract person name
                fName = rs.getString(COL_FIRST_NAME);
                mName = rs.getString(COL_MIDDLE_NAME);
                lName = rs.getString(COL_LAST_NAME);
                dob = rs.getDate(COL_DOB);
                sex = rs.getString(COL_SEX);

                // extract identifiers

                iqCarePersonId = rs.getString(COL_IQCARE_PERSON_PK);
                upn = rs.getString(COL_UPN);
                nationalId = rs.getString(COL_NATIONAL_ID);


                // extract address
                county = rs.getString(COL_COUNTY);
                subCounty = rs.getString(COL_SUB_COUNTY);
                ward = rs.getString(COL_WARD);
                village = rs.getString(COL_VILLAGE);
                landMark = rs.getString(COL_LAND_MARK);
                postalAddress = rs.getString(COL_POSTAL_ADDRESS);
                nearestHealthCenter = rs.getString("Nearest_Health_Centre");

                // insert person query
                sql = "insert into person (date_created, uuid, creator, gender, birthdate) " +
                        "values (now(), uuid(), ?, ?, ?);";

                PreparedStatement insertPerson = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                insertPerson.setInt(1, Context.getAuthenticatedUser().getId()); // set creator
                insertPerson.setString(2, sex);
                insertPerson.setTimestamp(3, (dob != null && !dob.equals("")) ? new java.sql.Timestamp(dob.getTime()) : null);
                insertPerson.executeUpdate();
                ResultSet returnedPerson = insertPerson.getGeneratedKeys();
                returnedPerson.next();
                patientId = returnedPerson.getInt(1);
                returnedPerson.close();

                // process into person name

                sql = "insert into person_name " +
                        "(date_created, uuid, creator,person_id, given_name, middle_name, family_name) " +
                        "values (now(),uuid(),?,?,?,?,?);";

                // execute query
                PreparedStatement insertPersonName = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                insertPersonName.setInt(1, Context.getAuthenticatedUser().getId()); // set creator
                insertPersonName.setInt(2, patientId.intValue()); // set person id
                insertPersonName.setString(3, fName);
                insertPersonName.setString(4, mName);
                insertPersonName.setString(5, lName);
                insertPersonName.executeUpdate();
                ResultSet returnedPersonName = insertPersonName.getGeneratedKeys();
                returnedPersonName.next();
                returnedPersonName.close();

                // process person address

                sql = "insert into person_address " +
                        "(date_created, uuid, creator, person_id, county_district, state_province, address4, city_village, address2, address1)  " +
                        " values(now(),uuid(), ?, ?, ?, ?, ?, ?, ?, ?);";

                PreparedStatement insertPersonAddress = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                insertPersonAddress.setInt(1, Context.getAuthenticatedUser().getId()); // set creator
                insertPersonAddress.setInt(2, patientId.intValue()); // set person id
                insertPersonAddress.setString(3, county);
                insertPersonAddress.setString(4, subCounty);
                insertPersonAddress.setString(5, ward);
                insertPersonAddress.setString(6, village);
                insertPersonAddress.setString(7, landMark);
                insertPersonAddress.setString(8, postalAddress);

                insertPersonAddress.executeUpdate();
                ResultSet returnedPersonAddress = insertPersonAddress.getGeneratedKeys();
                returnedPersonAddress.next();
                returnedPersonAddress.close();

                // insert into patient table

                sql = "insert into patient (patient_id, creator,date_created) values (" + patientId.intValue() + ", " + Context.getAuthenticatedUser().getId() + ", now()" + ")";
                Statement insertPatient = conn.createStatement();
                insertPatient.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
                ResultSet returnedPatient = insertPatient.getGeneratedKeys();
                returnedPatient.next();
                returnedPatient.close();

                for (String idType : identifierTypeList) {
                    String colName = null;
                    String colValue = null;
                    Integer idTypeId = null;
                    if (idType.equals(UNIQUE_PATIENT_NUMBER)) {
                        colName = COL_UPN;
                        idTypeId = upnIdType;
                    } else if (idType.equals(IQCARE_PERSON_PK)) {
                        colName = COL_IQCARE_PERSON_PK;
                        idTypeId = iqCarePkType;
                    } else if (idType.equals(NATIONAL_ID)) {
                        colName = COL_NATIONAL_ID;
                        idTypeId = natIdIdType;
                    }

                    colValue = rs.getString(colName);
                    if (colValue != null && !colValue.equals("")) {

                        sql = "insert into patient_identifier " +
                                "(date_created, uuid, location_id, creator,patient_id, identifier_type, identifier) " +
                                "values (now(),uuid(), NULL,?,?,?,?);";

                        PreparedStatement insertIdentifierStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

                        insertIdentifierStatement.setInt(1, Context.getAuthenticatedUser().getId()); // set creator
                        insertIdentifierStatement.setInt(2, patientId.intValue()); // set person id
                        insertIdentifierStatement.setInt(3, idTypeId.intValue());
                        insertIdentifierStatement.setString(4, colValue);

                        insertIdentifierStatement.executeUpdate();
                        ResultSet returnedId = insertIdentifierStatement.getGeneratedKeys();
                        returnedId.next();
                        returnedId.close();

                    }
                }

                recordCount++;
                DbImportUtil.updateMigrationProgressMapProperty("Demographics", "processedCount", String.valueOf(recordCount));

                /*if (recordCount == 1) {
                    System.out.println(new Date().toString() + ":: Completed processing record 1 ::  in demographics dataset");
                } else if (recordCount%5000==0) {
                    System.out.println(new Date().toString() + ":: Completed Processing record :: " + recordCount + " in demographics dataset");
                }*/
            }

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (Exception e) {
                }
            }
            if (conn != null) {
                try {
                    conn.commit();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                try {
                    conn.close();
                } catch (Exception e) {
                }
            }
        }


        return "Successful";
    }

    public static Map<String, Integer> getTemplateDatasetMap() {
        String GP_MIGRATION_CONFIG_DIR = "spreadsheetimport.migrationConfigDirectory";
        File configFile = OpenmrsUtil.getDirectoryInApplicationDataDirectory(Context.getAdministrationService().getGlobalProperty(GP_MIGRATION_CONFIG_DIR));
        String fullFilePath = configFile.getPath() + File.separator + "TemplateDatasetMap.json";
        JSONParser jsonParser = new JSONParser();
        try {
            //Read JSON file
            FileReader reader = new FileReader(fullFilePath);
            Object obj = jsonParser.parse(reader);

            JSONArray templateDatasetMap = (JSONArray) obj;
            Map<String, Integer> configMap = new LinkedHashMap<String, Integer>();

            for (int i = 0; i < templateDatasetMap.size(); i++) {
                JSONObject o = (JSONObject) templateDatasetMap.get(i);
                //every object has description, template_id, and dataset properties.
                Long tempId = (Long) (o.get("template_id"));// this value is read as Long
                int tempIdIntVal = tempId.intValue();
                configMap.put((String) o.get("dataset"), tempIdIntVal);
            }
            return configMap;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Interchanges the content of a map
     * to return values as keys
     * @param map
     * @return Map of values as keys and keys as values
     */
    private static Map<Integer, String> reverseMapKeyValues(Map<String, Integer> map) {
        Map<Integer, String> resMap = new HashMap<Integer, String>();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            resMap.put(entry.getValue(), entry.getKey());
        }
        return resMap;
    }

    /**
     * Processes configuration files for a dataset's grouped observations
     * @return a List of GroupedObservations
     */
    protected static List<GroupedObservations> getGroupedDatasetConfigForTemplate(String fileName) {
        File configFile = OpenmrsUtil.getDirectoryInApplicationDataDirectory(Context.getAdministrationService().getGlobalProperty(GP_MIGRATION_CONFIG_DIR));
        String fullFilePath = configFile.getPath() + File.separator + fileName;
        JSONParser jsonParser = new JSONParser();
        try {
            //Read JSON file
            FileReader reader = new FileReader(fullFilePath);
            Object obj = jsonParser.parse(reader);

            JSONArray obsGrp = (JSONArray) obj;
            List<GroupedObservations> grpObsForDataset = new ArrayList<GroupedObservations>();
            for (int i = 0; i < obsGrp.size(); i++) {
                JSONObject o = (JSONObject) obsGrp.get(i);
                Long groupingConcept = (Long) (o.get("groupingConcept"));// this value is read as Long
                JSONArray dsColumns = (JSONArray) o.get("datasetColumns"); // get col definitions

                Map<String, DatasetColumn> datasetColumns = new HashMap<String, DatasetColumn>();

                for (int j = 0; j < dsColumns.size(); j++) {
                    JSONObject colDef = (JSONObject) dsColumns.get(j);
                    String colName = (String) colDef.get("name");
                    Long colConceptQuestion = (Long) colDef.get("questionConcept");
                    String colConceptDataType = (String) colDef.get("dataType");
                    DatasetColumn cl = new DatasetColumn(colConceptQuestion.intValue(), colConceptDataType);
                    // key is column name, value is DatasetColumn object
                    datasetColumns.put(colName, new DatasetColumn(colConceptQuestion.intValue(), colConceptDataType));
                }

                GroupedObservations gObs = new GroupedObservations();
                gObs.setGroupConceptId(groupingConcept.intValue());
                gObs.setDatasetColumns(datasetColumns);
                grpObsForDataset.add(gObs);

            }
            return grpObsForDataset;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @return a LinkedHashMap <dataset, GroupedObsConfig>
     *     This is an ordered map
     */
    public static Map<String, String> getProcessingOrderAndGroupedObsConfig() {
        File configFile = OpenmrsUtil.getDirectoryInApplicationDataDirectory(Context.getAdministrationService().getGlobalProperty(GP_MIGRATION_CONFIG_DIR));
        String fullFilePath = configFile.getPath() + File.separator + "TemplateDatasetMap.json";
        JSONParser jsonParser = new JSONParser();
        try {
            //Read JSON file
            FileReader reader = new FileReader(fullFilePath);
            Object obj = jsonParser.parse(reader);

            JSONArray templateDatasetMap = (JSONArray) obj;
            Map<String, String> configMap = new LinkedHashMap<String, String>();

            for (int i = 0; i < templateDatasetMap.size(); i++) {
                JSONObject o = (JSONObject) templateDatasetMap.get(i);
                //every object has obsGroupConfig and dataset properties.
                configMap.put((String) o.get("dataset"), (String) o.get("obsGroupConfig"));
            }
            return configMap;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Process lab dataset using lab configuration file
     * @return
     */
    public static String processLabTestDataset(List<String> messages, String migrationDatabase) {
        String GP_LAB_DATASET_CONFIG_FILE = "lab_dataset_config_file.json";
        File configFile = OpenmrsUtil.getDirectoryInApplicationDataDirectory(Context.getAdministrationService().getGlobalProperty(GP_MIGRATION_CONFIG_DIR));
        String fullFilePath = configFile.getPath() + File.separator + GP_LAB_DATASET_CONFIG_FILE;
        JSONParser jsonParser = new JSONParser();
        try {
            //Read JSON file
            FileReader reader = new FileReader(fullFilePath);
            Object obj = jsonParser.parse(reader);

            JSONObject templateObject = (JSONObject) obj;
            String datasetName = (String) templateObject.get("datasetName");
            JSONArray templateDatasetMap = (JSONArray) templateObject.get("columns");
            Map<String, JSONObject> configMap = new LinkedHashMap<String, JSONObject>();

            for (int i = 0; i < templateDatasetMap.size(); i++) {
                JSONObject o = (JSONObject) templateDatasetMap.get(i);
                //every object has testName, conceptQuestion, and dataType properties.
                String testName = (String) o.get("testName");
                Long conceptQuestion = (Long) o.get("conceptQuestion");
                String dataType = (String) o.get("dataType");
                JSONObject col = new JSONObject();
                col.put("conceptQuestion", conceptQuestion);
                col.put("dataType", dataType);
                configMap.put(testName, col);
            }

            Connection conn = null;
            Statement s = null;

            try {

                // Connect to db
                Class.forName("com.mysql.jdbc.Driver").newInstance();

                Properties p = Context.getRuntimeProperties();
                String url = p.getProperty("connection.url");

                conn = DriverManager.getConnection(url, p.getProperty("connection.username"),
                        p.getProperty("connection.password"));
                conn.setAutoCommit(false);

                s = conn.createStatement();

                String query = "select * from :migrationDatabase.:labDataset";
                query = query.replace(":migrationDatabase", migrationDatabase);
                query = query.replace(":labDataset", datasetName);


                ResultSet rs = s.executeQuery(query);
                int recordCount = 0;

                while (rs.next()) {
                    String sql = null;

                    String encounterId = null;
                    Integer patientId = ((Long) rs.getLong("patient_id")).intValue();
                    Date encounterDate = rs.getDate("Encounter_Date");
                    ;
                    String orderNumber = rs.getString("OrderNumber");
                    ;
                    String urgency = rs.getString("Urgency");
                    ;
                    String LabTest = rs.getString("Lab_test");
                    ;
                    Date dateTestResultReceived = rs.getDate("Date_test_result_received");
                    Long value = rs.getLong("CD4PERCENT");

                    // if either of the order columns have value then create and order

                    /**
                     * for a lab order
                     * create/get a lab encounter on the date of order
                     * +----------------+------------+-------------+---------+---------------------+---------+---------------------+--------------------------------------+
                     | encounter_type | patient_id | location_id | form_id | encounter_datetime  | creator | date_created        | uuid                                 |
                     +----------------+------------+-------------+---------+---------------------+---------+---------------------+--------------------------------------+

                     * create order and assign the encounter - orders
                     * String TEST_ORDER_TYPE_UUID = "52a447d3-a64a-11e3-9aeb-50e549534c5e";
                     * +---------------+------------+---------+--------------+---------------------+---------------------+--------------+---------+--------------+--------------+--------------+----------------+
                     | order_type_id | concept_id | orderer | encounter_id | date_activated      | date_stopped        | order_reason | urgency | order_number | order_action | care_setting | order_group_id |
                     +---------------+------------+---------+--------------+---------------------+---------------------+--------------+---------+--------------+--------------+--------------+----------------+
                     |
                     *
                     */

                    /**
                     * create an encounter of type lab order
                     */

                    sql = "insert into encounter (date_created, uuid, creator, encounter_datetime, encounter_type, patient_id) " +
                            "values (now(), uuid(), ?, ?, ?, ?, ?);";

                    // create an order
                    /**
                     * order_action (text) = NEW if no result and DISCONTINUE is result is provided
                     * order_reason (coded) to be handled at transformation phase
                     * urgency (text) = ROUTINE or STAT and is text
                     * care_setting =
                     */
                    sql = "insert into orders (date_created, uuid, creator, order_type_id, concept_id, encounter_id, orderer, order_reason, urgency, order_action,date_activated, care_setting, patient_id) " +
                            "values (now(), uuid(), ?, ?, ?, ?, ?);";


                    // insert person query
                    sql = "insert into obs (date_created, uuid, creator, obs_datetime, concept_id, value_numeric, person_id) " +
                            "values (now(), uuid(), ?, ?, ?, ?, ?);";

                    PreparedStatement insertPerson = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    insertPerson.setInt(1, Context.getAuthenticatedUser().getId()); // set creator
                    insertPerson.setTimestamp(2, new java.sql.Timestamp(dateTestResultReceived.getTime()));
                    insertPerson.setInt(3, 730); // concept for CD4 percent
                    insertPerson.setDouble(4, value); // CD4 percent value
                    insertPerson.setInt(5, patientId); // set patient id
                    insertPerson.executeUpdate();
                    ResultSet returnedPerson = insertPerson.getGeneratedKeys();
                    returnedPerson.next();
                    patientId = returnedPerson.getInt(1);
                    returnedPerson.close();

                    recordCount++;
                    DbImportUtil.updateMigrationProgressMapProperty("Demographics", "processedCount", String.valueOf(recordCount));

                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (s != null) {
                    try {
                        s.close();
                    } catch (Exception e) {
                    }
                }
                if (conn != null) {
                    try {
                        conn.commit();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    try {
                        conn.close();
                    } catch (Exception e) {
                    }
                }
            }

            return "Success";

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String processViralLoadAndCD4Labs(List<String> messages, String migrationDatabase) {
        /*String GP_LAB_DATASET_CONFIG_FILE = "lab_dataset_config_file.json";
        File configFile = OpenmrsUtil.getDirectoryInApplicationDataDirectory(Context.getAdministrationService().getGlobalProperty(GP_MIGRATION_CONFIG_DIR));
        String fullFilePath = configFile.getPath() + File.separator + GP_LAB_DATASET_CONFIG_FILE;
        JSONParser jsonParser = new JSONParser();*/
        try {
            //Read JSON file
            /*FileReader reader = new FileReader(fullFilePath);
            Object obj = jsonParser.parse(reader);

            JSONObject templateObject = (JSONObject) obj;
            String datasetName = (String) templateObject.get("datasetName");
            JSONArray templateDatasetMap = (JSONArray) templateObject.get("columns");
            Map<String, JSONObject> configMap = new LinkedHashMap<String, JSONObject>();

            for (int i = 0; i < templateDatasetMap.size(); i++) {
                JSONObject o = (JSONObject) templateDatasetMap.get(i);
                //every object has testName, conceptQuestion, and dataType properties.
                String testName = (String) o.get("testName");
                Long conceptQuestion = (Long) o.get("conceptQuestion");
                String dataType = (String) o.get("dataType");
                JSONObject col = new JSONObject();
                col.put("conceptQuestion", conceptQuestion);
                col.put("dataType", dataType);
                configMap.put(testName, col);
            }*/

            Connection conn = null;
            Statement s = null;

            try {

                // Connect to db
                Class.forName("com.mysql.jdbc.Driver").newInstance();

                Properties p = Context.getRuntimeProperties();
                String url = p.getProperty("connection.url");

                conn = DriverManager.getConnection(url, p.getProperty("connection.username"),
                        p.getProperty("connection.password"));
                conn.setAutoCommit(false);

                s = conn.createStatement();

                String query = "select * from :migrationDatabase.:labDataset";
                query = query.replace(":migrationDatabase", migrationDatabase);
                query = query.replace(":labDataset", "tr_vital_labs");
                LabOrderDetails.setLabOrderDetails();
                LabOrderDetails labMetadata = LabOrderDetails.labOrderDetails;


                ResultSet rs = s.executeQuery(query);
                int recordCount = 0;

                String createEncounterSql = "insert into encounter (date_created, uuid, creator, encounter_datetime, " +
                        "encounter_type, patient_id) " +
                        "values (?, uuid(), ?, ?, ?, ?);";

                String createOrdersql = "insert into orders (date_created, uuid, creator, order_type_id, concept_id, " +
                        "encounter_id, orderer, order_reason, urgency, order_action,date_activated, care_setting,order_number, patient_id) " +
                        "values (?, uuid(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

                String createTestOrdersql = "insert into test_order (order_id) " +
                        "values (?);";

                String createNumericObsSql = "insert into obs (date_created, uuid, creator, obs_datetime, encounter_id, order_id, concept_id, value_numeric, person_id) " +
                        "values (?, uuid(), ?, ?, ?, ?, ?, ?, ?);";

                String createCodedObsSql = "insert into obs (date_created, uuid, creator, obs_datetime, encounter_id, order_id, concept_id, value_coded, person_id) " +
                        "values (?, uuid(), ?, ?, ?, ?, ?, ?, ?);";

                String getLabEncounterOnDaySql = "SELECT encounter_id from encounter where encounter_type=? and date(encounter_datetime)=date(?)  and patient_id=? limit 1";



                PreparedStatement insertEncounter = conn.prepareStatement(createEncounterSql, Statement.RETURN_GENERATED_KEYS);
                Integer orderEncounter = null;

                PreparedStatement insertOrder = conn.prepareStatement(createOrdersql, Statement.RETURN_GENERATED_KEYS);

                PreparedStatement insertTestOrder = conn.prepareStatement(createTestOrdersql, Statement.RETURN_GENERATED_KEYS);

                PreparedStatement insertNumericObs = conn.prepareStatement(createNumericObsSql, Statement.RETURN_GENERATED_KEYS);

                PreparedStatement insertCodedObs = conn.prepareStatement(createCodedObsSql, Statement.RETURN_GENERATED_KEYS);

                PreparedStatement getEncounterOnDay = conn.prepareStatement(getLabEncounterOnDaySql, Statement.RETURN_GENERATED_KEYS);


                while (rs.next()) {
                    String sql = null;

                    Integer encounterId = null;
                    Integer patientId = ((Long) rs.getLong("patient_id")).intValue();
                    Date encounterDate = rs.getDate("Encounter_Date");
                    String orderNumber = rs.getString("OrderNumber");
                    Integer orderReason = 161236;// rs.getInt("OrderReason");
                    String urgency = rs.getString("Urgency");
                    Integer labTest = ((Long) rs.getLong("Lab_test")).intValue();
                    String testResult = rs.getString("Test_result");
                    Date dateTestRequested = rs.getDate("Date_test_requested");
                    Date dateTestResultReceived = rs.getDate("Date_test_result_received");


                    Integer order = null;

                    if (StringUtils.isNotBlank(testResult)) { // handle lab result. create encounter, order, lab test, and obs for the result
                        if (StringUtils.isNotBlank(orderNumber) && dateTestResultReceived != null && dateTestRequested != null && encounterDate != null && patientId != null) {

                            getEncounterOnDay.setInt(1, labMetadata.getEncounterTypeId());
                            getEncounterOnDay.setDate(2, new java.sql.Date(encounterDate.getTime()));
                            getEncounterOnDay.setInt(3, patientId);


                            ResultSet rsGetEncounter = getEncounterOnDay.executeQuery();
                            if (rsGetEncounter.next()) {
                                orderEncounter = rsGetEncounter.getInt(1);
                                rsGetEncounter.close();
                            } else {

                                insertEncounter.setTimestamp(1, new java.sql.Timestamp(encounterDate.getTime())); // set date created
                                insertEncounter.setInt(2, Context.getAuthenticatedUser().getId()); // set creator
                                insertEncounter.setTimestamp(3, new java.sql.Timestamp(encounterDate.getTime()));
                                insertEncounter.setInt(4, labMetadata.getEncounterTypeId()); // set encounter type to lab test
                                insertEncounter.setInt(5, patientId); // set patient id
                                insertEncounter.executeUpdate();
                                ResultSet rsNewEncounter = insertEncounter.getGeneratedKeys();
                                rsNewEncounter.next();
                                orderEncounter = rsNewEncounter.getInt(1);
                                rsNewEncounter.close();
                            }

                            // create new order

                            if (StringUtils.isNotBlank(testResult) && labTest.equals(LabOrderDetails.HIV_VIRAL_LOAD)) {
                                insertOrder.setTimestamp(1, new java.sql.Timestamp(dateTestRequested.getTime())); // set date created
                                insertOrder.setInt(2, Context.getAuthenticatedUser().getId()); // set creator
                                insertOrder.setInt(3, labMetadata.getOrderTypeId()); //

                                // assign appropriate concept id
                                if (testResult.trim().equals("LDL")) {
                                    insertOrder.setInt(4, LabOrderDetails.HIV_VIRAL_LOAD_QUALITATIVE);
                                } else {
                                    insertOrder.setInt(4, LabOrderDetails.HIV_VIRAL_LOAD);
                                }

                                insertOrder.setInt(5, orderEncounter);
                                insertOrder.setInt(6, 1); // set provider TODO: set provider id
                                insertOrder.setInt(7, orderReason); // set order reason
                                insertOrder.setString(8, "ROUTINE"); // set order urgency
                                insertOrder.setString(9, "DISCONTINUE"); // set order action
                                insertOrder.setDate(10, new java.sql.Date(dateTestRequested.getTime())); // set date requested
                                insertOrder.setInt(11, labMetadata.getCareSettingId()); // set care setting
                                insertOrder.setString(12, orderNumber);
                                insertOrder.setInt(13, patientId);
                                insertOrder.executeUpdate();

                                ResultSet rsNewOrder = insertOrder.getGeneratedKeys();
                                rsNewOrder.next();
                                order = rsNewOrder.getInt(1);
                                rsNewOrder.close();
                            } else if (StringUtils.isNotBlank(testResult) && (labTest.equals(LabOrderDetails.CD4_COUNT) || labTest.equals(LabOrderDetails.CD4_PERCENT))){
                                insertOrder.setTimestamp(1, new java.sql.Timestamp(dateTestRequested.getTime())); // set date created
                                insertOrder.setInt(2, Context.getAuthenticatedUser().getId()); // set creator
                                insertOrder.setInt(3, labMetadata.getOrderTypeId()); //

                                // assign appropriate concept id. should translate to CD4 or CD4 percent
                                insertOrder.setInt(4, labTest);

                                insertOrder.setInt(5, orderEncounter);
                                insertOrder.setInt(6, Context.getAuthenticatedUser().getUserId()); // set provider
                                insertOrder.setInt(7, orderReason); // set order reason
                                insertOrder.setString(8, "ROUTINE"); // set order urgency
                                insertOrder.setString(9, "DISCONTINUE"); // set order action
                                insertOrder.setDate(10, new java.sql.Date(dateTestRequested.getTime())); // set date requested
                                insertOrder.setInt(11, labMetadata.getCareSettingId()); // set care setting
                                insertOrder.setInt(12, patientId);
                                insertOrder.executeUpdate();
                                ResultSet rsNewOrder = insertOrder.getGeneratedKeys();
                                rsNewOrder.next();
                                order = rsNewOrder.getInt(1);
                                rsNewOrder.close();
                            }

                            // create lab test
                            insertTestOrder.setInt(1, order);
                            insertTestOrder.executeUpdate();

                            // create obs with result
                            //                "insert into obs (date_created, uuid, creator, obs_datetime, encounter_id, order_id, concept_id, value_coded, patient_id) " +

                            PreparedStatement psObs = null;
                            if (testResult.trim().equals("LDL")) {
                                psObs = insertCodedObs;
                                psObs.setInt(6, LabOrderDetails.HIV_VIRAL_LOAD_QUALITATIVE);
                                psObs.setInt(7, LabOrderDetails.NOT_DETECTED);

                            } else {
                                psObs = insertNumericObs;
                                psObs.setInt(6, labTest);
                                psObs.setDouble(7, Double.parseDouble(testResult));
                            }

                            psObs.setTimestamp(1, new java.sql.Timestamp(dateTestRequested.getTime())); // set date created
                            psObs.setInt(2, Context.getAuthenticatedUser().getId()); // set creator
                            psObs.setTimestamp(3, new java.sql.Timestamp(dateTestResultReceived.getTime())); // set date created
                            psObs.setInt(4, orderEncounter);
                            psObs.setInt(5, order);

                            psObs.setInt(8, patientId);
                            psObs.executeUpdate();

                        }

                    } else { // handle a pending order result
                        if (labTest.equals(LabOrderDetails.HIV_VIRAL_LOAD)) {

                        } else {

                        }

                    }

                    recordCount++;
                    DbImportUtil.updateMigrationProgressMapProperty("Lab (VL and CD4)", "processedCount", String.valueOf(recordCount));

                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (s != null) {
                    try {
                        s.close();
                    } catch (Exception e) {
                    }
                }
                if (conn != null) {
                    try {
                        conn.commit();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    try {
                        conn.close();
                    } catch (Exception e) {
                    }
                }
            }

            return "Success";

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean addOrderWithResult(PreparedStatement sAddEncounter,
                                       PreparedStatement sAddOrder,
                                       PreparedStatement sAddLabOrder,
                                       PreparedStatement sAddObs,
                                       ResultSet rsResult) {

        try {
            Integer patientId = ((Long) rsResult.getLong("patient_id")).intValue();
            Date encounterDate = rsResult.getDate("Encounter_Date");
            String orderNumber = rsResult.getString("OrderNumber");
            Integer orderReason = rsResult.getInt("OrderReason");
            String urgency = rsResult.getString("Urgency");
            Integer labTest = ((Long) rsResult.getLong("Lab_test")).intValue();
            String testResult = rsResult.getString("Lab_test");
            Date dateTestRequested = rsResult.getDate("Date_test_requested");
            Date dateTestResultReceived = rsResult.getDate("Date_test_result_received");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean addOrderWithoutResult() {
        return false;
    }

    /**
     * Process system users
     * @param messages
     * @param migrationDatabase
     * @return
     */
    public static String processUsers(List<String> messages, String migrationDatabase) {

        try {

            Connection conn = null;
            Statement s = null;

            try {

                // Connect to db
                Class.forName("com.mysql.jdbc.Driver").newInstance();

                Properties p = Context.getRuntimeProperties();
                String url = p.getProperty("connection.url");

                conn = DriverManager.getConnection(url, p.getProperty("connection.username"),
                        p.getProperty("connection.password"));
                conn.setAutoCommit(false);

                s = conn.createStatement();

                String query = "select * from :migrationDatabase.:userDataset";
                query = query.replace(":migrationDatabase", migrationDatabase);
                query = query.replace(":userDataset", "tr_users");

                ResultSet rs = s.executeQuery(query);
                int recordCount = 0;

                String updateGeneratedUserIdSql = "update :migrationDatabase.:userDataset set OpenMRS_User_Id=? where User_Id=?";
                updateGeneratedUserIdSql = updateGeneratedUserIdSql.replace(":migrationDatabase", migrationDatabase);
                updateGeneratedUserIdSql = updateGeneratedUserIdSql.replace(":userDataset", "tr_users");

                PreparedStatement updateUserDetails = conn.prepareStatement(updateGeneratedUserIdSql, Statement.RETURN_GENERATED_KEYS);
                UserService us = Context.getUserService();
                ProviderService ps = Context.getProviderService();

                String clinicianDesignation = "Physician/Clinical Officer";
                String clinicianGroupName = "Clinical Staff";
                String dataManagerDesignation = "Data Manager";
                String dataManagerGroupName = "Data Managers";
                String triageGroupName = "Triage";

                while (rs.next()) {
                    Integer userId = ((Long) rs.getLong("User_Id")).intValue();
                    String firstName = rs.getString("First_Name");
                    String lastName = rs.getString("Last_Name");
                    String userName = rs.getString("User_Name");
                    String status = rs.getString("Status");
                    String designation = rs.getString("Designation");
                    String groupNames = rs.getString("GroupNames");

                    if (StringUtils.isNotBlank(firstName) && StringUtils.isNotBlank(lastName) && StringUtils.isNotBlank(userName)) {
                        String generatedPassword = userName + "12Dd001";
                        User u = new User();
                        u.setPerson(new Person());

                        u.addName(new PersonName(firstName, null, lastName));
                        userName = userName.replace(" ", "");
                        u.setUsername(userName);
                        u.getPerson().setGender("U");
                        Calendar c = Calendar.getInstance();
                        c.set(1970, 3, 3);
                        u.getPerson().setBirthdate(c.getTime());
                        u.getPerson().setBirthdateEstimated(true);
                        if (StringUtils.isNotBlank(status) && status.equals("InActive")) {
                            u.setRetired(true);
                            u.setRetireReason("Marked as inactive during migration");
                        }

                        if ((StringUtils.isNotBlank(designation) && designation.trim().contains(clinicianDesignation)) ||
                                        (StringUtils.isNotBlank(groupNames) && groupNames.trim().contains(clinicianGroupName))

                                ) {
                            Role role = us.getRole("Clinician");
                            u.addRole(role);
                        } else if ((StringUtils.isNotBlank(designation) && designation.trim().contains(dataManagerDesignation)) ||
                                (StringUtils.isNotBlank(groupNames) && groupNames.trim().contains(dataManagerGroupName))

                                ) {
                            Role role = us.getRole("Data Manager");
                            u.addRole(role);
                        } else if (StringUtils.isNotBlank(groupNames) && groupNames.trim().contains(triageGroupName)) {
                            Role rRole = us.getRole("Registration");
                            Role iRole = us.getRole("Intake");
                            Role dRole = us.getRole("Data Clerk");
                            u.addRole(rRole);
                            u.addRole(iRole);
                            u.addRole(dRole);
                        }

                        User createdUser = us.saveUser(u, generatedPassword);

                        // update user details
                        Integer generatedUserId = createdUser.getUserId();

                        updateUserDetails.setInt(1, generatedUserId);
                        updateUserDetails.setInt(2, userId);

                        updateUserDetails.executeUpdate();
                        ResultSet rsUpdatedUser = updateUserDetails.getGeneratedKeys();
                        rsUpdatedUser.next();
                        rsUpdatedUser.close();
                        if (
                                (StringUtils.isNotBlank(designation) && designation.trim().contains(clinicianDesignation)) ||
                                (StringUtils.isNotBlank(groupNames) && groupNames.trim().contains(clinicianGroupName)) ||
                                (StringUtils.isNotBlank(groupNames) && groupNames.trim().contains(triageGroupName))

                                ) {
                            Provider provider = new Provider();
                            provider.setIdentifier(createdUser.getSystemId());
                            provider.setPerson(createdUser.getPerson());
                            ps.saveProvider(provider);

                        }
                    }
                    recordCount++;
                    DbImportUtil.updateMigrationProgressMapProperty("Users", "processedCount", String.valueOf(recordCount));
                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (s != null) {
                    try {
                        s.close();
                    } catch (Exception e) {
                    }
                }
                if (conn != null) {
                    try {
                        conn.commit();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    try {
                        conn.close();
                    } catch (Exception e) {
                    }
                }
            }

            messages.add("Processing users was successful.");
            return "Success";

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Process patient relationships
     * @param messages
     * @param migrationDatabase
     * @return
     */
    public static String processPatientRelationships(List<String> messages, String migrationDatabase) {

        try {

            Connection conn = null;
            Statement s = null;

            try {

                // Connect to db
                Class.forName("com.mysql.jdbc.Driver").newInstance();

                Properties p = Context.getRuntimeProperties();
                String url = p.getProperty("connection.url");

                conn = DriverManager.getConnection(url, p.getProperty("connection.username"),
                        p.getProperty("connection.password"));
                conn.setAutoCommit(false);

                s = conn.createStatement();

                String query = "select * from :migrationDatabase.:relationshipDataset";
                query = query.replace(":migrationDatabase", migrationDatabase);
                query = query.replace(":relationshipDataset", "st_relationship");

                ResultSet rs = s.executeQuery(query);
                int recordCount = 0;

                String addRelationshipQuery = "insert into relationship (date_created, uuid, creator, person_a, " +
                        "relationship, person_b) " +
                        "values (now(), uuid(), ?, ?, ?, ?);";

                PreparedStatement addRelationshipDetails = conn.prepareStatement(addRelationshipQuery, Statement.RETURN_GENERATED_KEYS);

                while (rs.next()) {
                    Integer personA = ((Long) rs.getLong("Person_a_person_id")).intValue();
                    Integer personB = ((Long) rs.getLong("person_b_person_id")).intValue();
                    Integer relationshipType = ((Long) rs.getLong("openmrs_relationship_type")).intValue();


                    if (personA != null && personB != null && relationshipType != null) {
                        addRelationshipDetails.setInt(1, Context.getAuthenticatedUser().getId()); // set creator
                        addRelationshipDetails.setInt(2, personA);
                        addRelationshipDetails.setInt(3, relationshipType);
                        addRelationshipDetails.setInt(4, personB);

                        addRelationshipDetails.executeUpdate();
                        ResultSet rsNewRelationship = addRelationshipDetails.getGeneratedKeys();
                        rsNewRelationship.next();
                        rsNewRelationship.close();

                    }
                    recordCount++;
                    DbImportUtil.updateMigrationProgressMapProperty("Patient Relationships", "processedCount", String.valueOf(recordCount));
                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (s != null) {
                    try {
                        s.close();
                    } catch (Exception e) {
                    }
                }
                if (conn != null) {
                    try {
                        conn.commit();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    try {
                        conn.close();
                    } catch (Exception e) {
                    }
                }
            }

            messages.add("Processing relationships was successful.");
            return "Success";

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    public static void setRowCountForDatasets(String migrationDatabase) {

        MysqlDataSource dataSource = null;
        Connection conn = null;
        Statement s = null;
        Map<String, Properties> datasetRowCountMap = new LinkedHashMap<String, Properties>();
        String demographicsTableName = "tr_demographics";
        String usersTableName = "tr_users";

        try {
            Map<String, Integer> templateMap = getTemplateDatasetMap();

            Properties p = Context.getRuntimeProperties();
            String url = p.getProperty("connection.url");

            dataSource = new MysqlDataSource();
            dataSource.setURL(url);
            dataSource.setUser(p.getProperty("connection.username"));
            dataSource.setPassword(p.getProperty("connection.password"));

            conn = dataSource.getConnection();
            s = conn.createStatement();
            SpreadsheetImportService spreadsheetImportService = Context.getService(SpreadsheetImportService.class);

            // process for users

            String usersQuery = "select count(*) as rowCount from :migrationDatabase.:tableName";
            usersQuery = usersQuery.replace(":migrationDatabase", migrationDatabase);
            usersQuery = usersQuery.replace(":tableName", usersTableName);
            ResultSet rsUsers = s.executeQuery(usersQuery);
            rsUsers.next();
            DbImportUtil.updateMigrationProgressMapProperty("Users", "totalRowCount", String.valueOf(rsUsers.getInt("rowCount")));
            DbImportUtil.updateMigrationProgressMapProperty("Users", "processedCount", String.valueOf(0));
            rsUsers.close();

            // process for demographics

            String query = "select count(*) as rowCount from :migrationDatabase.:tableName";
            query = query.replace(":migrationDatabase", migrationDatabase);
            query = query.replace(":tableName", demographicsTableName);
            ResultSet rs = s.executeQuery(query);
            rs.next();
            DbImportUtil.updateMigrationProgressMapProperty("Demographics", "totalRowCount", String.valueOf(rs.getInt("rowCount")));
            DbImportUtil.updateMigrationProgressMapProperty("Demographics", "processedCount", String.valueOf(0));
            rs.close();

            // add other datasets
            for (Map.Entry<String, Integer> entry : templateMap.entrySet()) {


                String countQuery = "select count(*) as rowCount from :migrationDatabase.:tableName";
                countQuery = countQuery.replace(":migrationDatabase", migrationDatabase);
                countQuery = countQuery.replace(":tableName", entry.getKey());
                ResultSet countRs = s.executeQuery(countQuery);
                countRs.next();
                SpreadsheetImportTemplate template = spreadsheetImportService.getTemplateById(entry.getValue());
                DbImportUtil.updateMigrationProgressMapProperty(template.getName(), "totalRowCount", String.valueOf(countRs.getInt("rowCount")));
                DbImportUtil.updateMigrationProgressMapProperty(template.getName(), "processedCount", String.valueOf(0));

                countRs.close();
            }

            String labQuery = "select count(*) as rowCount from :migrationDatabase.tr_vital_labs";
            labQuery = labQuery.replace(":migrationDatabase", migrationDatabase);
            ResultSet rsLabs = s.executeQuery(labQuery);
            rsLabs.next();
            DbImportUtil.updateMigrationProgressMapProperty("Lab (VL and CD4)", "totalRowCount", String.valueOf(rsLabs.getInt("rowCount")));
            DbImportUtil.updateMigrationProgressMapProperty("Lab (VL and CD4)", "processedCount", String.valueOf(0));
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (Exception e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                }
            }
        }

    }
/*    protected boolean validatePatientIdentifier(String identifier) {
        String pitId = getPrespecifiedPatientIdentifierTypeIdFromPatientIdentifierColumn(piColumn);
        if (pitId == null)
            throw new SpreadsheetImportTemplateValidationException("no prespecified patient identifier type ID");

        sql = "select format from patient_identifier_type where patient_identifier_type_id = " + pitId;
        System.out.println("Identifier sql: " + sql);
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
    }*/
}
