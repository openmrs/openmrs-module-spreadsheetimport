package org.openmrs.module.spreadsheetimport;

import java.util.HashMap;
import java.util.Map;

/**
 * Holder for grouped observations.
 * It is used to hold details of dataset columns that
 * should be processed into grouped observations
 */
public class GroupedObservations {
    private Integer groupConceptId;
    private String datasetName;// if data is from a different dataset
    private Map<String, DatasetColumn> datasetColumns = new HashMap<String, DatasetColumn>(); // key is column name, value is DatasetColumn object
    private Boolean hasData;

    public GroupedObservations() {
    }

    public GroupedObservations(Integer groupConceptId, Map<String, DatasetColumn> datasetColumns) {
        this.groupConceptId = groupConceptId;
        this.datasetColumns = datasetColumns;
    }

    public Integer getGroupConceptId() {
        return groupConceptId;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public void setGroupConceptId(Integer groupConceptId) {
        this.groupConceptId = groupConceptId;
    }

    public Map<String, DatasetColumn> getDatasetColumns() {
        return datasetColumns;
    }

    public void setDatasetColumns(Map<String, DatasetColumn> columnDetails) {
        this.datasetColumns = columnDetails;
    }

    public void addColumn(String key, DatasetColumn column) {
        if (!datasetColumns.containsKey(key)) {
            datasetColumns.put(key, column);
        }
    }

    public Boolean getHasData() {
        return hasData;
    }

    public void setHasData(Boolean hasData) {
        this.hasData = hasData;
    }
}
