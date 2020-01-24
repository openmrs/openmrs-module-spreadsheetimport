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
package org.openmrs.module.spreadsheetimport.web.controller;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.spreadsheetimport.DbImportUtil;
import org.openmrs.module.spreadsheetimport.SpreadsheetImportTemplate;
import org.openmrs.module.spreadsheetimport.service.SpreadsheetImportService;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.web.WebConstants;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This controller backs and saves the Spreadsheet Import module settings
 */
@Controller
@RequestMapping("/module/spreadsheetimport/spreadsheetimportprocessdatasets.form")
public class SpreadsheetImportProcessDataFormController {

    /**
     * Logger for this class
     */
    protected final Log log = LogFactory.getLog(getClass());
    List<String> messages = new ArrayList<String>();
    boolean rollbackTransaction = false;



    @RequestMapping(value = "/module/spreadsheetimport/spreadsheetimportprocessdatasets.form", method = RequestMethod.GET)
    public String setupForm(ModelMap model,
                            final HttpServletRequest request) {
        return "/module/spreadsheetimport/spreadsheetimportProcessDataForm";
    }

    @RequestMapping(params = "All", value = "/module/spreadsheetimport/spreadsheetimportprocessdatasets.form", method = RequestMethod.POST)
    public String processAllDatasets(ModelMap model,
                                     HttpServletRequest request,
                                     HttpServletResponse response) throws Exception {


        String successfulProcessMsg = "";
        successfulProcessMsg = DbImportUtil.processDemographicsDataset(messages);
        System.out.println("Completed processing demographics ");
        doPostDemographics();

        processOtherDatasets();

        boolean succeeded = (successfulProcessMsg != null);

        String messageString = "";
        for (int i = 0; i < messages.size(); i++) {
            if (i != 0) {
                messageString += "<br />";
            }
            messageString += messages.get(i);
        }
        if (succeeded) {
            messageString += "<br />Successfully processed all datasets!";
        }

        if (!messageString.isEmpty()) {
            if (succeeded) {
                request.getSession().setAttribute(WebConstants.OPENMRS_MSG_ATTR, messageString);
            } else {
                request.getSession().setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "Error processing request, " + messageString);
            }
        }

        return "/module/spreadsheetimport/spreadsheetimportProcessDataForm";
    }

    // handle demographics

    @RequestMapping(params = "Demographics", value = "/module/spreadsheetimport/spreadsheetimportprocessdatasets.form", method = RequestMethod.POST)
    public String processDemographics(ModelMap model,
                                      HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {

        String successfulProcessMsg = "";
        successfulProcessMsg = DbImportUtil.processDemographicsDataset(messages);
        System.out.println("Completed processing demographics ");

        doPostDemographics();

        boolean succeeded = (successfulProcessMsg != null);

        String messageString = "";
        for (int i = 0; i < messages.size(); i++) {
            if (i != 0) {
                messageString += "<br />";
            }
            messageString += messages.get(i);
        }
        if (succeeded) {
            messageString += "<br />Successfully processed demographics!";
        }

        if (!messageString.isEmpty()) {
            if (succeeded) {
                request.getSession().setAttribute(WebConstants.OPENMRS_MSG_ATTR, messageString);
            } else {
                request.getSession().setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "Error processing request, " + messageString);
            }
        }

        return "/module/spreadsheetimport/spreadsheetimportProcessDataForm";
    }

    // process other datasets

    @RequestMapping(params = "Others", value = "/module/spreadsheetimport/spreadsheetimportprocessdatasets.form", method = RequestMethod.POST)
    public String processOtherDatasets(ModelMap model,
                                       HttpServletRequest request,
                                       HttpServletResponse response) throws Exception {

       processOtherDatasets();

        boolean succeeded = true;

        String messageString = "";
        for (int i = 0; i < messages.size(); i++) {
            if (i != 0) {
                messageString += "<br />";
            }
            messageString += messages.get(i);
        }
        if (succeeded) {
            messageString += "<br />Successfully processed other datasets!";
        }

        if (!messageString.isEmpty()) {
            if (succeeded) {
                request.getSession().setAttribute(WebConstants.OPENMRS_MSG_ATTR, messageString);
            } else {
                request.getSession().setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "Error processing request, " + messageString);
            }
        }

        return "/module/spreadsheetimport/spreadsheetimportProcessDataForm";
    }


    private String getMigrationPrimaryIdentifierType() {

        Connection conn = null;

        //TODO: add this identifier type as global property
        String IQCARE_PERSON_PK_ID_TYPE = "b3d6de9f-f215-4259-9805-8638c887e46b";
        String mainPtIdType = null;
        String mainIdQry = "select patient_identifier_type_id from patient_identifier_type where uuid='" + IQCARE_PERSON_PK_ID_TYPE + "'";

        try {

            conn = getDbConnection();
            Statement getPatientSt = conn.createStatement();

            ResultSet mainIdentifieryType = getPatientSt.executeQuery(mainIdQry);
            if (mainIdentifieryType.next()) {
                mainPtIdType = mainIdentifieryType.getString(1);

            }
            if (mainIdentifieryType != null) {
                mainIdentifieryType.close();
            }
        } catch (Exception e) {

        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {

            }
        }
        return mainPtIdType;
    }


    /**
     * Establishes db connection using details from openmrs runtime property.
     * @return Connection
     */
    private Connection getDbConnection() {
        Properties p = Context.getRuntimeProperties();
        String url = p.getProperty("connection.url");
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL(url);
        dataSource.setUser(p.getProperty("connection.username"));
        dataSource.setPassword(p.getProperty("connection.password"));
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Handles housekeeping after processing demographics.
     * These include:
     * 1. adding patient_id column to all datasets
     * 2. add index on Person_Id column which holds an identifier from source database
     * 3. updates patient_id column with patient_id values generated by OpenMRS after processing demographics
     *
     * The method executes a sql script provided in the OpenMRS app data directory
     */
    private void doPostDemographics() {
        ResourceDatabasePopulator rdp = new ResourceDatabasePopulator();
        String fullFilePath = OpenmrsUtil.getApplicationDataDirectory() + "post_demographics_processing_query.sql";
        rdp.addScript(new FileSystemResource(fullFilePath));
        rdp.setSqlScriptEncoding("UTF-8");
        rdp.setIgnoreFailedDrops(true);
        Connection conn = null;

        try {
            conn = getDbConnection();
            rdp.populate(conn);
            System.out.println("Completed running post-demographics housekeeping script");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void processOtherDatasets() {
        // get processing order for datasets and associated configs for grouped obs

        Map<String, String> datasetMap = DbImportUtil.getProcessingOrderAndGroupedObsConfig();
        Map<String, Integer> tableToTemplateMap = DbImportUtil.getTemplateDatasetMap();
        String iqcareidtype = getMigrationPrimaryIdentifierType();


        for (Map.Entry<String, String> e : datasetMap.entrySet()) {
            String dataset = e.getKey();
            String grpObsConfigFile = e.getValue();
            SpreadsheetImportTemplate template = null;


                System.out.println("processing " + dataset + " dataset ................");
                template = Context.getService(SpreadsheetImportService.class).getTemplateById(tableToTemplateMap.get(dataset));
            try {
                String successfulProcessMsg = DbImportUtil.importTemplate(template, messages, rollbackTransaction, iqcareidtype, grpObsConfigFile);
                System.out.println("Completed processing " + dataset + " dataset ..............");

            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }
}
