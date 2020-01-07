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
package org.openmrs.module.spreadsheetimport.web.controller;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.spreadsheetimport.DbImportUtil;
import org.openmrs.module.spreadsheetimport.SpreadsheetImportTemplate;
import org.openmrs.module.spreadsheetimport.service.SpreadsheetImportService;
import org.openmrs.web.WebConstants;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
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
	
	@RequestMapping(value = "/module/spreadsheetimport/spreadsheetimportprocessdatasets.form", method = RequestMethod.GET)
	public String setupForm(ModelMap model,
	                        final HttpServletRequest request) {
		return "/module/spreadsheetimport/spreadsheetimportProcessDataForm";
	}
	
	@RequestMapping(value = "/module/spreadsheetimport/spreadsheetimportprocessdatasets.form", method = RequestMethod.POST)
	public String processSubmit(ModelMap model,
	                            HttpServletRequest request,
	                            HttpServletResponse response) throws Exception {
		

		List<String> messages = new ArrayList<String>();
		boolean rollbackTransaction = false;

		String iqcareidtype = getMigrationPrimaryIdentifierType();


		Map<String, Integer> tableToTemplateMap = new HashMap<String, Integer>();
		tableToTemplateMap.put("tr_hiv_enrollment", 8);
		tableToTemplateMap.put("tr_hiv_program_enrollment", 9);
		tableToTemplateMap.put("tr_triage", 11);
		tableToTemplateMap.put("tr_hts_initial", 12);
		tableToTemplateMap.put("tr_hts_retest", 15);
		tableToTemplateMap.put("tr_hiv_regimen_history", 23);
		tableToTemplateMap.put("tr_hiv_followup", 22);




		/**
		 *  attempt nested processing of data
		 * 	the order should be demographics,
		 * 	hiv enrollment encounter,
		 * 	triage encounter,
		 * 	hiv testing initial encounter
		 * 	hiv patient program
		 */

		// custom logic to process patient demographics. the spreadsheet module coe
		// skips processing of other identifiers and other person related data once
		// an associated person is found.
		String successfulProcessMsg = "proceed";
		SpreadsheetImportTemplate template = null;

		successfulProcessMsg = DbImportUtil.processDemographicsDataset(messages);

		System.out.println("Completed processing demographics ");


		if (successfulProcessMsg != null) {
			System.out.println("processing HIV Enrollments ");

			// step 2: process hiv enrollment encounter
			template = Context.getService(SpreadsheetImportService.class).getTemplateById(tableToTemplateMap.get("tr_hiv_enrollment"));
			successfulProcessMsg = DbImportUtil.importTemplate(template, messages, rollbackTransaction, iqcareidtype);
			System.out.println("Completed processing HIV enrollments ");


		}

		/*if (successfulProcessMsg != null) {
			// step 3: process hiv program history
			template = Context.getService(SpreadsheetImportService.class).getTemplateById(tableToTemplateMap.get("tr_hiv_program_enrollment"));
			successfulProcessMsg = DbImportUtil.importTemplate(template, messages, rollbackTransaction);

		}
*/
		/*if (successfulProcessMsg != null) {
			// step 3: process hiv art history
			template = Context.getService(SpreadsheetImportService.class).getTemplateById(tableToTemplateMap.get("tr_hiv_regimen_history"));
			successfulProcessMsg = DbImportUtil.importTemplate(template, messages, rollbackTransaction);

		}*/

		/*if (successfulProcessMsg != null) {
			// step 4: process HTS
			template = Context.getService(SpreadsheetImportService.class).getTemplateById(tableToTemplateMap.get("tr_hts_initial"));
			successfulProcessMsg = DbImportUtil.importTemplate(template, messages, rollbackTransaction);

		}

		if (successfulProcessMsg != null) {
			// step 4: process HTS
			template = Context.getService(SpreadsheetImportService.class).getTemplateById(tableToTemplateMap.get("tr_hts_retest"));
			successfulProcessMsg = DbImportUtil.importTemplate(template, messages, rollbackTransaction);

		}*/

		if (successfulProcessMsg != null) {
			// step 4: process triage
			template = Context.getService(SpreadsheetImportService.class).getTemplateById(tableToTemplateMap.get("tr_triage"));
			successfulProcessMsg = DbImportUtil.importTemplate(template, messages, rollbackTransaction, iqcareidtype);
		}

		/*if (successfulProcessMsg != null) {
			// step 4: process triage
			template = Context.getService(SpreadsheetImportService.class).getTemplateById(tableToTemplateMap.get("tr_hiv_followup"));
			successfulProcessMsg = DbImportUtil.importTemplate(template, messages, rollbackTransaction);
		}*/

		boolean succeeded = (successfulProcessMsg != null);

		String messageString = "";
		for (int i = 0; i < messages.size(); i++) {
			if (i != 0) {
				messageString += "<br />";
			}
			messageString += messages.get(i);
		}
		if (succeeded) {
			messageString += "<br />Success!";
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

		MysqlDataSource dataSource = null;
		Connection conn = null;

		String IQCARE_PERSON_PK_ID_TYPE = "b3d6de9f-f215-4259-9805-8638c887e46b";
		String mainPtIdType = null;
		String mainIdQry = "select patient_identifier_type_id from patient_identifier_type where uuid='" + IQCARE_PERSON_PK_ID_TYPE + "'";

		try {
			Properties p = Context.getRuntimeProperties();
			String url = p.getProperty("connection.url");
			dataSource = new MysqlDataSource();
			dataSource.setURL(url);
			dataSource.setUser(p.getProperty("connection.username"));
			dataSource.setPassword(p.getProperty("connection.password"));
			conn = dataSource.getConnection();
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
		
}
