package com.rollingstone.kc.entra;

import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

final class RoleGroupSync {

    private RoleGroupSync() {
    }

    static void grantRoleInKeycloakAndDb(RealmModel realm, UserModel user, EntraUserSyncDao dao,
                                         long idpUserId, String roleName, Logger log) throws SQLException {
        RoleModel role = realm.getRole(roleName);
        if (role == null) {
            log.log(Level.WARNING, "rollingstone-entra: realm role ''{0}'' not found, skipping grant", roleName);
            return;
        }
        user.grantRole(role);
        dao.grantRole(idpUserId, roleName);
    }

    static void revokeRoleInKeycloakAndDb(RealmModel realm, UserModel user, EntraUserSyncDao dao,
                                          long idpUserId, String roleName, Logger log) throws SQLException {
        RoleModel role = realm.getRole(roleName);
        if (role != null) {
            user.deleteRoleMapping(role);
        }
        dao.revokeRole(idpUserId, roleName);
    }

    static void joinGroupInKeycloakAndDb(RealmModel realm, UserModel user, EntraUserSyncDao dao,
                                         long idpUserId, String groupName, Logger log) throws SQLException {
        GroupModel group = findGroupByName(realm, groupName);
        if (group == null) {
            log.log(Level.WARNING, "rollingstone-entra: group ''{0}'' not found, skipping join", groupName);
            return;
        }
        user.joinGroup(group);
        dao.joinGroup(idpUserId, groupName);
    }

    static void leaveGroupInKeycloakAndDb(RealmModel realm, UserModel user, EntraUserSyncDao dao,
                                          long idpUserId, String groupName, Logger log) throws SQLException {
        GroupModel group = findGroupByName(realm, groupName);
        if (group != null) {
            user.leaveGroup(group);
        }
        dao.leaveGroup(idpUserId, groupName);
    }

    private static GroupModel findGroupByName(RealmModel realm, String name) {
        return realm.getGroupsStream()
                .flatMap(RoleGroupSync::flatten)
                .filter(g -> g.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private static Stream<GroupModel> flatten(GroupModel group) {
        return Stream.concat(Stream.of(group), group.getSubGroupsStream().flatMap(RoleGroupSync::flatten));
    }
}