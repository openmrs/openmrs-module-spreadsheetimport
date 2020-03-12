package org.openmrs.module.spreadsheetimport;

import org.openmrs.api.context.Context;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class LabOrderDetails {

    public static LabOrderDetails labOrderDetails = null;
    public static String ENCOUNTER_TYPE_UUID = "e1406e88-e9a9-11e8-9f32-f2801f1b9fd1";
    public static String ORDER_TYPE_UUID = "52a447d3-a64a-11e3-9aeb-50e549534c5e";
    public static String CARE_SETTING_UUID = "6f0c9a92-6f24-11e3-af88-005056821db0";
    public static final Integer HIV_VIRAL_LOAD = 856;
    public static final Integer HIV_VIRAL_LOAD_QUALITATIVE = 1305;
    public static final Integer NOT_DETECTED = 1302;
    public static final Integer CD4_COUNT = 5497;
    public static final Integer CD4_PERCENT = 730;

    private Integer encounterTypeId;
    private Integer orderTypeId;
    private Integer careSettingId;

    public LabOrderDetails(Integer encounterTypeId, Integer orderTypeId, Integer careSettingId) {
        this.encounterTypeId = encounterTypeId;
        this.orderTypeId = orderTypeId;
        this.careSettingId = careSettingId;
    }

    public Integer getEncounterTypeId() {
        return encounterTypeId;
    }

    public void setEncounterTypeId(Integer encounterTypeId) {
        this.encounterTypeId = encounterTypeId;
    }

    public Integer getOrderTypeId() {
        return orderTypeId;
    }

    public void setOrderTypeId(Integer orderTypeId) {
        this.orderTypeId = orderTypeId;
    }

    public Integer getCareSettingId() {
        return careSettingId;
    }

    public void setCareSettingId(Integer careSettingId) {
        this.careSettingId = careSettingId;
    }

    public static void setLabOrderDetails() {

        Connection conn = null;
        Statement s = null;
        Integer careSettingId = null;
        Integer orderTypeId = null;
        Integer encounterTypeId = null;



        try {

            // Connect to db
            Class.forName("com.mysql.jdbc.Driver").newInstance();

            Properties p = Context.getRuntimeProperties();
            String url = p.getProperty("connection.url");

            conn = DriverManager.getConnection(url, p.getProperty("connection.username"),
                    p.getProperty("connection.password"));
            conn.setAutoCommit(false);

            // set care setting
            String qryCareSetting = "select care_setting_id from care_setting where uuid='" + LabOrderDetails.CARE_SETTING_UUID + "'";
            ResultSet rsCareSetting = null;
            try {
                Statement sCareSetting = conn.createStatement();
                rsCareSetting = sCareSetting.executeQuery(qryCareSetting);
                if (rsCareSetting.next()) {
                    careSettingId = rsCareSetting.getInt(1);
                    rsCareSetting.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // set order type
            String qryOrderType = "select order_type_id from order_type where uuid='" + LabOrderDetails.ORDER_TYPE_UUID + "'";
            ResultSet rsOrderType = null;
            try {
                Statement sOrderType = conn.createStatement();
                rsOrderType = sOrderType.executeQuery(qryOrderType);
                if (rsOrderType.next()) {
                    orderTypeId = rsOrderType.getInt(1);
                    rsOrderType.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // set encounter type
            String qryEncType = "select encounter_type_id from encounter_type where uuid='" + LabOrderDetails.ENCOUNTER_TYPE_UUID + "'";
            ResultSet rsEncounterType = null;
            try {
                Statement sEncounterType = conn.createStatement();
                rsEncounterType = sEncounterType.executeQuery(qryEncType);
                if (rsEncounterType.next()) {
                    encounterTypeId = rsEncounterType.getInt(1);
                    rsEncounterType.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            LabOrderDetails.labOrderDetails = new LabOrderDetails(encounterTypeId, orderTypeId, careSettingId);

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
