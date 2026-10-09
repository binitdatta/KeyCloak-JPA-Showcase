package com.rollingstone.idpsync.dto;

import com.rollingstone.idpsync.domain.IdpSyncAudit;

import java.time.LocalDateTime;

public record IdpSyncAuditResponse(
        Long id,
        Long idpUserId,
        String eventType,
        String detail,
        LocalDateTime eventAt
) {
    public static IdpSyncAuditResponse from(IdpSyncAudit a) {
        return new IdpSyncAuditResponse(a.getId(), a.getIdpUserId(), a.getEventType().name(), a.getDetail(), a.getEventAt());
    }
}
