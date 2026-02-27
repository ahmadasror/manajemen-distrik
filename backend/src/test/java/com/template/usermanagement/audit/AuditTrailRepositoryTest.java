package com.template.usermanagement.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AuditTrailRepositoryTest {

    @Autowired
    private AuditTrailRepository auditTrailRepository;

    @BeforeEach
    void setUp() {
        auditTrailRepository.deleteAll();
    }

    private AuditTrail createAudit(String entityType, Long entityId, String action) {
        AuditTrail audit = AuditTrail.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .beforeState(Map.of("field", "before"))
                .afterState(Map.of("field", "after"))
                .changedFields(List.of("field"))
                .performedBy("admin")
                .ipAddress("127.0.0.1")
                .correlationId("test-corr-id")
                .build();
        return auditTrailRepository.save(audit);
    }

    @Test
    void findByEntityTypeAndEntityId_shouldReturnMatches() {
        createAudit("USER", 1L, "CREATE");
        createAudit("USER", 1L, "UPDATE");
        createAudit("USER", 2L, "CREATE");

        Page<AuditTrail> result = auditTrailRepository.findByEntityTypeAndEntityId("USER", 1L, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findByEntityTypeAndEntityId_shouldReturnEmptyForNoMatch() {
        Page<AuditTrail> result = auditTrailRepository.findByEntityTypeAndEntityId("USER", 999L, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void findAll_shouldReturnAllWithPagination() {
        createAudit("USER", 1L, "CREATE");
        createAudit("USER", 2L, "UPDATE");
        createAudit("USER", 3L, "DELETE");

        Page<AuditTrail> page1 = auditTrailRepository.findAll(PageRequest.of(0, 2));
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isEqualTo(3);

        Page<AuditTrail> page2 = auditTrailRepository.findAll(PageRequest.of(1, 2));
        assertThat(page2.getContent()).hasSize(1);
    }

    @Test
    void findByEntityTypeAndEntityIdOrderByCreatedAtDesc_shouldReturnOrdered() {
        createAudit("USER", 5L, "CREATE");
        createAudit("USER", 5L, "UPDATE");

        List<AuditTrail> result = auditTrailRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("USER", 5L);
        assertThat(result).hasSize(2);
    }

    @Test
    void count_shouldReturnTotalCount() {
        createAudit("USER", 1L, "CREATE");
        createAudit("USER", 2L, "UPDATE");

        assertThat(auditTrailRepository.count()).isEqualTo(2);
    }

    @Test
    void count_shouldReturnZeroWhenEmpty() {
        assertThat(auditTrailRepository.count()).isEqualTo(0);
    }
}
