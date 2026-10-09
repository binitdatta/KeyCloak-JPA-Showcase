package com.rollingstone.idpsync.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Append-only event log. Deliberately exposed as READ-ONLY via the REST API
 * (see IdpUserController) -- rows are only ever written as a side effect of
 * the service layer's own grant/revoke/join/leave/create/update operations,
 * never directly by an API caller. This preserves it as a trustworthy audit
 * trail rather than an arbitrary CRUD table.
 */
@Entity
@Table(name = "idp_sync_audit")
@Getter
@Setter
@NoArgsConstructor
public class IdpSyncAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idp_user_id", nullable = false)
    private Long idpUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private SyncEventType eventType;

    @Column(name = "detail", length = 255)
    private String detail;

    @CreationTimestamp
    @Column(name = "event_at", nullable = false, updatable = false)
    private LocalDateTime eventAt;

    public IdpSyncAudit(Long idpUserId, SyncEventType eventType, String detail) {
        this.idpUserId = idpUserId;
        this.eventType = eventType;
        this.detail = detail;
    }
}
