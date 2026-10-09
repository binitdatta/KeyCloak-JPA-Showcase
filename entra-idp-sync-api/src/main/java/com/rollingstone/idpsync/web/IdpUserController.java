package com.rollingstone.idpsync.web;

import com.rollingstone.idpsync.domain.IdpUserGroup;
import com.rollingstone.idpsync.domain.IdpUserRole;
import com.rollingstone.idpsync.domain.SyncEventType;
import com.rollingstone.idpsync.dto.GroupRequest;
import com.rollingstone.idpsync.dto.IdpSyncAuditResponse;
import com.rollingstone.idpsync.dto.IdpUserGroupResponse;
import com.rollingstone.idpsync.dto.IdpUserResponse;
import com.rollingstone.idpsync.dto.IdpUserRoleResponse;
import com.rollingstone.idpsync.dto.RoleRequest;
import com.rollingstone.idpsync.dto.UpdateIdpUserProfileRequest;
import com.rollingstone.idpsync.dto.UpsertIdpUserRequest;
import com.rollingstone.idpsync.service.IdpUserSyncService;
import com.rollingstone.idpsync.service.UpsertResult;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * DAO-method -> endpoint map (see README.md for the full narrative):
 *
 *   dao.upsertUser(...)            -> PUT    /api/v1/idp-users/by-entra-object-id/{entraObjectId}
 *   dao.findIdByEntraObjectId(...) -> GET    /api/v1/idp-users/by-entra-object-id/{entraObjectId}
 *   dao.currentActiveRoles(...)    -> GET    /api/v1/idp-users/{id}/roles/active-names
 *   dao.currentActiveGroups(...)   -> GET    /api/v1/idp-users/{id}/groups/active-names
 *   dao.grantRole(...)             -> POST   /api/v1/idp-users/{id}/roles
 *   dao.revokeRole(...)            -> DELETE /api/v1/idp-users/{id}/roles/{roleName}
 *   dao.joinGroup(...)             -> POST   /api/v1/idp-users/{id}/groups
 *   dao.leaveGroup(...)            -> DELETE /api/v1/idp-users/{id}/groups/{groupName}
 *   dao.touchLastLogin(...)        -> POST   /api/v1/idp-users/{id}/touch-login
 *
 * Everything else (GET by id, list, PATCH, DELETE user, audit listing) is
 * general-purpose CRUD/admin surface, not currently called by the
 * authenticators.
 */
@RestController
@RequestMapping("/api/v1/idp-users")
public class IdpUserController {

    private final IdpUserSyncService service;

    public IdpUserController(IdpUserSyncService service) {
        this.service = service;
    }

    @PutMapping("/by-entra-object-id/{entraObjectId}")
    public ResponseEntity<IdpUserResponse> upsert(@PathVariable String entraObjectId,
                                                   @Valid @RequestBody UpsertIdpUserRequest request) {
        UpsertResult result = service.upsertUser(entraObjectId, request);
        IdpUserResponse body = IdpUserResponse.from(result.user());
        if (result.created()) {
            return ResponseEntity.created(URI.create("/api/v1/idp-users/" + result.user().getId())).body(body);
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    public IdpUserResponse getById(@PathVariable long id) {
        return IdpUserResponse.from(service.getUser(id));
    }

    @GetMapping("/by-entra-object-id/{entraObjectId}")
    public ResponseEntity<IdpUserResponse> getByEntraObjectId(@PathVariable String entraObjectId) {
        return service.findByEntraObjectId(entraObjectId)
                .map(u -> ResponseEntity.ok(IdpUserResponse.from(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public Page<IdpUserResponse> list(@RequestParam(required = false) String email,
                                       @RequestParam(required = false) String status,
                                       Pageable pageable) {
        return service.listUsers(email, status, pageable).map(IdpUserResponse::from);
    }

    @PatchMapping("/{id}")
    public IdpUserResponse updateProfile(@PathVariable long id, @RequestBody UpdateIdpUserProfileRequest request) {
        return IdpUserResponse.from(service.updateProfile(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/touch-login")
    public ResponseEntity<Void> touchLastLogin(@PathVariable long id) {
        service.touchLastLogin(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Roles ----

    @GetMapping("/{id}/roles")
    public List<IdpUserRoleResponse> listRoles(@PathVariable long id,
                                                @RequestParam(defaultValue = "true") boolean activeOnly) {
        return service.listRoles(id, activeOnly).stream().map(IdpUserRoleResponse::from).toList();
    }

    @GetMapping("/{id}/roles/active-names")
    public List<String> activeRoleNames(@PathVariable long id) {
        return service.currentActiveRoleNames(id);
    }

    @PostMapping("/{id}/roles")
    public ResponseEntity<IdpUserRoleResponse> grantRole(@PathVariable long id, @Valid @RequestBody RoleRequest request) {
        IdpUserRole role = service.grantRole(id, request.roleName());
        return ResponseEntity.status(HttpStatus.CREATED).body(IdpUserRoleResponse.from(role));
    }

    @DeleteMapping("/{id}/roles/{roleName}")
    public ResponseEntity<Void> revokeRole(@PathVariable long id, @PathVariable String roleName) {
        service.revokeRole(id, roleName);
        return ResponseEntity.noContent().build();
    }

    // ---- Groups ----

    @GetMapping("/{id}/groups")
    public List<IdpUserGroupResponse> listGroups(@PathVariable long id,
                                                  @RequestParam(defaultValue = "true") boolean activeOnly) {
        return service.listGroups(id, activeOnly).stream().map(IdpUserGroupResponse::from).toList();
    }

    @GetMapping("/{id}/groups/active-names")
    public List<String> activeGroupNames(@PathVariable long id) {
        return service.currentActiveGroupNames(id);
    }

    @PostMapping("/{id}/groups")
    public ResponseEntity<IdpUserGroupResponse> joinGroup(@PathVariable long id, @Valid @RequestBody GroupRequest request) {
        IdpUserGroup group = service.joinGroup(id, request.groupName());
        return ResponseEntity.status(HttpStatus.CREATED).body(IdpUserGroupResponse.from(group));
    }

    @DeleteMapping("/{id}/groups/{groupName}")
    public ResponseEntity<Void> leaveGroup(@PathVariable long id, @PathVariable String groupName) {
        service.leaveGroup(id, groupName);
        return ResponseEntity.noContent().build();
    }

    // ---- Audit (read-only -- see IdpSyncAudit javadoc) ----

    @GetMapping("/{id}/audit")
    public Page<IdpSyncAuditResponse> listAudit(@PathVariable long id,
                                                 @RequestParam(required = false) SyncEventType eventType,
                                                 Pageable pageable) {
        return service.listAudit(id, eventType, pageable).map(IdpSyncAuditResponse::from);
    }
}
