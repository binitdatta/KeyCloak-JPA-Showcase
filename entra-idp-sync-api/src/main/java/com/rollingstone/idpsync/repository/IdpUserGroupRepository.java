package com.rollingstone.idpsync.repository;

import com.rollingstone.idpsync.domain.IdpUserGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IdpUserGroupRepository extends JpaRepository<IdpUserGroup, Long> {

    List<IdpUserGroup> findByIdpUserId(Long idpUserId);

    List<IdpUserGroup> findByIdpUserIdAndRevokedAtIsNull(Long idpUserId);

    Optional<IdpUserGroup> findFirstByIdpUserIdAndGroupNameAndRevokedAtIsNull(Long idpUserId, String groupName);
}
