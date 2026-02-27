package com.template.usermanagement.workflow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PendingActionRepository extends JpaRepository<PendingAction, Long> {

    @Query("SELECT pa FROM PendingAction pa WHERE " +
            "(:status IS NULL OR pa.status = :status) " +
            "AND (:entityType IS NULL OR pa.entityType = :entityType)")
    Page<PendingAction> findAllFiltered(@Param("status") String status,
                                         @Param("entityType") String entityType,
                                         Pageable pageable);

    Optional<PendingAction> findByEntityTypeAndEntityIdAndStatus(String entityType, Long entityId, String status);

    boolean existsByEntityTypeAndEntityIdAndStatus(String entityType, Long entityId, String status);

    long countByStatus(String status);

    Page<PendingAction> findByMakerId(Long makerId, Pageable pageable);
}
