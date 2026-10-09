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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bound to the Entra IdP's Post Login Flow (havi-entra-post-login) — unlike
 * the First Login Flow, this runs on EVERY subsequent brokered login. Diffs
 * the roles/groups carried in this login's SAML assertion against the
 * last-known ACTIVE set in MySQL: anything new is granted (in Keycloak and
 * MySQL), anything missing is revoked (in Keycloak and MySQL). This is what
 * makes a role/group removal in Entra actually take effect — on the user's
 * next login, not just once at account creation.
 */
public class EntraPostLoginRoleGroupSyncAuthenticator extends AbstractIdpAuthenticator {

    public static final String PROVIDER_ID = "havi-entra-post-login-sync";

    private static final Logger LOG = Logger.getLogger(EntraPostLoginRoleGroupSyncAuthenticator.class.getName());

    private final EntraUserSyncDao dao = new EntraUserSyncDao();

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context,
                                    SerializedBrokeredIdentityContext serializedCtx,
                                    BrokeredIdentityContext brokerContext) {

        UserModel user = context.getUser();
        RealmModel realm = context.getRealm();

        if (user == null) {
            context.success();
            return;
        }

        try {
            Long idpUserId = dao.findIdByEntraObjectId(brokerContext.getId());
            if (idpUserId == null) {
                // Shouldn't happen — First Login Flow always runs first — but don't block login.
                LOG.log(Level.WARNING, "havi-entra-post-login-sync: no idp_user row for entra id {0}", brokerContext.getId());
                context.success();
                return;
            }

            reconcile(realm, user, brokerContext, idpUserId);
            dao.touchLastLogin(idpUserId);
            context.success();

        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "havi-entra-post-login-sync: DB sync failed", e);
            context.getEvent().error("havi_entra_post_login_sync_failed");
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
        }
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
        Object value = ctx.getContextData().get(name);
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return value == null ? List.of() : List.of(String.valueOf(value));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        authenticate(context);
    }

    @Override
    protected void actionImpl(AuthenticationFlowContext context,
                              SerializedBrokeredIdentityContext serializedCtx,
                              BrokeredIdentityContext brokerContext) {
        authenticateImpl(context, serializedCtx, brokerContext);
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
