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
package org.openmrs.module.spreadsheetimport.service.impl;

import java.util.Date;
import java.util.List;

import org.hibernate.SessionFactory;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.spreadsheetimport.SpreadsheetImportTemplate;
import org.openmrs.module.spreadsheetimport.db.hibernate.SpreadsheetImportDAO;
import org.openmrs.module.spreadsheetimport.service.SpreadsheetImportService;

public class SpreadsheetImportServiceImpl extends BaseOpenmrsService implements SpreadsheetImportService {
	
	SpreadsheetImportDAO dao;
	
	public SessionFactory getSessionFactory() {
		return dao.getSessionFactory();
	}
	
	public void setDao(final SpreadsheetImportDAO dao) {
		this.dao = dao;
	}
	
	public List<SpreadsheetImportTemplate> getAllTemplates() {
		return dao.getAllTemplates();
	}
	
	public SpreadsheetImportTemplate getTemplateById(final Integer id) {
		return dao.getTemplateById(id);
	}
	
	public void deleteSpreadsheetImportTemplate(final SpreadsheetImportTemplate template) {
		dao.deleteSpreadsheetImportTemplate(template);
	}
	
	public SpreadsheetImportTemplate saveSpreadsheetImportTemplate(final SpreadsheetImportTemplate template) {
		// let's prevent a lovely NPE from occuring. 
		if (template == null)
			throw new IllegalArgumentException("template cannot be null");
		final Date now = new Date();
		if (template.getCreated() == null)
			template.setCreated(now);
		final User authenticatedUser = Context.getAuthenticatedUser();
		if (template.getCreator() == null)
			template.setCreator(authenticatedUser);
		if (template.getId() == null) {
			template.setModified(now);
			final User modifiedBy = Context.getAuthenticatedUser();
			template.setModifiedBy(modifiedBy);
		}
		dao.saveSpreadsheetImportTemplate(template);
		return template;
	}
	
	
	public String getPredfinedValueById(int id) {
		return dao.getPredfinedValueById(id);
	}
}
