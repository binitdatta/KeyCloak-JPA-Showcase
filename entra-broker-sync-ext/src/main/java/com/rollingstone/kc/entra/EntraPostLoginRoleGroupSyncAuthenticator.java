package com.rollingstone.kc.entra;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.PostBrokerLoginConstants;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bound to the Entra IdP's Post Login Flow (rollingstone-entra-post-login) —
 * unlike the First Login Flow, this runs on EVERY subsequent brokered login.
 * Diffs the roles/groups carried in this login's SAML assertion against the
 * last-known ACTIVE set in MySQL: anything new is granted (in Keycloak and
 * MySQL), anything missing is revoked (in Keycloak and MySQL). This is what
 * makes a role/group removal in Entra actually take effect — on the user's
 * next login, not just once at account creation.
 *
 * KNOWN KEYCLOAK QUIRK: AbstractIdpAuthenticator#authenticate() (the base
 * class method, called by the framework BEFORE authenticateImpl()) only
 * looks for the serialized broker context under BROKERED_CONTEXT_NOTE --
 * the note key used by the First Broker Login Flow. When this authenticator
 * is bound to a Post Login Flow instead, Keycloak stores that same context
 * under a DIFFERENT note key, PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT,
 * so the base class's lookup always comes back null and throws
 * "Not found serialized context in clientSession" before our code ever runs.
 * (See https://groups.google.com/g/keycloak-user/c/eiX5dCjN3qc.)
 * authenticate() is overridden below to check both note keys.
 *
 * ATTRIBUTE SOURCE: same as ExtendedEntraCreateUserAuthenticator -- the IdP's
 * Attribute Importer mappers write claim values into
 * BrokeredIdentityContext.getContextData() under a "user.attributes." prefix
 * (confirmed via runtime dump), not under the bare attribute name.
 */
public class EntraPostLoginRoleGroupSyncAuthenticator extends AbstractIdpAuthenticator {

    public static final String PROVIDER_ID = "rollingstone-entra-post-login-sync";

    private static final Logger LOG = Logger.getLogger(EntraPostLoginRoleGroupSyncAuthenticator.class.getName());

    private static final String USER_ATTR_PREFIX = "user.attributes.";

    private final EntraUserSyncDao dao = new EntraUserSyncDao();

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        SerializedBrokeredIdentityContext serializedCtx =
                SerializedBrokeredIdentityContext.readFromAuthenticationSession(authSession, BROKERED_CONTEXT_NOTE);

        if (serializedCtx == null) {
            // Post Login Flow stores it under a different note than First Broker Login Flow does.
            serializedCtx = SerializedBrokeredIdentityContext.readFromAuthenticationSession(
                    authSession, PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT);
            LOG.log(Level.INFO, "rollingstone-entra-post-login-sync: fell back to PBL_BROKERED_IDENTITY_CONTEXT note, found={0}",
                    serializedCtx != null);
        }

        if (serializedCtx == null) {
            LOG.log(Level.SEVERE, "rollingstone-entra-post-login-sync: no serialized broker context under either note key");
            context.failure(AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
            return;
        }

        BrokeredIdentityContext brokerContext = serializedCtx.deserialize(context.getSession(), authSession);
        authenticateImpl(context, serializedCtx, brokerContext);
    }

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context,
                                    SerializedBrokeredIdentityContext serializedCtx,
                                    BrokeredIdentityContext brokerContext) {

        LOG.log(Level.INFO, "rollingstone-entra-post-login-sync: authenticateImpl ENTERED, idp identity={0}",
                brokerContext == null ? "null-brokerContext" : brokerContext.getId());

        UserModel user = context.getUser();
        RealmModel realm = context.getRealm();

        LOG.log(Level.INFO, "rollingstone-entra-post-login-sync: context.getUser()={0}",
                user == null ? "null" : user.getUsername());

        if (user == null) {
            LOG.log(Level.WARNING, "rollingstone-entra-post-login-sync: user is null, skipping sync");
            context.success();
            return;
        }

        try {
            Long idpUserId = dao.findIdByEntraObjectId(brokerContext.getId());
            LOG.log(Level.INFO, "rollingstone-entra-post-login-sync: resolved idpUserId={0} for entra id {1}",
                    new Object[]{idpUserId, brokerContext.getId()});

            if (idpUserId == null) {
                LOG.log(Level.WARNING, "rollingstone-entra-post-login-sync: no idp_user row for entra id {0}", brokerContext.getId());
                context.success();
                return;
            }

            LOG.log(Level.INFO, "DEBUG post-login contextData keys: {0}", brokerContext.getContextData().keySet());
            LOG.log(Level.INFO, "DEBUG post-login entraRoles raw (prefixed): {0}",
                    brokerContext.getContextData().get(USER_ATTR_PREFIX + EntraFblConstants.ATTR_ROLES));

            reconcile(realm, user, brokerContext, idpUserId);
            dao.touchLastLogin(idpUserId);
            LOG.log(Level.INFO, "rollingstone-entra-post-login-sync: reconcile + context.success() completed for idpUserId={0}", idpUserId);
            context.success();

        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "rollingstone-entra-post-login-sync: DB sync failed", e);
            context.getEvent().error("rollingstone_entra_post_login_sync_failed");
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        LOG.log(Level.INFO, "rollingstone-entra-post-login-sync: action() ENTERED");
        authenticate(context);
    }

    @Override
    protected void actionImpl(AuthenticationFlowContext context,
                              SerializedBrokeredIdentityContext serializedCtx,
                              BrokeredIdentityContext brokerContext) {
        LOG.log(Level.INFO, "rollingstone-entra-post-login-sync: actionImpl ENTERED (unexpected path)");
        authenticateImpl(context, serializedCtx, brokerContext);
    }

    private void reconcile(RealmModel realm, UserModel user, BrokeredIdentityContext brokerContext, long idpUserId)
            throws SQLException {

        Set<String> assertedRoles = new HashSet<>();
        Set<String> assertedGroups = new HashSet<>();
        for (String value : attrList(brokerContext, EntraFblConstants.ATTR_ROLES)) {
            if (value.startsWith(EntraFblConstants.GROUP_ROLE_PREFIX)) {
                assertedGroups.add(value.substring(EntraFblConstants.GROUP_ROLE_PREFIX.length()));
            } else {
                assertedRoles.add(value);
            }
        }

        Set<String> activeRoles = dao.currentActiveRoles(idpUserId);
        Set<String> activeGroups = dao.currentActiveGroups(idpUserId);

        LOG.log(Level.INFO, "rollingstone-entra-post-login-sync: assertedRoles={0} assertedGroups={1} activeRoles={2} activeGroups={3}",
                new Object[]{assertedRoles, assertedGroups, activeRoles, activeGroups});

        for (String roleName : assertedRoles) {
            if (!activeRoles.contains(roleName)) {
                RoleGroupSync.grantRoleInKeycloakAndDb(realm, user, dao, idpUserId, roleName, LOG);
            }
        }
        for (String roleName : activeRoles) {
            if (!assertedRoles.contains(roleName)) {
                RoleGroupSync.revokeRoleInKeycloakAndDb(realm, user, dao, idpUserId, roleName, LOG);
            }
        }

        for (String groupName : assertedGroups) {
            if (!activeGroups.contains(groupName)) {
                RoleGroupSync.joinGroupInKeycloakAndDb(realm, user, dao, idpUserId, groupName, LOG);
            }
        }
        for (String groupName : activeGroups) {
            if (!assertedGroups.contains(groupName)) {
                RoleGroupSync.leaveGroupInKeycloakAndDb(realm, user, dao, idpUserId, groupName, LOG);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> attrList(BrokeredIdentityContext ctx, String name) {
        Object value = ctx.getContextData().get(USER_ATTR_PREFIX + name);
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return value == null ? List.of() : List.of(String.valueOf(value));
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }
}