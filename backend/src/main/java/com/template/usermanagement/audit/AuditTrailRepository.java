package com.template.usermanagement.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long> {

    Page<AuditTrail> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    Page<AuditTrail> findAll(Pageable pageable);

    List<AuditTrail> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    long count();
}
