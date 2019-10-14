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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.openmrs.api.context.Context;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
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
        Map<String, Integer> columnPosition = new HashMap<String, Integer>();


        try {

            System.out.println("Attempting to read from the migration database!========");

            // Connect to db
            Class.forName("com.mysql.jdbc.Driver").newInstance();

            Properties p = Context.getRuntimeProperties();
            String url = p.getProperty("connection.url");

            conn = DriverManager.getConnection(url, p.getProperty("connection.username"),
                    p.getProperty("connection.password"));

            //conn.setAutoCommit(false);

            s = conn.createStatement();

            DatabaseMetaData dmd = conn.getMetaData();
            ResultSet rsColumns = dmd.getColumns("data_migration", null, "demographics", null);

            while (rsColumns.next()) {
                String colName = rsColumns.getString("COLUMN_NAME");
                int colPos = Integer.valueOf(rsColumns.getString("ORDINAL_POSITION"));
                columnNames.add(colName);
                columnPosition.put(colName, colPos);
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
        if (columnNamesOnlyInSheet.isEmpty() == false) {
            messages.add("Extra column names present, these will not be processed: " + toString(columnNamesOnlyInSheet));
        }

        // Process rows
        // testing with demographics table in migration database
        String query = "select `First Name`, `DOB`,`Middle Name`,`Last Name`,Sex,UPN,County,`Clinic Number` from data_migration.demographics";
        ResultSet rs = s.executeQuery(query);
        boolean skipThisRow = true;
        //for (Row row : sheet) {
        while (rs.next()) {

            boolean rowHasData = false;
            // attempt to process the extra encounter_datetime
            String encounterDateColumn = "Encounter Date";
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
                        value = "'" + rs.getString(column.getName()) + "'";
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
                        System.out.println("Importing column with value: " + value);
                    } else
                        column.setValue("");
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
                    String encounterId = DatabaseBackend.importData(rowData, rowEncDate, rollbackTransaction);
                    if (encounterId != null) {
                        for (UniqueImport uniqueImport : rowData.keySet()) {
                            Set<SpreadsheetImportTemplateColumn> columnSet = rowData.get(uniqueImport);
                            for (SpreadsheetImportTemplateColumn column : columnSet) {
                                //Write generated encounter_id in the Encounter ID column
                                if ("encounter".equals(column.getTableName())) {
                                    System.out.println("New encounter: " + encounterId);
                                }
                            }
                        }
                    }
                } catch (SpreadsheetImportTemplateValidationException e) {
                    messages.add("Validation failed: " + e.getMessage());
                    return null;
                } catch (SpreadsheetImportDuplicateValueException e) {
                    messages.add("found duplicate value for column " + e.getColumn().getName() + " with value " + e.getColumn().getValue());
                    return null;
                } catch (SpreadsheetImportSQLSyntaxException e) {
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
}
