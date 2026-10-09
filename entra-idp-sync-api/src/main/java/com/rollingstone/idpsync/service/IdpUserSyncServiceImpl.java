package com.rollingstone.idpsync.service;

import com.rollingstone.idpsync.domain.IdpSyncAudit;
import com.rollingstone.idpsync.domain.IdpUser;
import com.rollingstone.idpsync.domain.IdpUserGroup;
import com.rollingstone.idpsync.domain.IdpUserRole;
import com.rollingstone.idpsync.domain.SyncEventType;
import com.rollingstone.idpsync.dto.UpdateIdpUserProfileRequest;
import com.rollingstone.idpsync.dto.UpsertIdpUserRequest;
import com.rollingstone.idpsync.exception.IdpUserNotFoundException;
import com.rollingstone.idpsync.repository.IdpSyncAuditRepository;
import com.rollingstone.idpsync.repository.IdpUserGroupRepository;
import com.rollingstone.idpsync.repository.IdpUserRepository;
import com.rollingstone.idpsync.repository.IdpUserRoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Direct, 1:1 replacement for the logic that used to live in the JDBC
 * EntraUserSyncDao + RoleGroupSync classes. Grant/join are idempotent (a
 * second grant of an already-active role/group is a safe no-op, matching
 * how EntraPostLoginRoleGroupSyncAuthenticator.reconcile() only calls grant
 * for roles not already in activeRoles). Revoke/leave are likewise safe
 * no-ops when nothing active exists for that name.
 */
@Service
@Transactional
public class IdpUserSyncServiceImpl implements IdpUserSyncService {

    private final IdpUserRepository userRepository;
    private final IdpUserRoleRepository roleRepository;
    private final IdpUserGroupRepository groupRepository;
    private final IdpSyncAuditRepository auditRepository;

    public IdpUserSyncServiceImpl(IdpUserRepository userRepository,
                                   IdpUserRoleRepository roleRepository,
                                   IdpUserGroupRepository groupRepository,
                                   IdpSyncAuditRepository auditRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.groupRepository = groupRepository;
        this.auditRepository = auditRepository;
    }

    @Override
    public UpsertResult upsertUser(String entraObjectId, UpsertIdpUserRequest request) {
        Optional<IdpUser> existing = userRepository.findByEntraObjectId(entraObjectId);

        if (existing.isEmpty()) {
            IdpUser user = new IdpUser(request.keycloakUserId(), entraObjectId, request.email());
            applyProfileFields(user, request);
            user = userRepository.save(user);
            audit(user.getId(), SyncEventType.USER_CREATED, request.email());
            return new UpsertResult(user, true);
        }

        IdpUser user = existing.get();
        // keycloak_user_id can legitimately change: e.g. if the Keycloak-side
        // user was deleted and recreated for the same Entra identity.
        user.setKeycloakUserId(request.keycloakUserId());
        user.setEmail(request.email());
        applyProfileFields(user, request);
        user = userRepository.save(user);
        audit(user.getId(), SyncEventType.USER_UPDATED, request.email());
        return new UpsertResult(user, false);
    }

    private void applyProfileFields(IdpUser user, UpsertIdpUserRequest request) {
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setDateOfBirth(request.dateOfBirth());
        user.setPhoneNumber(request.phoneNumber());
        user.setStreetAddress(request.streetAddress());
        user.setCity(request.city());
        user.setState(request.state());
        user.setPostalCode(request.postalCode());
        user.setCountry(request.country());
    }

    @Override
    public IdpUser getUser(long idpUserId) {
        return userRepository.findById(idpUserId)
                .orElseThrow(() -> new IdpUserNotFoundException(idpUserId));
    }

    @Override
    public Optional<IdpUser> findByEntraObjectId(String entraObjectId) {
        return userRepository.findByEntraObjectId(entraObjectId);
    }

    @Override
    public Page<IdpUser> listUsers(String email, String status, Pageable pageable) {
        if (StringUtils.hasText(email)) {
            return userRepository.findByEmailContainingIgnoreCase(email, pageable);
        }
        if (StringUtils.hasText(status)) {
            return userRepository.findByStatus(status, pageable);
        }
        return userRepository.findAll(pageable);
    }

