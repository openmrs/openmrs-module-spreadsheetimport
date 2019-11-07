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
import org.apache.commons.validator.GenericValidator;
import org.openmrs.api.context.Context;
import org.openmrs.module.spreadsheetimport.service.SpreadsheetImportService;
import org.springframework.web.multipart.MultipartFile;

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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class DbImportUtil {

    /** Logger for this class and subclasses */
    protected static final Log log = LogFactory.getLog(SpreadsheetImportUtil.class);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");


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
                        columnsImportFirst.add(((TreeSet<SpreadsheetImportTemplateColumn>)mapUiToCs.get(uniqueImport)).first());
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

    public static String importTemplate(SpreadsheetImportTemplate template, MultipartFile file, String sheetName,
                                        List<String> messages, boolean rollbackTransaction) throws Exception {

        if (file.isEmpty()) {
            messages.add("file must not be empty");
            return null;
        }



        Connection conn = null;
        Statement s = null;
        String sql = null;
        List<String> columnNames = new Vector<String>();

        Map<Integer, String> tableToTemplateMap = new HashMap<Integer, String>();
        tableToTemplateMap.put(10,"demographics");
        tableToTemplateMap.put(9, "hiv_patient_program");
        tableToTemplateMap.put(8, "tr_hiv_enrollment");
        tableToTemplateMap.put(5, "triage_encounter");
        tableToTemplateMap.put(7, "hiv_testing_initial_encounter");

        String tableToProcess = tableToTemplateMap.get(template.getId());


        try {

            System.out.println("Attempting to read from the migration database!");

            // Connect to db
            Class.forName("com.mysql.jdbc.Driver").newInstance();

            Properties p = Context.getRuntimeProperties();
            String url = p.getProperty("connection.url");

            conn = DriverManager.getConnection(url, p.getProperty("connection.username"),
                    p.getProperty("connection.password"));

            //conn.setAutoCommit(false);

            s = conn.createStatement();

            DatabaseMetaData dmd = conn.getMetaData();
            ResultSet rsColumns = dmd.getColumns("migration_tr", null, tableToProcess, null);

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
        // remove the extra encounter date column provided
        if (columnNamesOnlyInSheet.contains("Encounter_Date")) {
            columnNamesOnlyInSheet.remove("Encounter_Date");
        }

        if (columnNamesOnlyInSheet.isEmpty() == false) {
            messages.add("Extra column names present, these will not be processed: " + toString(columnNamesOnlyInSheet));
        }

        // Process rows


        String tableName = tableToTemplateMap.get(template.getId());
        String query = "select * from migration_tr.:tableName";
        query = query.replace(":tableName", tableName);
        //System.out.println("Resulting table name query: " + query);
        ResultSet rs = s.executeQuery(query);

        while (rs.next()) {

            boolean rowHasData = false;
            // attempt to process the extra encounter_datetime
            String encounterDateColumn = "Encounter_Date";
            String mainIdentifierColumn = "Person_Id";
            String patientIdColVal = rs.getString(mainIdentifierColumn);
            String patientId = null;

            // ==========================
            String patientIdsql = "select patient_id from patient_identifier where identifier = " + patientIdColVal + " and identifier_type=16";

            Statement getPatientSt = conn.createStatement();
            ResultSet getPatientRs = getPatientSt.executeQuery(patientIdsql);
            if (getPatientRs.next()) {
                patientId = getPatientRs.getString(1);

            }
            if (getPatientRs != null) {
                getPatientRs.close();
            }

            // ==========================
            String rowEncDate = null;
            int encDateColumnIdx = columnNames.indexOf(encounterDateColumn);

            if (encDateColumnIdx >= 0) {
                //Cell encDateCell = row.getCell(encDateColumnIdx);
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
                        if (value !=null && !value.equals("")) {
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



            if (rowHasData) {
                Exception exception = null;
                try {
                    DatabaseBackend.validateData(rowData);
                    String encounterId = DatabaseBackend.importData(rowData, rowEncDate, patientId, rollbackTransaction);
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

        }


        return "Successful import";
    }

    public static String processDemographicsDataset(List<String> messages) {

        /**
         * compose mapping template for the demographics metadata
         */

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



        Map<String,String> columnMap = new HashMap<String, String>(); // stores table.column mapping for template columns
        Map<String,String> columnConfigMap = new HashMap<String, String>();// stores type uuid

        columnMap.put("First_Name","person_name.given_name");
        columnMap.put("Middle_Name","person_name.middle_name");
        columnMap.put("Last_Name","person_name.family_name");
        columnMap.put("DOB","birthdate");
        columnMap.put("Sex","gender");

        // identifiers
        columnMap.put("UPN","patient_identifier.identifier");
        columnMap.put("National_id_no","patient_identifier.identifier");

        columnConfigMap.put("UPN",UNIQUE_PATIENT_NUMBER);
        columnConfigMap.put("National_id_no",NATIONAL_ID);


        // addresses
        columnMap.put("Postal_Address","person_address.address1");
        columnMap.put("County","person_address.county_district");
        columnMap.put("Sub_county","person_address.state_province");
        columnMap.put("Ward","person_address.address4");
        columnMap.put("Village","person_address.city_village");
        columnMap.put("Landmark","person_address.address2");

        //death status
        columnMap.put("Dead","dead");
        columnMap.put("Death_date","person.dead");

        // person attributes
        columnMap.put("Phone_number","person_attribute.value");
        columnMap.put("Alternate_Phone_number","person_attribute.value");
        columnMap.put("Email_address","person_attribute.value");
        columnMap.put("Nearest_Health_Center","person_attribute.value");

        columnConfigMap.put("Phone_number", TELEPHONE_CONTACT);
        columnConfigMap.put("Alternate_Phone_number", ALTERNATE_PHONE_CONTACT);
        columnConfigMap.put("Email_address", EMAIL_ADDRESS);
        columnConfigMap.put("Nearest_Health_Center", NEAREST_HEALTH_CENTER);


        // obs
        columnMap.put("Marital_status","obs.value_coded");
        columnMap.put("Occupation","obs.value_coded");
        columnMap.put("Education_level","obs.value_coded");

        columnConfigMap.put("Marital_status", "1054");
        columnConfigMap.put("Occupation", "1542");
        columnConfigMap.put("Education_level", "1712");


        Connection conn = null;
        Statement s = null;
        Exception exception = null;
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

            String sqlIQCareNumberStr = "select patient_identifier_type_id from patient_identifier_type where uuid='" + IQCARE_PERSON_PK +"'";
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
            //String sqlUpnStr = "select patient_identifier_type_id from patient_identifier_type where uuid='" + UNIQUE_PATIENT_NUMBER +"'";
            String sqlUpnStr = "select patient_identifier_type_id from patient_identifier_type where uuid='" + UNIQUE_PATIENT_NUMBER +"'";
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
            String sqlNatIdStr = "select patient_identifier_type_id from patient_identifier_type where uuid='" + NATIONAL_ID +"'";
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

            String query = "select * from migration_tr.tr_demographics";

            ResultSet rs = s.executeQuery(query);

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



                // extract dob
                dob = rs.getDate(COL_DOB);
                //dob = "'" + new java.sql.Timestamp(dob.getTime()).toString() + "'";


                // extract gender
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
                //rs.close(); //


                String columnNames = "";
                String columnValues = "";

                columnNames += "date_created";
                columnValues += "now()";

                // creator
                columnNames += ",creator";
                columnValues += "," + Context.getAuthenticatedUser().getId();

                // uuids
                columnNames += ",uuid";
                columnValues += ",uuid()";

                // process into person table
                if (sex != null) {
                    columnNames += ",gender";
                    columnValues += ",'" + sex + "'";
                }

                if (dob != null) {
                    columnNames += ",birthdate";
                    //"'" + new java.sql.Timestamp(date.getTime()).toString() + "'"
                    columnValues += ",'" + new java.sql.Timestamp(dob.getTime()).toString() + "'";
                }


                // insert person query
                sql = "insert into person (" + columnNames + ")" + " values ("
                        + columnValues + ")";

                //System.out.println(" person query: " + sql);

                Statement insertPerson = conn.createStatement();
                insertPerson.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
                ResultSet returnedPerson = insertPerson.getGeneratedKeys();
                returnedPerson.next();
                patientId = returnedPerson.getInt(1);
                returnedPerson.close();

                // process into person name
                columnNames = "";
                columnValues = "";

                columnNames += "date_created";
                columnValues += "now()";

                // creator
                columnNames += ",creator";
                columnValues += "," + Context.getAuthenticatedUser().getId();

                // uuids
                columnNames += ",uuid";
                columnValues += ",uuid()";

                // person_id
                columnNames += ",person_id";
                columnValues += "," + patientId.intValue();

                if (fName != null && !fName.equals("")) {
                    // first name
                    columnNames += ",given_name";
                    columnValues += ",\"" + fName + "\"";
                }

                if (mName != null && !mName.equals("")) {
                    // middle name
                    columnNames += ",middle_name";
                    columnValues += ",\"" + mName + "\"";
                }
                if (lName != null && !lName.equals("")) {
                    // family name
                    columnNames += ",family_name";
                    columnValues += ",\"" + lName + "\"";
                }

                // insert person query
                sql = "insert into person_name (" + columnNames + ")" + " values ("
                        + columnValues + ")";
                //System.out.println(" person name query: " + sql + ",columns: " + columnValues);

                // execute query
                Statement insertPersonName = conn.createStatement();
                //PreparedStatement insertPersonStatement= conn.prepareStatement(sql );
                //insertPersonStatement.setString(1, columnValues);

                insertPersonName.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
                //insertPersonStatement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
                ResultSet returnedPersonName = insertPersonName.getGeneratedKeys();
                returnedPersonName.next();
                //patientId = returnedPersonName.getInt(1);
                returnedPersonName.close();

                //=================================

                // process person address
                columnNames = "";
                columnValues = "";


                columnNames += "date_created";
                columnValues += "now()";

                // creator
                columnNames += ",creator";
                columnValues += "," + Context.getAuthenticatedUser().getId();

                // uuids
                columnNames += ",uuid";
                columnValues += ",uuid()";

                // person_id
                columnNames += ",person_id";
                columnValues += "," + patientId.intValue();

                if (county != null && !county.equals("")) {
                    // family name
                    columnNames += ",county_district";
                    columnValues += ",\"" + county + "\""; // use double quotes to take care of apostrophe in the text
                }

                if (subCounty != null && !subCounty.equals("")) {
                    // family name
                    columnNames += ",state_province";
                    columnValues += ",\"" + subCounty + "\"";
                }

                if (ward != null && !ward.equals("")) {
                    // family name
                    columnNames += ",address4";
                    columnValues += ",\"" + ward + "\"";
                }

                if (village != null && !village.equals("")) {
                    // family name
                    columnNames += ",city_village";
                    columnValues += ",\"" + village + "\"";
                }

                if (landMark != null && !landMark.equals("")) {
                    // family name
                    columnNames += ",address2";
                    columnValues += ",\"" + landMark + "\"";
                }

                if (postalAddress != null && !postalAddress.equals("")) {
                    // family name
                    columnNames += ",address1";
                    columnValues += ",\"" + postalAddress + "\"";
                }

                // insert into person address
                sql = "insert into person_address (" + columnNames + ")" + " values ("
                        + columnValues + ")";


                Statement insertPersonAddress = conn.createStatement();
                insertPersonAddress.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
                ResultSet returnedPersonAddress = insertPersonAddress.getGeneratedKeys();
                returnedPersonAddress.next();
                returnedPersonAddress.close();

                // insert into patient table

                sql = "insert into patient (patient_id, creator,date_created) values (" + patientId.intValue() + ", " + Context.getAuthenticatedUser().getId()+ ", now()" + ")";
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
                        columnNames = "";
                        columnValues = "";

                        columnNames += "date_created";
                        columnValues += "now()";

                        // creator
                        columnNames += ",creator";
                        columnValues += "," + Context.getAuthenticatedUser().getId();

                        // uuids
                        columnNames += ",uuid";
                        columnValues += ",uuid()";

                        // patient_id
                        columnNames += ",patient_id";
                        columnValues += "," + patientId.intValue();

                        // location_id
                        columnNames += ", location_id";
                        columnValues += ", NULL";

                        columnNames += ",identifier_type";
                        columnValues += "," + idTypeId.intValue();

                        columnNames += ",identifier";
                        columnValues += ",'" + colValue + "'";
                        sql = "insert into patient_identifier (" + columnNames + ")" + " values ("
                                + columnValues + ")";

                        Statement insertIdentifierStatement = conn.createStatement();
                        insertIdentifierStatement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
                        ResultSet returnedId = insertIdentifierStatement.getGeneratedKeys();
                        returnedId.next();
                        returnedId.close();

                    }
                }
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
                }
                catch (Exception e) {}
            }
            if (conn != null) {
                try {
                    conn.commit();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                try {
                    conn.close();
                }
                catch (Exception e) {}
            }
        }


        return "Successful";
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
