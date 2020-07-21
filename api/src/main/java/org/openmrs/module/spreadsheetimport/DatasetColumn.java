package org.openmrs.module.spreadsheetimport;

/**
 * A holder for dataset column, concept question, and concept question type
 */
public class DatasetColumn {
    private Integer questionConceptId;
    private String questionConceptDatatype;
    private String value;

    public DatasetColumn(Integer questionConceptId, String questionConceptDatatype) {
        this.questionConceptId = questionConceptId;
        this.questionConceptDatatype = questionConceptDatatype;
    }

    public Integer getQuestionConceptId() {
        return questionConceptId;
    }

    public void setQuestionConceptId(Integer questionConceptId) {
        this.questionConceptId = questionConceptId;
    }

    public String getQuestionConceptDatatype() {
        return questionConceptDatatype;
    }

    public void setQuestionConceptDatatype(String questionConceptDatatype) {
        this.questionConceptDatatype = questionConceptDatatype;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