    @Override
    public IdpUser updateProfile(long idpUserId, UpdateIdpUserProfileRequest request) {
        IdpUser user = getUser(idpUserId);
        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null) user.setLastName(request.lastName());
        if (request.dateOfBirth() != null) user.setDateOfBirth(request.dateOfBirth());
        if (request.phoneNumber() != null) user.setPhoneNumber(request.phoneNumber());
        if (request.streetAddress() != null) user.setStreetAddress(request.streetAddress());
        if (request.city() != null) user.setCity(request.city());
        if (request.state() != null) user.setState(request.state());
        if (request.postalCode() != null) user.setPostalCode(request.postalCode());
        if (request.country() != null) user.setCountry(request.country());
        if (request.status() != null) user.setStatus(request.status());
        user = userRepository.save(user);
        audit(user.getId(), SyncEventType.USER_UPDATED, "profile updated via API");
        return user;
    }

    @Override
    public void deleteUser(long idpUserId) {
        IdpUser user = getUser(idpUserId);
        // idp_user_role / idp_user_group / idp_sync_audit all have
        // ON DELETE CASCADE FKs back to idp_user.id -- this removes them too.
        userRepository.delete(user);
    }

    @Override
    public void touchLastLogin(long idpUserId) {
        IdpUser user = getUser(idpUserId);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public List<IdpUserRole> listRoles(long idpUserId, boolean activeOnly) {
        ensureUserExists(idpUserId);
        return activeOnly
                ? roleRepository.findByIdpUserIdAndRevokedAtIsNull(idpUserId)
                : roleRepository.findByIdpUserId(idpUserId);
    }

    @Override
    public List<String> currentActiveRoleNames(long idpUserId) {
        return listRoles(idpUserId, true).stream().map(IdpUserRole::getRoleName).collect(Collectors.toList());
    }

    @Override
    public IdpUserRole grantRole(long idpUserId, String roleName) {
        ensureUserExists(idpUserId);
        Optional<IdpUserRole> active = roleRepository.findFirstByIdpUserIdAndRoleNameAndRevokedAtIsNull(idpUserId, roleName);
        if (active.isPresent()) {
            return active.get();
        }
        IdpUserRole role = roleRepository.save(new IdpUserRole(idpUserId, roleName));
        audit(idpUserId, SyncEventType.ROLE_GRANTED, roleName);
        return role;
    }

    @Override
    public void revokeRole(long idpUserId, String roleName) {
        ensureUserExists(idpUserId);
        roleRepository.findFirstByIdpUserIdAndRoleNameAndRevokedAtIsNull(idpUserId, roleName)
                .ifPresent(role -> {
                    role.setRevokedAt(LocalDateTime.now());
                    roleRepository.save(role);
                    audit(idpUserId, SyncEventType.ROLE_REVOKED, roleName);
                });
    }

    @Override
    public List<IdpUserGroup> listGroups(long idpUserId, boolean activeOnly) {
        ensureUserExists(idpUserId);
        return activeOnly
                ? groupRepository.findByIdpUserIdAndRevokedAtIsNull(idpUserId)
                : groupRepository.findByIdpUserId(idpUserId);
    }

    @Override
    public List<String> currentActiveGroupNames(long idpUserId) {
        return listGroups(idpUserId, true).stream().map(IdpUserGroup::getGroupName).collect(Collectors.toList());
    }

    @Override
    public IdpUserGroup joinGroup(long idpUserId, String groupName) {
        ensureUserExists(idpUserId);
        Optional<IdpUserGroup> active = groupRepository.findFirstByIdpUserIdAndGroupNameAndRevokedAtIsNull(idpUserId, groupName);
        if (active.isPresent()) {
            return active.get();
        }
        IdpUserGroup group = groupRepository.save(new IdpUserGroup(idpUserId, groupName));
        audit(idpUserId, SyncEventType.GROUP_JOINED, groupName);
        return group;
    }

    @Override
    public void leaveGroup(long idpUserId, String groupName) {
        ensureUserExists(idpUserId);
        groupRepository.findFirstByIdpUserIdAndGroupNameAndRevokedAtIsNull(idpUserId, groupName)
                .ifPresent(group -> {
                    group.setRevokedAt(LocalDateTime.now());
                    groupRepository.save(group);
                    audit(idpUserId, SyncEventType.GROUP_LEFT, groupName);
                });
    }

    @Override
    public Page<IdpSyncAudit> listAudit(long idpUserId, SyncEventType eventType, Pageable pageable) {
        ensureUserExists(idpUserId);
        return eventType != null
                ? auditRepository.findByIdpUserIdAndEventTypeOrderByEventAtAsc(idpUserId, eventType, pageable)
                : auditRepository.findByIdpUserIdOrderByEventAtAsc(idpUserId, pageable);
    }

    private void ensureUserExists(long idpUserId) {
        if (!userRepository.existsById(idpUserId)) {
            throw new IdpUserNotFoundException(idpUserId);
        }
    }

    private void audit(Long idpUserId, SyncEventType eventType, String detail) {
        String trimmed = (detail != null && detail.length() > 255) ? detail.substring(0, 255) : detail;
        auditRepository.save(new IdpSyncAudit(idpUserId, eventType, trimmed));
    }
}
