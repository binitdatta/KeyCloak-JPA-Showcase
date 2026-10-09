package com.rollingstone.idpsync.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "idp_user_role")
@Getter
@Setter
@NoArgsConstructor
public class IdpUserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idp_user_id", nullable = false)
    private Long idpUserId;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Column(name = "source", nullable = false, length = 20)
    private String source = "ENTRA";

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public IdpUserRole(Long idpUserId, String roleName) {
        this.idpUserId = idpUserId;
        this.roleName = roleName;
    }
}
