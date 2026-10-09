package com.rollingstone.kc.fbl;


import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bound to the Entra IdP's First Login Flow (havi-entra-first-broker-login).
 * Runs exactly once, the first time a given Entra identity links to a
 * Keycloak account. Reads the SAML-attribute-importer-mapped claims off the
 * BrokeredIdentityContext, upserts the local idp_user row, and grants the
 * initial roles/groups carried in the "roles" App Role claim (values
 * prefixed GROUP_ are treated as group membership, everything else as a
 * realm role).
 */
public class ExtendedEntraCreateUserAuthenticator extends AbstractIdpAuthenticator {

    public static final String PROVIDER_ID = "havi-entra-create-user";

    private static final Logger LOG = Logger.getLogger(ExtendedEntraCreateUserAuthenticator.class.getName());

    private final EntraUserSyncDao dao = new EntraUserSyncDao();

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context,
                                    SerializedBrokeredIdentityContext serializedCtx,
                                    BrokeredIdentityContext brokerContext) {

        context.getAuthenticationSession().setAuthNote("entra.processed", "false");
        // Nothing to persist yet — the Keycloak user doesn't exist until the
        // remaining steps in this flow (Create User If Unique) run. Defer.
        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        authenticate(context);
    }

    @Override
    protected void actionImpl(AuthenticationFlowContext context,
                              SerializedBrokeredIdentityContext serializedCtx,
                              BrokeredIdentityContext brokerContext) {

        UserModel user = context.getUser();
        RealmModel realm = context.getRealm();

        if (user == null) {
            // User not created yet at this point in the flow — nothing to do.
            context.success();
            return;
        }

        try {
            long idpUserId = persistProfile(brokerContext, user);
            applyInitialRolesAndGroups(realm, user, brokerContext, idpUserId);
            context.success();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "havi-entra-create-user: DB sync failed", e);
            context.getEvent().error("havi_entra_db_sync_failed");
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
        }
    }

    private long persistProfile(BrokeredIdentityContext brokerContext, UserModel user) throws SQLException {
        String entraObjectId = brokerContext.getId();
        String email = brokerContext.getEmail();
        String firstName = attr(brokerContext, EntraFblConstants.ATTR_FIRST_NAME);
        String lastName = attr(brokerContext, EntraFblConstants.ATTR_LAST_NAME);
        String phone = attr(brokerContext, EntraFblConstants.ATTR_PHONE);
        String street = attr(brokerContext, EntraFblConstants.ATTR_STREET);
        String city = attr(brokerContext, EntraFblConstants.ATTR_CITY);
        String state = attr(brokerContext, EntraFblConstants.ATTR_STATE);
        String postalCode = attr(brokerContext, EntraFblConstants.ATTR_POSTAL_CODE);
        String country = attr(brokerContext, EntraFblConstants.ATTR_COUNTRY);
        LocalDate dob = parseDob(attr(brokerContext, EntraFblConstants.ATTR_DOB));

        return dao.upsertUser(user.getId(), entraObjectId, email, firstName, lastName, dob,
                phone, street, city, state, postalCode, country);
    }

    private void applyInitialRolesAndGroups(RealmModel realm, UserModel user,
                                            BrokeredIdentityContext brokerContext, long idpUserId) throws SQLException {
        List<String> claimValues = attrList(brokerContext, EntraFblConstants.ATTR_ROLES);

        for (String value : claimValues) {
            if (value.startsWith(EntraFblConstants.GROUP_ROLE_PREFIX)) {
                String groupName = value.substring(EntraFblConstants.GROUP_ROLE_PREFIX.length());
                RoleGroupSync.joinGroupInKeycloakAndDb(realm, user, dao, idpUserId, groupName, LOG);
            } else {
                RoleGroupSync.grantRoleInKeycloakAndDb(realm, user, dao, idpUserId, value, LOG);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String attr(BrokeredIdentityContext ctx, String name) {
        Object value = ctx.getContextData().get(name);
        if (value instanceof List<?> list && !list.isEmpty()) {
            return String.valueOf(list.get(0));
        }
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private List<String> attrList(BrokeredIdentityContext ctx, String name) {
        Object value = ctx.getContextData().get(name);
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return value == null ? List.of() : List.of(String.valueOf(value));
    }

    private LocalDate parseDob(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            LOG.log(Level.WARNING, "havi-entra-create-user: unparseable dateOfBirth ''{0}''", raw);
            return null;
        }
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }
}
