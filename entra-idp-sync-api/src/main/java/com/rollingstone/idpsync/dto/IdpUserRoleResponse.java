package com.rollingstone.idpsync.dto;

import com.rollingstone.idpsync.domain.IdpUserRole;

import java.time.LocalDateTime;

public record IdpUserRoleResponse(
        Long id,
        Long idpUserId,
        String roleName,
        String source,
        LocalDateTime assignedAt,
        LocalDateTime revokedAt
) {
    public static IdpUserRoleResponse from(IdpUserRole r) {
        return new IdpUserRoleResponse(r.getId(), r.getIdpUserId(), r.getRoleName(), r.getSource(), r.getAssignedAt(), r.getRevokedAt());
    }
}
