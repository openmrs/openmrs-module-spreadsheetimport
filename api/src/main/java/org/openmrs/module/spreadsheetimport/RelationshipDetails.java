package org.openmrs.module.spreadsheetimport;

import org.openmrs.api.context.Context;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class RelationshipDetails {

    public static RelationshipDetails relationshipDetails = null;
    public static String PARENT_CHILD_TYPE_UUID = "8d91a210-c2cc-11de-8d13-0010c6dffd0f";
    public static String SIBLING_TYPE_UUID = "8d91a01c-c2cc-11de-8d13-0010c6dffd0f";
    public static String SPOUSE_TYPE = "d6895098-5d8d-11e3-94ee-b35a4132a5e3";
    public static String SEXUAL_PARTNER_TYPE = "007b765f-6725-4ae9-afee-9966302bace4";

    private Integer parentChildRelationshipTypeId;
    private Integer siblingRelationshipTypeId;
    private Integer spouseRelationshipTypeId;
    private Integer sexualRelationshipTypeId;

    public RelationshipDetails(Integer parentChildRelationshipTypeId, Integer siblingRelationshipTypeId, Integer spouseRelationshipTypeId, Integer sexualRelationshipTypeId) {
        this.parentChildRelationshipTypeId = parentChildRelationshipTypeId;
        this.siblingRelationshipTypeId = siblingRelationshipTypeId;
        this.spouseRelationshipTypeId = spouseRelationshipTypeId;
        this.sexualRelationshipTypeId = sexualRelationshipTypeId;
    }

    public Integer getParentChildRelationshipTypeId() {
        return parentChildRelationshipTypeId;
    }

    public void setParentChildRelationshipTypeId(Integer parentChildRelationshipTypeId) {
        this.parentChildRelationshipTypeId = parentChildRelationshipTypeId;
    }

    public Integer getSiblingRelationshipTypeId() {
        return siblingRelationshipTypeId;
    }

    public void setSiblingRelationshipTypeId(Integer siblingRelationshipTypeId) {
        this.siblingRelationshipTypeId = siblingRelationshipTypeId;
    }

    public Integer getSpouseRelationshipTypeId() {
        return spouseRelationshipTypeId;
    }

    public void setSpouseRelationshipTypeId(Integer spouseRelationshipTypeId) {
        this.spouseRelationshipTypeId = spouseRelationshipTypeId;
    }

    public Integer getSexualRelationshipTypeId() {
        return sexualRelationshipTypeId;
    }

    public void setSexualRelationshipTypeId(Integer sexualRelationshipTypeId) {
        this.sexualRelationshipTypeId = sexualRelationshipTypeId;
    }

    public static void setRelationshipDetails() {

        Connection conn = null;
        Statement s = null;

        Integer parentChildId = null;
        Integer siblingId = null;
        Integer spouseId = null;
        Integer sexualId = null;



        try {

            // Connect to db
            Class.forName("com.mysql.jdbc.Driver").newInstance();

            Properties p = Context.getRuntimeProperties();
            String url = p.getProperty("connection.url");

            conn = DriverManager.getConnection(url, p.getProperty("connection.username"),
                    p.getProperty("connection.password"));
            conn.setAutoCommit(false);

            // set parent-child relationship
            String qryParentChild = "select relationship_type_id from relationship_type where uuid='" + RelationshipDetails.PARENT_CHILD_TYPE_UUID + "'";
            ResultSet rsParentChild = null;
            try {
                Statement sParentChild = conn.createStatement();
                rsParentChild = sParentChild.executeQuery(qryParentChild);
                if (rsParentChild.next()) {
                    parentChildId = rsParentChild.getInt(1);
                    rsParentChild.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // set parent-child relationship
            String qrySibling = "select relationship_type_id from relationship_type where uuid='" + RelationshipDetails.SIBLING_TYPE_UUID + "'";
            ResultSet rsSibling = null;
            try {
                Statement sSibling = conn.createStatement();
                rsSibling = sSibling.executeQuery(qrySibling);
                if (rsSibling.next()) {
                    siblingId = rsSibling.getInt(1);
                    rsSibling.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // set parent-child relationship
            String qrySpouse = "select relationship_type_id from relationship_type where uuid='" + RelationshipDetails.SPOUSE_TYPE + "'";
            ResultSet rsSpouse = null;
            try {
                Statement sSpouse = conn.createStatement();
                rsSpouse = sSpouse.executeQuery(qrySpouse);
                if (rsSpouse.next()) {
                    spouseId = rsSpouse.getInt(1);
                    rsSpouse.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // set parent-child relationship
            String qryPartner = "select relationship_type_id from relationship_type where uuid='" + RelationshipDetails.SEXUAL_PARTNER_TYPE + "'";
            ResultSet rsPartner = null;
            try {
                Statement sPartner = conn.createStatement();
                rsPartner = sPartner.executeQuery(qryPartner);
                if (rsPartner.next()) {
                    sexualId = rsPartner.getInt(1);
                    rsPartner.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            RelationshipDetails.relationshipDetails = new RelationshipDetails(parentChildId, siblingId, spouseId, sexualId);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
