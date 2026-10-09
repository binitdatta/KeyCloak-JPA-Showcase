package com.rollingstone.kc.fbl;

/**
 * Shared constants for the Entra ID broker-sync authenticators. DB
 * credentials are hardcoded here to keep this training example short — in a
 * real deployment, source these from Config.Scope (SPI config in
 * keycloak.conf) instead.
 */
public final class EntraFblConstants {

    private EntraFblConstants() {
    }

    public static final String JDBC_URL = "jdbc:mysql://localhost:3306/keycloak_jpa_demo?useSSL=false&serverTimezone=UTC";
    public static final String JDBC_USER = "root";
    public static final String JDBC_PASSWORD = "changeit";

    /** SAML attribute names, as configured on the Keycloak IdP's Attribute Importer mappers. */
    public static final String ATTR_FIRST_NAME = "firstName";
    public static final String ATTR_LAST_NAME = "lastName";
    public static final String ATTR_PHONE = "phoneNumber";
    public static final String ATTR_STREET = "streetAddress";
    public static final String ATTR_CITY = "city";
    public static final String ATTR_STATE = "state";
    public static final String ATTR_POSTAL_CODE = "postalCode";
    public static final String ATTR_COUNTRY = "country";
    public static final String ATTR_DOB = "dateOfBirth";
    public static final String ATTR_ROLES = "entraRoles";

    /** App Role values carrying this prefix are treated as group membership, not a role. */
    public static final String GROUP_ROLE_PREFIX = "GROUP_";
}