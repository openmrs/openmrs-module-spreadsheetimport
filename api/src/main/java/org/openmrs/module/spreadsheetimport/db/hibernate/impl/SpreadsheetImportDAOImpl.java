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
package org.openmrs.module.spreadsheetimport.db.hibernate.impl;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Order;
import org.hibernate.engine.TypedValue;
import org.openmrs.module.spreadsheetimport.SpreadsheetImportTemplate;
import org.openmrs.module.spreadsheetimport.SpreadsheetImportTemplateColumn;
import org.openmrs.module.spreadsheetimport.SpreadsheetImportTemplateColumnPrespecifiedValue;
import org.openmrs.module.spreadsheetimport.SpreadsheetImportTemplatePrespecifiedValue;
import org.openmrs.module.spreadsheetimport.db.hibernate.SpreadsheetImportDAO;

public class SpreadsheetImportDAOImpl implements SpreadsheetImportDAO {
	
	private SessionFactory sessionFactory;
	
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	@SuppressWarnings("unchecked")
	public List<SpreadsheetImportTemplate> getAllTemplates() {
		Criteria crit = sessionFactory.getCurrentSession().createCriteria(SpreadsheetImportTemplate.class);
		crit.addOrder(Order.asc("name"));
		return (List<SpreadsheetImportTemplate>) crit.list();
	}
	
	public SpreadsheetImportTemplate getTemplateById(Integer id) {
		return (SpreadsheetImportTemplate) sessionFactory.getCurrentSession().get(SpreadsheetImportTemplate.class, id);
	}
	
	public SpreadsheetImportTemplate saveSpreadsheetImportTemplate(SpreadsheetImportTemplate template) {
		sessionFactory.getCurrentSession().saveOrUpdate(template);
		return template;
	}
	
	public void deleteSpreadsheetImportTemplate(SpreadsheetImportTemplate template) {
		sessionFactory.getCurrentSession().delete(template);
	}
	
	public String getPredfinedValueById(int id) {
		SpreadsheetImportTemplatePrespecifiedValue spreadsheetImportTemplatePrespecifiedValue = (SpreadsheetImportTemplatePrespecifiedValue) sessionFactory.getCurrentSession().load(SpreadsheetImportTemplatePrespecifiedValue.class, id);
		return spreadsheetImportTemplatePrespecifiedValue != null ? spreadsheetImportTemplatePrespecifiedValue.getValue() : "";
	}
	
	
}
