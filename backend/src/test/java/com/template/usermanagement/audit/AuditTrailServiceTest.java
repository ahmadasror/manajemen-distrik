package com.template.usermanagement.audit;

import com.template.usermanagement.TestFixtures;
import com.template.usermanagement.audit.dto.AuditTrailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditTrailServiceTest {

    @Mock
    private AuditTrailRepository auditTrailRepository;

    @InjectMocks
    private AuditTrailService auditTrailService;

    @Nested
    @DisplayName("recordAudit")
    class RecordAudit {

        @Test
        @DisplayName("should save audit trail with correct fields and computed changedFields")
        void recordAudit_WithChangedFields() {
            Map<String, Object> before = Map.of("a", 1, "b", 2);
            Map<String, Object> after = Map.of("a", 1, "b", 3, "c", 4);

            when(auditTrailRepository.save(any(AuditTrail.class))).thenAnswer(inv -> inv.getArgument(0));

            auditTrailService.recordAudit("USER", 1L, "UPDATE", before, after, "admin", 10L);

            ArgumentCaptor<AuditTrail> captor = ArgumentCaptor.forClass(AuditTrail.class);
            verify(auditTrailRepository).save(captor.capture());

            AuditTrail saved = captor.getValue();
            assertThat(saved.getEntityType()).isEqualTo("USER");
            assertThat(saved.getEntityId()).isEqualTo(1L);
            assertThat(saved.getAction()).isEqualTo("UPDATE");
            assertThat(saved.getBeforeState()).isEqualTo(before);
            assertThat(saved.getAfterState()).isEqualTo(after);
            assertThat(saved.getPerformedBy()).isEqualTo("admin");
            assertThat(saved.getPendingActionId()).isEqualTo(10L);
            // b changed (2->3) and c is new (not in before)
            assertThat(saved.getChangedFields()).containsExactlyInAnyOrder("b", "c");
        }

        @Test
        @DisplayName("should save with empty changedFields when before is null")
        void recordAudit_NullBefore() {
            when(auditTrailRepository.save(any(AuditTrail.class))).thenAnswer(inv -> inv.getArgument(0));

            auditTrailService.recordAudit("USER", 1L, "CREATE", null, Map.of("a", 1), "admin", 10L);

            ArgumentCaptor<AuditTrail> captor = ArgumentCaptor.forClass(AuditTrail.class);
            verify(auditTrailRepository).save(captor.capture());

            assertThat(captor.getValue().getChangedFields()).isEmpty();
        }

        @Test
        @DisplayName("should save with empty changedFields when after is null")
        void recordAudit_NullAfter() {
            when(auditTrailRepository.save(any(AuditTrail.class))).thenAnswer(inv -> inv.getArgument(0));

            auditTrailService.recordAudit("USER", 1L, "DELETE", Map.of("a", 1), null, "admin", 10L);

            ArgumentCaptor<AuditTrail> captor = ArgumentCaptor.forClass(AuditTrail.class);
            verify(auditTrailRepository).save(captor.capture());

            assertThat(captor.getValue().getChangedFields()).isEmpty();
        }

        @Test
        @DisplayName("should save with empty changedFields when both before and after are null")
        void recordAudit_BothNull() {
            when(auditTrailRepository.save(any(AuditTrail.class))).thenAnswer(inv -> inv.getArgument(0));

            auditTrailService.recordAudit("USER", 1L, "CANCEL", null, null, "admin", 10L);

            ArgumentCaptor<AuditTrail> captor = ArgumentCaptor.forClass(AuditTrail.class);
            verify(auditTrailRepository).save(captor.capture());

            assertThat(captor.getValue().getChangedFields()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("should return page of AuditTrailResponse")
        void getAll_ReturnsPage() {
            Pageable pageable = PageRequest.of(0, 10);
            AuditTrail audit1 = TestFixtures.createAuditTrail(1L, "USER", 1L, "CREATE");
            AuditTrail audit2 = TestFixtures.createAuditTrail(2L, "USER", 2L, "UPDATE");
            Page<AuditTrail> page = new PageImpl<>(List.of(audit1, audit2), pageable, 2);

            when(auditTrailRepository.findAll(pageable)).thenReturn(page);

            Page<AuditTrailResponse> result = auditTrailService.getAll(pageable);

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getEntityType()).isEqualTo("USER");
            assertThat(result.getContent().get(0).getAction()).isEqualTo("CREATE");
            assertThat(result.getContent().get(1).getAction()).isEqualTo("UPDATE");
        }
    }

    @Nested
    @DisplayName("getByEntity")
    class GetByEntity {

        @Test
        @DisplayName("should return page of audits for specific entity")
        void getByEntity_ReturnsPage() {
            Pageable pageable = PageRequest.of(0, 10);
            AuditTrail audit = TestFixtures.createAuditTrail(1L, "USER", 1L, "UPDATE");
            Page<AuditTrail> page = new PageImpl<>(List.of(audit), pageable, 1);

            when(auditTrailRepository.findByEntityTypeAndEntityId("USER", 1L, pageable)).thenReturn(page);

            Page<AuditTrailResponse> result = auditTrailService.getByEntity("USER", 1L, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getEntityId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("should return AuditTrailResponse when found")
        void getById_Found() {
            AuditTrail audit = TestFixtures.createAuditTrail(1L, "USER", 1L, "CREATE");

            when(auditTrailRepository.findById(1L)).thenReturn(Optional.of(audit));

            AuditTrailResponse result = auditTrailService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEntityType()).isEqualTo("USER");
            assertThat(result.getAction()).isEqualTo("CREATE");
            assertThat(result.getPerformedBy()).isEqualTo("admin");
        }

        @Test
        @DisplayName("should return null when not found")
        void getById_NotFound() {
            when(auditTrailRepository.findById(99L)).thenReturn(Optional.empty());

            AuditTrailResponse result = auditTrailService.getById(99L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("countTotal")
    class CountTotal {

        @Test
        @DisplayName("should delegate to repository count")
        void countTotal_Delegates() {
            when(auditTrailRepository.count()).thenReturn(42L);

            long result = auditTrailService.countTotal();

            assertThat(result).isEqualTo(42L);
            verify(auditTrailRepository).count();
        }
    }
}
