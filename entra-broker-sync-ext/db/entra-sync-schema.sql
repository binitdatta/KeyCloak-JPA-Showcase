-- Run against the keycloak_jpa_demo MySQL 8 schema BEFORE the first Entra login test.
use ecommerce_demo;

CREATE TABLE idp_user (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    keycloak_user_id    VARCHAR(64)     NOT NULL,
    entra_object_id     VARCHAR(64)     NOT NULL,
    email               VARCHAR(255)    NOT NULL,
    first_name          VARCHAR(100),
    last_name           VARCHAR(100),
    date_of_birth       DATE,
    phone_number        VARCHAR(30),
    street_address      VARCHAR(255),
    city                VARCHAR(100),
    state               VARCHAR(100),
    postal_code         VARCHAR(20),
    country             VARCHAR(100),
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at       TIMESTAMP       NULL,
    UNIQUE KEY uq_idp_user_keycloak_id (keycloak_user_id),
    UNIQUE KEY uq_idp_user_entra_object_id (entra_object_id),
    KEY idx_idp_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE idp_user_role (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    idp_user_id     BIGINT UNSIGNED NOT NULL,
    role_name       VARCHAR(100)    NOT NULL,
    source          VARCHAR(20)     NOT NULL DEFAULT 'ENTRA',
    assigned_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at      TIMESTAMP       NULL,
    CONSTRAINT fk_idp_user_role_user FOREIGN KEY (idp_user_id) REFERENCES idp_user(id) ON DELETE CASCADE,
    KEY idx_idp_user_role_active (idp_user_id, role_name, revoked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE idp_user_group (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    idp_user_id     BIGINT UNSIGNED NOT NULL,
    group_name      VARCHAR(100)    NOT NULL,
    source          VARCHAR(20)     NOT NULL DEFAULT 'ENTRA',
    assigned_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at      TIMESTAMP       NULL,
    CONSTRAINT fk_idp_user_group_user FOREIGN KEY (idp_user_id) REFERENCES idp_user(id) ON DELETE CASCADE,
    KEY idx_idp_user_group_active (idp_user_id, group_name, revoked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE idp_sync_audit (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    idp_user_id     BIGINT UNSIGNED NOT NULL,
    event_type      VARCHAR(30)     NOT NULL,
    detail          VARCHAR(255),
    event_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_idp_sync_audit_user FOREIGN KEY (idp_user_id) REFERENCES idp_user(id) ON DELETE CASCADE,
    KEY idx_idp_sync_audit_user (idp_user_id, event_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- event_type values written by EntraUserSyncDao:
--   USER_CREATED, USER_UPDATED, ROLE_GRANTED, ROLE_REVOKED, GROUP_JOINED, GROUP_LEFT
