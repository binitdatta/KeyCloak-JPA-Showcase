package com.rollingstone.idpsync.dto;

import com.rollingstone.idpsync.domain.IdpUserGroup;

import java.time.LocalDateTime;

public record IdpUserGroupResponse(
        Long id,
        Long idpUserId,
        String groupName,
        String source,
        LocalDateTime assignedAt,
        LocalDateTime revokedAt
) {
    public static IdpUserGroupResponse from(IdpUserGroup g) {
        return new IdpUserGroupResponse(g.getId(), g.getIdpUserId(), g.getGroupName(), g.getSource(), g.getAssignedAt(), g.getRevokedAt());
    }
}
