package com.rollingstone.kc.entra;

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
 * Bound to the Entra IdP's First Login Flow (rollingstone-entra-first-broker-login).
 * Runs exactly once, the first time a given Entra identity links to a
 * Keycloak account.
 *
 * IMPORTANT: AbstractIdpAuthenticator#authenticate() calls authenticateImpl()
 * directly. action()/actionImpl() are ONLY invoked later if authenticateImpl()
 * issued a browser challenge (context.challenge(...)) and the user submitted
 * that form. This authenticator never challenges, so all of its real work
 * (user creation, MySQL sync, initial role/group grants) must happen inside
 * authenticateImpl() itself -- putting it in actionImpl() means it never runs.
 * action()/actionImpl() are still implemented below purely because
 * AbstractIdpAuthenticator declares actionImpl() abstract -- they are dead
 * code in this authenticator's normal flow.
 *
 * Since this authenticator now also replaces the built-in "Create User If
 * Unique" step in the User creation or linking sub-flow, it creates the
 * Keycloak UserModel itself when context.getUser() is still null at this
 * point in the flow, instead of assuming some other execution already did it.
 *
 * ATTRIBUTE SOURCE: the Keycloak IdP's "Attribute Importer" mappers write
 * claim values into BrokeredIdentityContext.getContextData(), keyed with a
 * "user.attributes." prefix (e.g. "user.attributes.entraRoles",
 * "user.attributes.firstName") -- confirmed from a runtime dump of
 * getContextData().keySet(). attr()/attrList() below read from
 * brokerContext with that prefix prepended, not from the UserModel.
 */
public class ExtendedEntraCreateUserAuthenticator extends AbstractIdpAuthenticator {

    public static final String PROVIDER_ID = "rollingstone-entra-create-user";

    private static final Logger LOG = Logger.getLogger(ExtendedEntraCreateUserAuthenticator.class.getName());

    private static final String USER_ATTR_PREFIX = "user.attributes.";

    private final EntraUserSyncDao dao = new EntraUserSyncDao();

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context,
                                    SerializedBrokeredIdentityContext serializedCtx,
                                    BrokeredIdentityContext brokerContext) {

        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();

        if (user == null) {
            user = createKeycloakUser(context, brokerContext);
        }

        try {
            long idpUserId = persistProfile(brokerContext, user);
            applyInitialRolesAndGroups(realm, user, brokerContext, idpUserId);
            context.success();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "rollingstone-entra-create-user: DB sync failed", e);
            context.getEvent().error("rollingstone_entra_db_sync_failed");
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        authenticate(context);
    }

    @Override
    protected void actionImpl(AuthenticationFlowContext context,
                              SerializedBrokeredIdentityContext serializedCtx,
                              BrokeredIdentityContext brokerContext) {
        // authenticateImpl() never challenges, so this is never reached in
        // practice -- required override only to satisfy AbstractIdpAuthenticator.
        context.success();
    }

    /**
     * Creates the Keycloak-side UserModel for a brand-new Entra identity.
     * Mirrors the minimum the built-in "Create User If Unique" step did --
     * no duplicate-email detection here, since this is a training lab where
     * every Entra login is expected to be a genuinely new account.
     */
    private UserModel createKeycloakUser(AuthenticationFlowContext context, BrokeredIdentityContext brokerContext) {
        RealmModel realm = context.getRealm();
        KeycloakSession session = context.getSession();

        String username = brokerContext.getModelUsername();
        if (username == null || username.isBlank()) {
            username = brokerContext.getUsername();
        }
        if (username == null || username.isBlank()) {
            username = brokerContext.getEmail();
        }

        UserModel newUser = session.users().addUser(realm, username);
        newUser.setEnabled(true);
        newUser.setEmail(brokerContext.getEmail());
        newUser.setEmailVerified(true);

        context.setUser(newUser);

        LOG.log(Level.INFO, "rollingstone-entra-create-user: created Keycloak user ''{0}'' for Entra object {1}",
                new Object[]{username, brokerContext.getId()});

        return newUser;
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

        LOG.log(Level.INFO, "DEBUG contextData keys: {0}", brokerContext.getContextData().keySet());
        LOG.log(Level.INFO, "DEBUG contextData entraRoles raw (prefixed): {0}",
                brokerContext.getContextData().get(USER_ATTR_PREFIX + EntraFblConstants.ATTR_ROLES));
        LOG.log(Level.INFO, "DEBUG contextData firstName raw (prefixed): {0}",
                brokerContext.getContextData().get(USER_ATTR_PREFIX + EntraFblConstants.ATTR_FIRST_NAME));

        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }

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
        Object value = ctx.getContextData().get(USER_ATTR_PREFIX + name);
        if (value instanceof List<?> list && !list.isEmpty()) {
            return String.valueOf(list.get(0));
        }
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private List<String> attrList(BrokeredIdentityContext ctx, String name) {
        Object value = ctx.getContextData().get(USER_ATTR_PREFIX + name);
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
            LOG.log(Level.WARNING, "rollingstone-entra-create-user: unparseable dateOfBirth ''{0}''", raw);
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