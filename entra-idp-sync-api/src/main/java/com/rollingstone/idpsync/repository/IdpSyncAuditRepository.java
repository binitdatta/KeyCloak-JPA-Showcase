package com.rollingstone.idpsync.repository;

import com.rollingstone.idpsync.domain.IdpSyncAudit;
import com.rollingstone.idpsync.domain.SyncEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdpSyncAuditRepository extends JpaRepository<IdpSyncAudit, Long> {

    Page<IdpSyncAudit> findByIdpUserIdOrderByEventAtAsc(Long idpUserId, Pageable pageable);

    Page<IdpSyncAudit> findByIdpUserIdAndEventTypeOrderByEventAtAsc(Long idpUserId, SyncEventType eventType, Pageable pageable);
}
