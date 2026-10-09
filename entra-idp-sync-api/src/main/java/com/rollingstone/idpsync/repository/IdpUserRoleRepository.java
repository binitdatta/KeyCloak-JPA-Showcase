package com.rollingstone.idpsync.repository;

import com.rollingstone.idpsync.domain.IdpUserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IdpUserRoleRepository extends JpaRepository<IdpUserRole, Long> {

    List<IdpUserRole> findByIdpUserId(Long idpUserId);

    List<IdpUserRole> findByIdpUserIdAndRevokedAtIsNull(Long idpUserId);

    Optional<IdpUserRole> findFirstByIdpUserIdAndRoleNameAndRevokedAtIsNull(Long idpUserId, String roleName);
}
