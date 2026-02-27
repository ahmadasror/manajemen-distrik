package com.template.usermanagement.audit;

import com.template.usermanagement.audit.dto.AuditTrailResponse;
import com.template.usermanagement.logging.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditTrailService {

    private final AuditTrailRepository auditTrailRepository;

    @Transactional
    public void recordAudit(String entityType, Long entityId, String action,
                            Map<String, Object> beforeState, Map<String, Object> afterState,
                            String performedBy, Long pendingActionId) {
        List<String> changedFields = computeChangedFields(beforeState, afterState);
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        String ipAddress = getClientIpAddress();

        AuditTrail audit = AuditTrail.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .beforeState(beforeState)
                .afterState(afterState)
                .changedFields(changedFields)
                .performedBy(performedBy)
                .ipAddress(ipAddress)
                .correlationId(correlationId)
                .pendingActionId(pendingActionId)
                .build();

        auditTrailRepository.save(audit);
        log.info("Audit recorded: {} on {}/{} by {}", action, entityType, entityId, performedBy);
    }

    @Transactional(readOnly = true)
    public Page<AuditTrailResponse> getAll(Pageable pageable) {
        return auditTrailRepository.findAll(pageable).map(AuditTrailResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AuditTrailResponse> getByEntity(String entityType, Long entityId, Pageable pageable) {
        return auditTrailRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable)
                .map(AuditTrailResponse::from);
    }

    @Transactional(readOnly = true)
    public AuditTrailResponse getById(Long id) {
        return auditTrailRepository.findById(id)
                .map(AuditTrailResponse::from)
                .orElse(null);
    }

    private List<String> computeChangedFields(Map<String, Object> before, Map<String, Object> after) {
        if (before == null || after == null) return Collections.emptyList();

        List<String> changed = new ArrayList<>();
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(before.keySet());
        allKeys.addAll(after.keySet());

        for (String key : allKeys) {
            Object beforeVal = before.get(key);
            Object afterVal = after.get(key);
            if (!Objects.equals(beforeVal, afterVal)) {
                changed.add(key);
            }
        }
        return changed;
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xff = request.getHeader("X-Forwarded-For");
                if (xff != null && !xff.isEmpty()) {
                    return xff.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    public long countTotal() {
        return auditTrailRepository.count();
    }
}
