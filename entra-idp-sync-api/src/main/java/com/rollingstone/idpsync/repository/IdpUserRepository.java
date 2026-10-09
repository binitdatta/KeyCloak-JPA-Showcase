package com.rollingstone.idpsync.repository;

import com.rollingstone.idpsync.domain.IdpUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdpUserRepository extends JpaRepository<IdpUser, Long> {

    Optional<IdpUser> findByEntraObjectId(String entraObjectId);

    Optional<IdpUser> findByKeycloakUserId(String keycloakUserId);

    Page<IdpUser> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    Page<IdpUser> findByStatus(String status, Pageable pageable);
}
