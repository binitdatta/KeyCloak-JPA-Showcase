package com.rollingstone.kc.fbl;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs as the last, REQUIRED step of the rollingstone-first-broker-login flow, after
 * the user record is guaranteed to exist (idp-review-profile and
 * rollingstone-create-user-if-unique-ext have already run). Reads the
 * rollingstone.externalRoles / rollingstone.externalGroups auth-session notes written by
 * ExtendedCreateUserIfUniqueAuthenticator and applies them via
 * UserModel.grantRole(...) / UserModel.joinGroup(...).
 *
 * Deliberately never fails the login on a provisioning miss (an unmapped
 * role/group name logs a warning and is skipped) — losing a role mapping
 * should not lock a legitimate, already-externally-validated user out of
 * their first login.
 */
public class PostLoginRoleGroupAuthenticator implements Authenticator {

    public static final String PROVIDER_ID = "rollingstone-post-login-role-group";

    private static final Logger LOG = Logger.getLogger(PostLoginRoleGroupAuthenticator.class.getName());

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        RealmModel realm = context.getRealm();

        if (user == null) {
            // Nothing to provision yet; let the flow continue rather than blocking login.
            context.success();
            return;
        }

        String rolesNote = context.getAuthenticationSession().getAuthNote(rollingstoneFblConstants.NOTE_EXTERNAL_ROLES);
        String groupsNote = context.getAuthenticationSession().getAuthNote(rollingstoneFblConstants.NOTE_EXTERNAL_GROUPS);

        applyRoles(realm, user, rolesNote);
        applyGroups(realm, user, groupsNote);

        context.success();
    }

    private void applyRoles(RealmModel realm, UserModel user, String rolesNote) {
        if (rolesNote == null || rolesNote.isBlank()) {
            return;
        }
        Arrays.stream(rolesNote.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(roleName -> {
                    RoleModel role = realm.getRole(roleName);
                    if (role == null) {
                        LOG.log(Level.WARNING, "rollingstone-post-login-role-group: realm role '{0}' not found, skipping", roleName);
                        return;
                    }
                    user.grantRole(role);
                });
    }

    private void applyGroups(RealmModel realm, UserModel user, String groupsNote) {
        if (groupsNote == null || groupsNote.isBlank()) {
            return;
        }
        Arrays.stream(groupsNote.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(groupPath -> {
                    GroupModel group = findGroupByPath(realm, groupPath);
                    if (group == null) {
                        LOG.log(Level.WARNING, "rollingstone-post-login-role-group: group path '{0}' not found, skipping", groupPath);
                        return;
                    }
                    user.joinGroup(group);
                });
    }

    private GroupModel findGroupByPath(RealmModel realm, String path) {
        return realm.getGroupsStream()
                .flatMap(this::flattenGroup)
                .filter(g -> pathOf(g).equals(path))
                .findFirst()
                .orElse(null);
    }

    private java.util.stream.Stream<GroupModel> flattenGroup(GroupModel group) {
        return java.util.stream.Stream.concat(
                java.util.stream.Stream.of(group),
                group.getSubGroupsStream().flatMap(this::flattenGroup)
        );
    }

    private String pathOf(GroupModel group) {
        StringBuilder sb = new StringBuilder(group.getName());
        GroupModel parent = group.getParent();
        while (parent != null) {
            sb.insert(0, parent.getName() + "/");
            parent = parent.getParent();
        }
        return "/" + sb;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        authenticate(context);
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

    @Override
    public void close() {
    }
}
