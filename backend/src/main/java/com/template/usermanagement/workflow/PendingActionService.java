package com.template.usermanagement.workflow;

import com.template.usermanagement.audit.AuditTrailService;
import com.template.usermanagement.common.BusinessException;
import com.template.usermanagement.common.ErrorCode;
import com.template.usermanagement.common.ResourceNotFoundException;
import com.template.usermanagement.user.User;
import com.template.usermanagement.user.UserRepository;
import com.template.usermanagement.workflow.dto.PendingActionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PendingActionService {

    private final PendingActionRepository pendingActionRepository;
    private final UserRepository userRepository;
    private final EntityApplierRegistry entityApplierRegistry;
    private final AuditTrailService auditTrailService;

    @Transactional
    public PendingAction createPendingAction(String entityType, Long entityId, String actionType,
                                              Map<String, Object> payload, Map<String, Object> previousState,
                                              Long makerId) {
        // Check no existing pending action for this entity
        if (entityId != null && pendingActionRepository.existsByEntityTypeAndEntityIdAndStatus(entityType, entityId, "PENDING")) {
            throw new BusinessException("A pending action already exists for this entity", ErrorCode.PENDING_ALREADY_EXISTS);
        }

        User maker = userRepository.findById(makerId).orElseThrow();

        PendingAction pa = PendingAction.builder()
                .entityType(entityType)
                .entityId(entityId)
                .actionType(actionType)
                .payload(payload)
                .previousState(previousState)
                .status("PENDING")
                .maker(maker)
                .build();

        pa = pendingActionRepository.save(pa);
        log.info("Created pending action {} for {}/{} by user {}", pa.getId(), entityType, entityId, makerId);

        // Audit the submission
        auditTrailService.recordAudit(entityType, entityId, "SUBMIT_" + actionType,
                previousState, payload, maker.getUsername(), pa.getId());

        return pa;
    }

    @Transactional(readOnly = true)
    public Page<PendingActionResponse> getAll(String status, String entityType, Pageable pageable) {
        return pendingActionRepository.findAllFiltered(status, entityType, pageable)
                .map(PendingActionResponse::from);
    }

    @Transactional(readOnly = true)
    public PendingActionResponse getById(Long id) {
        PendingAction pa = pendingActionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pending action not found", ErrorCode.PENDING_NOT_FOUND));
        return PendingActionResponse.from(pa);
    }

    @Transactional
    public PendingActionResponse approve(Long id, Long checkerId, String remarks) {
        PendingAction pa = pendingActionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pending action not found", ErrorCode.PENDING_NOT_FOUND));

        validateForAction(pa, checkerId);

        User checker = userRepository.findById(checkerId).orElseThrow();
        EntityApplier applier = entityApplierRegistry.getApplier(pa.getEntityType());

        // Apply the change
        Map<String, Object> afterState = null;
        switch (pa.getActionType()) {
            case "CREATE" -> {
                Long newId = applier.applyCreate(pa.getPayload(), checker.getUsername());
                pa.setEntityId(newId);
                afterState = applier.getCurrentState(newId);
            }
            case "UPDATE" -> {
                applier.applyUpdate(pa.getEntityId(), pa.getPayload(), checker.getUsername());
                afterState = applier.getCurrentState(pa.getEntityId());
            }
            case "DELETE" -> {
                applier.applyDelete(pa.getEntityId(), checker.getUsername());
            }
        }

        pa.setStatus("APPROVED");
        // Sole-admin self-approval: leave checker null to satisfy DB constraint (maker_id != checker_id).
        // The audit trail below still records who approved it.
        if (!pa.getMaker().getId().equals(checkerId)) {
            pa.setChecker(checker);
        }
        pa.setRemarks(remarks);
        pendingActionRepository.save(pa);

        // Audit the approval
        auditTrailService.recordAudit(pa.getEntityType(), pa.getEntityId(),
                "APPROVE_" + pa.getActionType(), pa.getPreviousState(), afterState,
                checker.getUsername(), pa.getId());

        log.info("Approved pending action {} by checker {}", id, checkerId);
        return PendingActionResponse.from(pa);
    }

    @Transactional
    public PendingActionResponse reject(Long id, Long checkerId, String remarks) {
        PendingAction pa = pendingActionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pending action not found", ErrorCode.PENDING_NOT_FOUND));

        validateForAction(pa, checkerId);

        User checker = userRepository.findById(checkerId).orElseThrow();

        pa.setStatus("REJECTED");
        // Sole-admin self-rejection: leave checker null to satisfy DB constraint.
        if (!pa.getMaker().getId().equals(checkerId)) {
            pa.setChecker(checker);
        }
        pa.setRemarks(remarks);
        pendingActionRepository.save(pa);

        // Audit the rejection
        auditTrailService.recordAudit(pa.getEntityType(), pa.getEntityId(),
                "REJECT_" + pa.getActionType(), null, null,
                checker.getUsername(), pa.getId());

        log.info("Rejected pending action {} by checker {}", id, checkerId);
        return PendingActionResponse.from(pa);
    }

    @Transactional
    public PendingActionResponse cancel(Long id, Long userId) {
        PendingAction pa = pendingActionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pending action not found", ErrorCode.PENDING_NOT_FOUND));

        if (!"PENDING".equals(pa.getStatus())) {
            throw new BusinessException("Only pending actions can be cancelled", ErrorCode.PENDING_INVALID_STATUS);
        }
        if (!pa.getMaker().getId().equals(userId)) {
            throw new BusinessException("Only the maker can cancel this action", ErrorCode.PENDING_NOT_AUTHORIZED);
        }

        pa.setStatus("CANCELLED");
        pendingActionRepository.save(pa);

        User maker = userRepository.findById(userId).orElseThrow();
        auditTrailService.recordAudit(pa.getEntityType(), pa.getEntityId(),
                "CANCEL_" + pa.getActionType(), null, null,
                maker.getUsername(), pa.getId());

        log.info("Cancelled pending action {} by maker {}", id, userId);
        return PendingActionResponse.from(pa);
    }

    private void validateForAction(PendingAction pa, Long checkerId) {
        if (!"PENDING".equals(pa.getStatus())) {
            throw new BusinessException("Action is no longer pending", ErrorCode.PENDING_INVALID_STATUS);
        }
        if (pa.getMaker().getId().equals(checkerId) && !isSoleAdmin(checkerId)) {
            throw new BusinessException("Maker cannot approve/reject their own action", ErrorCode.PENDING_SAME_MAKER_CHECKER);
        }
    }

    /**
     * Returns true when the given user is an ADMIN and is the only active admin in the system.
     * In that case, maker-checker self-approval is permitted so that the sole admin can
     * bootstrap new users (e.g., E2E seed setup) without requiring a second admin.
     */
    private boolean isSoleAdmin(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName()));
        return isAdmin && userRepository.countActiveByRoleName("ADMIN") == 1;
    }

    public long countPending() {
        return pendingActionRepository.countByStatus("PENDING");
    }
}
