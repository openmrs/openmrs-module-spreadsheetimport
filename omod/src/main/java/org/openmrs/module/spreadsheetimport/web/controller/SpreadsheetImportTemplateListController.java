package org.openmrs.module.spreadsheetimport.web.controller;

import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.spreadsheetimport.SpreadsheetImportTemplate;
import org.openmrs.module.spreadsheetimport.service.SpreadsheetImportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/module/spreadsheetimport/spreadsheetimport.list")
public class SpreadsheetImportTemplateListController<E> {
	
	/**
	 * Logger for this class
	 */
	protected final Log log = LogFactory.getLog(getClass());
	
	public SpreadsheetImportTemplateListController() {
	}
	
	@ModelAttribute("templates")
	List<SpreadsheetImportTemplate> populateTemplates() {
		return Context.getService(SpreadsheetImportService.class).getAllTemplates();
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public String listTemplates() {
		return "/module/spreadsheetimport/spreadsheetimportTemplateList";
	}
	
	@RequestMapping(method = RequestMethod.POST)
	public String delete(@ModelAttribute("templates") List<SpreadsheetImportTemplate> templates,
	                     HttpServletRequest request) throws ServletRequestBindingException {
		SpreadsheetImportService svc = Context.getService(SpreadsheetImportService.class);
		
		// Must use iterator to update module attribute
		Iterator<SpreadsheetImportTemplate> iterator = templates.iterator();
		while (iterator.hasNext()) {
			SpreadsheetImportTemplate template = iterator.next();
			if (request.getParameter(template.getId().toString()) != null) {
				svc.deleteSpreadsheetImportTemplate(template);
				iterator.remove();
			}
		}
		
		return "/module/spreadsheetimport/spreadsheetimportTemplateList";
	}
	
}
