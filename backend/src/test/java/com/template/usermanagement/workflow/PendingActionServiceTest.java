package com.template.usermanagement.workflow;

import com.template.usermanagement.TestFixtures;
import com.template.usermanagement.audit.AuditTrailService;
import com.template.usermanagement.common.BusinessException;
import com.template.usermanagement.common.ErrorCode;
import com.template.usermanagement.user.User;
import com.template.usermanagement.user.UserRepository;
import com.template.usermanagement.workflow.dto.PendingActionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PendingActionServiceTest {

    @Mock
    private PendingActionRepository pendingActionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityApplierRegistry entityApplierRegistry;

    @Mock
    private AuditTrailService auditTrailService;

    @InjectMocks
    private PendingActionService pendingActionService;

    private User maker;
    private User checker;

    @BeforeEach
    void setUp() {
        maker = TestFixtures.createUser(1L, "maker", "ADMIN");
        checker = TestFixtures.createUser(2L, "checker", "ADMIN");
    }

    @Nested
    @DisplayName("createPendingAction")
    class CreatePendingAction {

        @Test
        @DisplayName("should save pending action and record audit")
        void createPendingAction_Success() {
            Map<String, Object> payload = Map.of("username", "newuser");
            Map<String, Object> previousState = Map.of("field", "value");

            when(pendingActionRepository.existsByEntityTypeAndEntityIdAndStatus("USER", 1L, "PENDING"))
                    .thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(maker));
            when(pendingActionRepository.save(any(PendingAction.class))).thenAnswer(inv -> {
                PendingAction pa = inv.getArgument(0);
                pa.setId(10L);
                return pa;
            });

            PendingAction result = pendingActionService.createPendingAction(
                    "USER", 1L, "UPDATE", payload, previousState, 1L);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getEntityType()).isEqualTo("USER");
            assertThat(result.getEntityId()).isEqualTo(1L);
            assertThat(result.getActionType()).isEqualTo("UPDATE");
            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getMaker()).isEqualTo(maker);

            verify(auditTrailService).recordAudit(
                    eq("USER"), eq(1L), eq("SUBMIT_UPDATE"),
                    eq(previousState), eq(payload), eq("maker"), eq(10L));
        }

        @Test
        @DisplayName("should throw PENDING_ALREADY_EXISTS when duplicate pending action for same entity")
        void createPendingAction_DuplicateEntity() {
            when(pendingActionRepository.existsByEntityTypeAndEntityIdAndStatus("USER", 1L, "PENDING"))
                    .thenReturn(true);

            assertThatThrownBy(() -> pendingActionService.createPendingAction(
                    "USER", 1L, "UPDATE", Map.of(), null, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.PENDING_ALREADY_EXISTS);
                    });
        }

        @Test
        @DisplayName("should skip duplicate check when entityId is null (CREATE)")
        void createPendingAction_NullEntityIdSkipsCheck() {
            Map<String, Object> payload = Map.of("username", "newuser");

            when(userRepository.findById(1L)).thenReturn(Optional.of(maker));
            when(pendingActionRepository.save(any(PendingAction.class))).thenAnswer(inv -> {
                PendingAction pa = inv.getArgument(0);
                pa.setId(11L);
                return pa;
            });

            PendingAction result = pendingActionService.createPendingAction(
                    "USER", null, "CREATE", payload, null, 1L);

            assertThat(result.getId()).isEqualTo(11L);
            assertThat(result.getActionType()).isEqualTo("CREATE");

            // Should never check for existing pending actions
            verify(pendingActionRepository, never()).existsByEntityTypeAndEntityIdAndStatus(anyString(), anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("should set APPROVED status and call applier for CREATE action")
        void approve_Create() {
            PendingAction pa = TestFixtures.createPendingAction(
                    1L, "USER", null, "CREATE", "PENDING", maker, null);
            pa.setPayload(Map.of("username", "newuser", "email", "new@test.com"));

            EntityApplier applier = mock(EntityApplier.class);
            Map<String, Object> afterState = Map.of("id", 5L, "username", "newuser");

            when(pendingActionRepository.findById(1L)).thenReturn(Optional.of(pa));
            when(userRepository.findById(2L)).thenReturn(Optional.of(checker));
            when(entityApplierRegistry.getApplier("USER")).thenReturn(applier);
            when(applier.applyCreate(pa.getPayload(), "checker")).thenReturn(5L);
            when(applier.getCurrentState(5L)).thenReturn(afterState);
            when(pendingActionRepository.save(any(PendingAction.class))).thenAnswer(inv -> inv.getArgument(0));

            PendingActionResponse result = pendingActionService.approve(1L, 2L, "Approved");

            assertThat(result.getStatus()).isEqualTo("APPROVED");
            assertThat(result.getEntityId()).isEqualTo(5L);
            assertThat(result.getCheckerId()).isEqualTo(2L);
            assertThat(result.getRemarks()).isEqualTo("Approved");

            verify(applier).applyCreate(pa.getPayload(), "checker");
            verify(auditTrailService).recordAudit(
                    eq("USER"), eq(5L), eq("APPROVE_CREATE"),
                    isNull(), eq(afterState), eq("checker"), eq(1L));
        }

        @Test
        @DisplayName("should call applyUpdate for UPDATE action")
        void approve_Update() {
            PendingAction pa = TestFixtures.createPendingAction(
                    2L, "USER", 1L, "UPDATE", "PENDING", maker, null);
            pa.setPayload(Map.of("email", "updated@test.com"));
            pa.setPreviousState(Map.of("email", "old@test.com"));

            EntityApplier applier = mock(EntityApplier.class);
            Map<String, Object> afterState = Map.of("email", "updated@test.com");

            when(pendingActionRepository.findById(2L)).thenReturn(Optional.of(pa));
            when(userRepository.findById(2L)).thenReturn(Optional.of(checker));
            when(entityApplierRegistry.getApplier("USER")).thenReturn(applier);
            when(applier.getCurrentState(1L)).thenReturn(afterState);
            when(pendingActionRepository.save(any(PendingAction.class))).thenAnswer(inv -> inv.getArgument(0));

            PendingActionResponse result = pendingActionService.approve(2L, 2L, "OK");

            assertThat(result.getStatus()).isEqualTo("APPROVED");
            verify(applier).applyUpdate(1L, pa.getPayload(), "checker");
        }

        @Test
        @DisplayName("should call applyDelete for DELETE action")
        void approve_Delete() {
            PendingAction pa = TestFixtures.createPendingAction(
                    3L, "USER", 1L, "DELETE", "PENDING", maker, null);
            pa.setPayload(Map.of());
            pa.setPreviousState(Map.of("username", "testuser"));

            EntityApplier applier = mock(EntityApplier.class);

            when(pendingActionRepository.findById(3L)).thenReturn(Optional.of(pa));
            when(userRepository.findById(2L)).thenReturn(Optional.of(checker));
            when(entityApplierRegistry.getApplier("USER")).thenReturn(applier);
            when(pendingActionRepository.save(any(PendingAction.class))).thenAnswer(inv -> inv.getArgument(0));

            PendingActionResponse result = pendingActionService.approve(3L, 2L, "Deleted");

            assertThat(result.getStatus()).isEqualTo("APPROVED");
            verify(applier).applyDelete(1L, "checker");
            // afterState should be null for DELETE
            verify(auditTrailService).recordAudit(
                    eq("USER"), eq(1L), eq("APPROVE_DELETE"),
                    eq(Map.of("username", "testuser")), isNull(), eq("checker"), eq(3L));
        }

        @Test
        @DisplayName("should throw PENDING_SAME_MAKER_CHECKER when same maker and checker and multiple admins exist")
        void approve_SameMakerChecker() {
            PendingAction pa = TestFixtures.createPendingAction(
                    4L, "USER", 1L, "UPDATE", "PENDING", maker, null);

            when(pendingActionRepository.findById(4L)).thenReturn(Optional.of(pa));
            when(userRepository.findById(1L)).thenReturn(Optional.of(maker));
            when(userRepository.countActiveByRoleName("ADMIN")).thenReturn(2L);

            assertThatThrownBy(() -> pendingActionService.approve(4L, 1L, "Self-approve"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.PENDING_SAME_MAKER_CHECKER);
                    });
        }

        @Test
        @DisplayName("should allow self-approval when checker is the sole admin in the system")
        void approve_SelfApproval_SoleAdmin() {
            PendingAction pa = TestFixtures.createPendingAction(
                    6L, "USER", null, "CREATE", "PENDING", maker, null);
            pa.setPayload(Map.of("username", "newuser", "email", "new@test.com"));

            EntityApplier applier = mock(EntityApplier.class);
            Map<String, Object> afterState = Map.of("id", 7L, "username", "newuser");

            when(pendingActionRepository.findById(6L)).thenReturn(Optional.of(pa));
            when(userRepository.findById(1L)).thenReturn(Optional.of(maker));  // for isSoleAdmin
            when(userRepository.countActiveByRoleName("ADMIN")).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(maker));  // for checker load
            when(entityApplierRegistry.getApplier("USER")).thenReturn(applier);
            when(applier.applyCreate(pa.getPayload(), "maker")).thenReturn(7L);
            when(applier.getCurrentState(7L)).thenReturn(afterState);
            when(pendingActionRepository.save(any(PendingAction.class))).thenAnswer(inv -> inv.getArgument(0));

            PendingActionResponse result = pendingActionService.approve(6L, 1L, "Bootstrap approve");

            assertThat(result.getStatus()).isEqualTo("APPROVED");
            assertThat(result.getEntityId()).isEqualTo(7L);
            // checker must be null on the PA to satisfy the DB constraint (maker_id != checker_id)
            assertThat(result.getCheckerId()).isNull();
            assertThat(result.getCheckerUsername()).isNull();
        }

        @Test
        @DisplayName("should throw PENDING_INVALID_STATUS when not PENDING")
        void approve_NonPendingStatus() {
            PendingAction pa = TestFixtures.createPendingAction(
                    5L, "USER", 1L, "UPDATE", "APPROVED", maker, checker);

            when(pendingActionRepository.findById(5L)).thenReturn(Optional.of(pa));

            assertThatThrownBy(() -> pendingActionService.approve(5L, 2L, "Late"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.PENDING_INVALID_STATUS);
                    });
        }
    }

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("should set REJECTED status and record audit")
        void reject_Success() {
            PendingAction pa = TestFixtures.createPendingAction(
                    1L, "USER", 1L, "UPDATE", "PENDING", maker, null);

            when(pendingActionRepository.findById(1L)).thenReturn(Optional.of(pa));
            when(userRepository.findById(2L)).thenReturn(Optional.of(checker));
            when(pendingActionRepository.save(any(PendingAction.class))).thenAnswer(inv -> inv.getArgument(0));

            PendingActionResponse result = pendingActionService.reject(1L, 2L, "Not approved");

            assertThat(result.getStatus()).isEqualTo("REJECTED");
            assertThat(result.getCheckerId()).isEqualTo(2L);
            assertThat(result.getRemarks()).isEqualTo("Not approved");

            verify(auditTrailService).recordAudit(
                    eq("USER"), eq(1L), eq("REJECT_UPDATE"),
                    isNull(), isNull(), eq("checker"), eq(1L));
        }

        @Test
        @DisplayName("should throw PENDING_SAME_MAKER_CHECKER when same maker rejects and multiple admins exist")
        void reject_SameMakerChecker() {
            PendingAction pa = TestFixtures.createPendingAction(
                    2L, "USER", 1L, "UPDATE", "PENDING", maker, null);

            when(pendingActionRepository.findById(2L)).thenReturn(Optional.of(pa));
            when(userRepository.findById(1L)).thenReturn(Optional.of(maker));
            when(userRepository.countActiveByRoleName("ADMIN")).thenReturn(2L);

            assertThatThrownBy(() -> pendingActionService.reject(2L, 1L, "Self-reject"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.PENDING_SAME_MAKER_CHECKER);
                    });
        }

        @Test
        @DisplayName("should throw PENDING_INVALID_STATUS when not PENDING")
        void reject_NonPendingStatus() {
            PendingAction pa = TestFixtures.createPendingAction(
                    3L, "USER", 1L, "UPDATE", "APPROVED", maker, checker);

            when(pendingActionRepository.findById(3L)).thenReturn(Optional.of(pa));

            assertThatThrownBy(() -> pendingActionService.reject(3L, 2L, "Late reject"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.PENDING_INVALID_STATUS);
                    });
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("should set CANCELLED status when cancelled by maker")
        void cancel_ByMaker() {
            PendingAction pa = TestFixtures.createPendingAction(
                    1L, "USER", 1L, "UPDATE", "PENDING", maker, null);

            when(pendingActionRepository.findById(1L)).thenReturn(Optional.of(pa));
            when(pendingActionRepository.save(any(PendingAction.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(1L)).thenReturn(Optional.of(maker));

            PendingActionResponse result = pendingActionService.cancel(1L, 1L);

            assertThat(result.getStatus()).isEqualTo("CANCELLED");

            verify(auditTrailService).recordAudit(
                    eq("USER"), eq(1L), eq("CANCEL_UPDATE"),
                    isNull(), isNull(), eq("maker"), eq(1L));
        }

        @Test
        @DisplayName("should throw PENDING_NOT_AUTHORIZED when cancelled by non-maker")
        void cancel_ByNonMaker() {
            PendingAction pa = TestFixtures.createPendingAction(
                    2L, "USER", 1L, "UPDATE", "PENDING", maker, null);

            when(pendingActionRepository.findById(2L)).thenReturn(Optional.of(pa));

            assertThatThrownBy(() -> pendingActionService.cancel(2L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.PENDING_NOT_AUTHORIZED);
                    });
        }

        @Test
        @DisplayName("should throw PENDING_INVALID_STATUS when not PENDING")
        void cancel_NonPendingStatus() {
            PendingAction pa = TestFixtures.createPendingAction(
                    3L, "USER", 1L, "UPDATE", "APPROVED", maker, checker);

            when(pendingActionRepository.findById(3L)).thenReturn(Optional.of(pa));

            assertThatThrownBy(() -> pendingActionService.cancel(3L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.PENDING_INVALID_STATUS);
                    });
        }
    }
}
