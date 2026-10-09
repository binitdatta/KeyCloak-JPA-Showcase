package com.rollingstone.kc.fbl;

/**
 * Shared constants for the two authenticators in this module. In a real
 * deployment the validate URL should come from Config.Scope (SPI config in
 * standalone.xml / keycloak.conf) rather than being hardcoded — left as a
 * constant here to keep the training example short.
 */
public final class rollingstoneFblConstants {

    private rollingstoneFblConstants() {
    }

    public static final String VALIDATE_URL = "http://localhost:8091/api/auth/validate";

    /** Auth-session note keys written by ExtendedCreateUserIfUniqueAuthenticator
     *  and read by PostLoginRoleGroupAuthenticator. */
    public static final String NOTE_EXTERNAL_ROLES = "rollingstone.externalRoles";
    public static final String NOTE_EXTERNAL_GROUPS = "rollingstone.externalGroups";
    public static final String NOTE_EXTERNAL_CUSTOMER_ID = "rollingstone.externalCustomerId";
}
