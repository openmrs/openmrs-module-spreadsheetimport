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

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

/**
 * This controller handles processing of db datasets
 */
@Controller
@RequestMapping("/module/spreadsheetimport/spreadsheetimportprocessdatasets.form")
public class SpreadsheetImportProcessDataFormController {

    @RequestMapping(value = "/module/spreadsheetimport/spreadsheetimportprocessdatasets.form", method = RequestMethod.GET)
    public String setupForm(ModelMap model,
                            final HttpServletRequest request) {
        return "/module/spreadsheetimport/spreadsheetimportProcessDataForm";
    }

}
