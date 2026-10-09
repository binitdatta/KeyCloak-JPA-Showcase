package com.rollingstone.idpsync.domain;

/**
 * Mirrors the event_type values written by the original EntraUserSyncDao
 * (direct-JDBC version) into idp_sync_audit.event_type (VARCHAR(30)).
 */
public enum SyncEventType {
    USER_CREATED,
    USER_UPDATED,
    ROLE_GRANTED,
    ROLE_REVOKED,
    GROUP_JOINED,
    GROUP_LEFT
}
