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
package org.openmrs.module.spreadsheetimport.validators;

import org.openmrs.module.spreadsheetimport.SpreadsheetImportTemplate;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public class SpreadsheetImportTemplateValidator implements Validator {
	
	public boolean supports(Class clazz) {
		return SpreadsheetImportTemplate.class.equals(clazz);
	}
	
	public void validate(Object obj, Errors errors) {
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "description", "");
		
		SpreadsheetImportTemplate template = (SpreadsheetImportTemplate) obj;

		for (int i = 0; i < template.getColumns().size(); i++) {
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "columns["+i+"].name", "");
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "columns["+i+"].tableDotColumn", "");
		}

//		for (int i = 0; i < template.getPrespecifiedValues().size(); i++) {
//			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "prespecifiedValues["+i+"].value", "");
//		}	
	}
}
