package org.openmrs.module.spreadsheetimport;

import java.util.List;

/**
 * Defines a template for grouped observations in datasets
 */
public interface DatasetGroupedObservations {

    public Integer getTemplateID();

    public List<GroupedObservations> getColumnDefinitions ();
}
