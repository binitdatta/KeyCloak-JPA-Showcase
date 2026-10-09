package com.rollingstone.idpsync.service;

import com.rollingstone.idpsync.domain.IdpSyncAudit;
import com.rollingstone.idpsync.domain.IdpUser;
import com.rollingstone.idpsync.domain.IdpUserGroup;
import com.rollingstone.idpsync.domain.IdpUserRole;
import com.rollingstone.idpsync.domain.SyncEventType;
import com.rollingstone.idpsync.dto.UpdateIdpUserProfileRequest;
import com.rollingstone.idpsync.dto.UpsertIdpUserRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Every method here corresponds directly to a call the current custom
 * Keycloak authenticators make against EntraUserSyncDao / RoleGroupSync
 * over JDBC. See README.md for the full DAO-method -> REST-endpoint map.
 */
public interface IdpUserSyncService {

    UpsertResult upsertUser(String entraObjectId, UpsertIdpUserRequest request);

    IdpUser getUser(long idpUserId);

    Optional<IdpUser> findByEntraObjectId(String entraObjectId);

    Page<IdpUser> listUsers(String email, String status, Pageable pageable);

    IdpUser updateProfile(long idpUserId, UpdateIdpUserProfileRequest request);

    void deleteUser(long idpUserId);

    void touchLastLogin(long idpUserId);

    List<IdpUserRole> listRoles(long idpUserId, boolean activeOnly);

    List<String> currentActiveRoleNames(long idpUserId);

    IdpUserRole grantRole(long idpUserId, String roleName);

    void revokeRole(long idpUserId, String roleName);

    List<IdpUserGroup> listGroups(long idpUserId, boolean activeOnly);

    List<String> currentActiveGroupNames(long idpUserId);

    IdpUserGroup joinGroup(long idpUserId, String groupName);

    void leaveGroup(long idpUserId, String groupName);

    Page<IdpSyncAudit> listAudit(long idpUserId, SyncEventType eventType, Pageable pageable);
}
