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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

/**
 * This controller backs and saves the Spreadsheet Import module settings
 */
@Controller
@RequestMapping("/module/spreadsheetimport/spreadsheetimportImport.form")
@SessionAttributes({"template", "spreadsheetFile"})
public class SpreadsheetImportImportFormController {
	
	/**
	 * Logger for this class
	 */
	protected final Log log = LogFactory.getLog(getClass());
	
	@RequestMapping(value = "/module/spreadsheetimport/spreadsheetimportImport.form", method = RequestMethod.GET)
	public String setupForm(@RequestParam(value = "id", required = true) Integer id, 
							ModelMap model,
	                        final HttpServletRequest request) {
		SpreadsheetImportTemplate template = null;
		template = Context.getService(SpreadsheetImportService.class).getTemplateById(id);
		model.addAttribute("template", template);
		return "/module/spreadsheetimport/spreadsheetimportImportForm";
	}
	
	@RequestMapping(value = "/module/spreadsheetimport/spreadsheetimportImport.form", method = RequestMethod.POST)
	public String processSubmit(@ModelAttribute("template") SpreadsheetImportTemplate template, 
								ModelMap model, 
	                            @RequestParam(value = "file", required = true) MultipartFile file,
	                            @RequestParam(value = "sheet", required = true) String sheet,
	                            HttpServletRequest request,
	                            HttpServletResponse response) throws Exception {
		
		
//		Map<UniqueImport, Set<SpreadsheetImportTemplateColumn>> rowDataTemp = template.getMapOfUniqueImportToColumnSetSortedByImportIdx();
//
//		for (UniqueImport uniqueImport : rowDataTemp.keySet()) {
//			Set<SpreadsheetImportTemplateColumn> columnSet = rowDataTemp.get(uniqueImport);
//			boolean isFirst = true;
//			for (SpreadsheetImportTemplateColumn column : columnSet) {
//
//				if (isFirst) {
//					// Should be same for all columns in unique import
//					System.out.println("SpreadsheetImportUtil.importTemplate: column.getColumnPrespecifiedValues(): " + column.getColumnPrespecifiedValues().size());
//					if (column.getColumnPrespecifiedValues().size() > 0) {
//						Set<SpreadsheetImportTemplateColumnPrespecifiedValue> columnPrespecifiedValueSet = column.getColumnPrespecifiedValues();
//						for (SpreadsheetImportTemplateColumnPrespecifiedValue columnPrespecifiedValue : columnPrespecifiedValueSet) {
//							System.out.println(columnPrespecifiedValue.getPrespecifiedValue().getValue());
//						}
//					}
//				}
//			}
//		}

		
		
		List<String> messages = new ArrayList<String>();
		boolean rollbackTransaction = true;
		if (request.getParameter("rollbackTransaction") == null) {
			rollbackTransaction = false;
		}

		Map<String, Integer> tableToTemplateMap = new HashMap<String, Integer>();
		tableToTemplateMap.put("tr_hiv_enrollment", 8);

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
		String successfulProcessMsg = null;

		successfulProcessMsg = DbImportUtil.processDemographicsDataset(messages);

		if (successfulProcessMsg != null) {
			// step 2: process hiv enrollment encounter
			template = Context.getService(SpreadsheetImportService.class).getTemplateById(tableToTemplateMap.get("tr_hiv_enrollment"));
			successfulProcessMsg = DbImportUtil.importTemplate(template, file, sheet, messages, rollbackTransaction);

		}


		// step 1: set template to demographics
		//template = Context.getService(SpreadsheetImportService.class).getTemplateById(tableToTemplateMap.get("demographics"));
		//successfulProcessMsg = DbImportUtil.importTemplate(template, file, sheet, messages, rollbackTransaction);

		/*if (successfulProcessMsg != null) {
			// step 2: process hiv enrollment encounter
			template = Context.getService(SpreadsheetImportService.class).getTemplateById(tableToTemplateMap.get("hiv_enrollment_encounter"));
			successfulProcessMsg = DbImportUtil.importTemplate(template, file, sheet, messages, rollbackTransaction);

			*//*if (successfulProcessMsg != null) {
				// step 3: process triage encounter
				template = Context.getService(SpreadsheetImportService.class).getTemplateById(tableToTemplateMap.get("triage_encounter"));
				successfulProcessMsg = DbImportUtil.importTemplate(template, file, sheet, messages, rollbackTransaction);

				if (successfulProcessMsg != null) {
					// step 4: process hts initial encounter
					template = Context.getService(SpreadsheetImportService.class).getTemplateById(tableToTemplateMap.get("hiv_testing_initial_encounter"));
					successfulProcessMsg = DbImportUtil.importTemplate(template, file, sheet, messages, rollbackTransaction);

					if (successfulProcessMsg != null) {
						// step 5: process hiv patient program
						template = Context.getService(SpreadsheetImportService.class).getTemplateById(tableToTemplateMap.get("hiv_patient_program"));
						successfulProcessMsg = DbImportUtil.importTemplate(template, file, sheet, messages, rollbackTransaction);
						if (successfulProcessMsg != null) {

						} else {
							messages.add("Error processing patient program");
						}

					} else {
						messages.add("Error processing HTS initial encounter");
					}

				} else {
					messages.add("Error processing triage encounters");

				}

			} else {
				messages.add("Error processing HIV enrollment encounters");

			}*//*

		} else {
			messages.add("Error processing patient demographics");

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
		
		return "/module/spreadsheetimport/spreadsheetimportImportForm";
	}
		
}
