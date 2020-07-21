package org.openmrs.module.spreadsheetimport;

import org.openmrs.api.context.Context;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class PatientRegistrationMetadata {

    public static PatientRegistrationMetadata registrationMetadata = null;
    public static final String NEXT_OF_KIN_ADDRESS = "7cf22bec-d90a-46ad-9f48-035952261294";
    public static final String NEXT_OF_KIN_CONTACT = "342a1d39-c541-4b29-8818-930916f4c2dc";
    public static final String ALTERNATE_PHONE_CONTACT = "94614350-84c8-41e0-ac29-86bc107069be";
    public static final String NEXT_OF_KIN_NAME = "830bef6d-b01f-449d-9f8d-ac0fede8dbd3";
    public static final String NEXT_OF_KIN_RELATIONSHIP = "d0aa9fd1-2ac5-45d8-9c5e-4317c622c8f5";
    public static final String TELEPHONE_CONTACT = "b2c38640-2603-4629-aebd-3b54f33f1e3a";
    public static final String GUARDIAN_FIRST_NAME = "8caf6d06-9070-49a5-b715-98b45e5d427b";
    public static final String GUARDIAN_LAST_NAME = "0803abbd-2be4-4091-80b3-80c6940303df";

    public static final Integer MARITAL_STATUS_CONCEPT = 1054;
    public static final Integer OCCUPATION_CONCEPT = 1542;
    public static final Integer EDUCATION_LEVEL_CONCEPT = 1712;

    private Integer nextOfKinAddressId;
    private Integer nextOfKinNameId;
    private Integer nextOfKinContactId;
    private Integer nextOfKinRelationshipId;
    private Integer telephoneContactId;
    private Integer guardianFirstNameId;
    private Integer guardianLastNameId;
    private Integer alternatePhoneContactId;

    public PatientRegistrationMetadata(Integer nextOfKinNameId, Integer nextOfKinContactId, Integer nextOfKinAddressId, Integer nextOfKinRelationshipId, Integer telephoneContactId, Integer guardianFirstNameId, Integer guardianLastNameId, Integer alternatePhoneContactId) {
        this.nextOfKinNameId = nextOfKinNameId;
        this.nextOfKinAddressId = nextOfKinAddressId;
        this.nextOfKinContactId = nextOfKinContactId;
        this.nextOfKinRelationshipId = nextOfKinRelationshipId;
        this.telephoneContactId = telephoneContactId;
        this.guardianFirstNameId = guardianFirstNameId;
        this.guardianLastNameId = guardianLastNameId;
        this.alternatePhoneContactId = alternatePhoneContactId;
    }

    public Integer getNextOfKinAddressId() {
        return nextOfKinAddressId;
    }

    public void setNextOfKinAddressId(Integer nextOfKinAddressId) {
        this.nextOfKinAddressId = nextOfKinAddressId;
    }

    public Integer getNextOfKinNameId() {
        return nextOfKinNameId;
    }

    public void setNextOfKinNameId(Integer nextOfKinNameId) {
        this.nextOfKinNameId = nextOfKinNameId;
    }

    public Integer getNextOfKinContactId() {
        return nextOfKinContactId;
    }

    public void setNextOfKinContactId(Integer nextOfKinContactId) {
        this.nextOfKinContactId = nextOfKinContactId;
    }

    public Integer getNextOfKinRelationshipId() {
        return nextOfKinRelationshipId;
    }

    public void setNextOfKinRelationshipId(Integer nextOfKinRelationshipId) {
        this.nextOfKinRelationshipId = nextOfKinRelationshipId;
    }

    public Integer getTelephoneContactId() {
        return telephoneContactId;
    }

    public void setTelephoneContactId(Integer telephoneContactId) {
        this.telephoneContactId = telephoneContactId;
    }

    public Integer getGuardianFirstNameId() {
        return guardianFirstNameId;
    }

    public void setGuardianFirstNameId(Integer guardianFirstNameId) {
        this.guardianFirstNameId = guardianFirstNameId;
    }

    public Integer getGuardianLastNameId() {
        return guardianLastNameId;
    }

    public void setGuardianLastNameId(Integer guardianLastNameId) {
        this.guardianLastNameId = guardianLastNameId;
    }

    public Integer getAlternatePhoneContactId() {
        return alternatePhoneContactId;
    }

    public void setAlternatePhoneContactId(Integer alternatePhoneContactId) {
        this.alternatePhoneContactId = alternatePhoneContactId;
    }

    public static void setRegistrationMetadata() {

        Connection conn = null;
        Statement s = null;
        Integer nokNameId = null;
        Integer nokContactId = null;
        Integer nokAddressId = null;
        Integer nokRelationshipId = null;
        Integer phoneContactId = null;
        Integer alternatePhoneContactId = null;
        Integer gFirstNameId = null;
        Integer gLastNameId = null;

        try {

            // Connect to db
            Class.forName("com.mysql.jdbc.Driver").newInstance();

            Properties p = Context.getRuntimeProperties();
            String url = p.getProperty("connection.url");

            conn = DriverManager.getConnection(url, p.getProperty("connection.username"),
                    p.getProperty("connection.password"));
            conn.setAutoCommit(false);

            // Next of Kin name
            String qryNokName = "select person_attribute_type_id from person_attribute_type where uuid='" + PatientRegistrationMetadata.NEXT_OF_KIN_NAME + "'";
            ResultSet rsNokName = null;
            try {
                Statement sNokName = conn.createStatement();
                rsNokName = sNokName.executeQuery(qryNokName);
                if (rsNokName.next()) {
                    nokNameId = rsNokName.getInt(1);
                    rsNokName.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // set next of kin contact
            String qryNokContact = "select person_attribute_type_id from person_attribute_type where uuid='" + PatientRegistrationMetadata.NEXT_OF_KIN_CONTACT  + "'";
            ResultSet rsNokContact = null;
            try {
                Statement sNokContact = conn.createStatement();
                rsNokContact = sNokContact.executeQuery(qryNokContact);
                if (rsNokContact.next()) {
                    nokContactId = rsNokContact.getInt(1);
                    rsNokContact.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // set next of kin address
            String qryNokAddress = "select person_attribute_type_id from person_attribute_type where uuid='" + PatientRegistrationMetadata.NEXT_OF_KIN_ADDRESS + "'";
            ResultSet rsNokAddress = null;
            try {
                Statement sNokAddress = conn.createStatement();
                rsNokAddress = sNokAddress.executeQuery(qryNokAddress);
                if (rsNokAddress.next()) {
                    nokAddressId = rsNokAddress.getInt(1);
                    rsNokAddress.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // set next of kin relationship
            String qryNokRelationship = "select person_attribute_type_id from person_attribute_type where uuid='" + PatientRegistrationMetadata.NEXT_OF_KIN_RELATIONSHIP + "'";
            ResultSet rsNokRelationship = null;
            try {
                Statement sNokRelationship = conn.createStatement();
                rsNokRelationship = sNokRelationship.executeQuery(qryNokRelationship);
                if (rsNokRelationship.next()) {
                    nokRelationshipId = rsNokRelationship.getInt(1);
                    rsNokRelationship.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // set phone contact
            String qryTelContact = "select person_attribute_type_id from person_attribute_type where uuid='" + PatientRegistrationMetadata.TELEPHONE_CONTACT + "'";
            ResultSet rsTelContact = null;
            try {
                Statement sTelContact = conn.createStatement();
                rsTelContact = sTelContact.executeQuery(qryTelContact);
                if (rsTelContact.next()) {
                    phoneContactId = rsTelContact.getInt(1);
                    rsTelContact.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // set phone contact
            String qryAltTelContact = "select person_attribute_type_id from person_attribute_type where uuid='" + PatientRegistrationMetadata.ALTERNATE_PHONE_CONTACT + "'";
            ResultSet rsAltTelContact = null;
            try {
                Statement sAltTelContact = conn.createStatement();
                rsAltTelContact = sAltTelContact.executeQuery(qryAltTelContact);
                if (rsAltTelContact.next()) {
                    alternatePhoneContactId = rsAltTelContact.getInt(1);
                    rsAltTelContact.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            PatientRegistrationMetadata.registrationMetadata = new PatientRegistrationMetadata(nokNameId, nokContactId, nokAddressId, nokRelationshipId, phoneContactId, null, null, alternatePhoneContactId);

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
