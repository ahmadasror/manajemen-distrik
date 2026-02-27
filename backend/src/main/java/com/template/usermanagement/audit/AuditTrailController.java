package com.template.usermanagement.audit;

import com.template.usermanagement.audit.dto.AuditTrailResponse;
import com.template.usermanagement.common.ApiResponse;
import com.template.usermanagement.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit-trail")
@RequiredArgsConstructor
public class AuditTrailController {

    private final AuditTrailService auditTrailService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHECKER', 'VIEWER')")
    public ResponseEntity<ApiResponse<PageResponse<AuditTrailResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<AuditTrailResponse> result = auditTrailService.getAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHECKER', 'VIEWER')")
    public ResponseEntity<ApiResponse<AuditTrailResponse>> getById(@PathVariable Long id) {
        AuditTrailResponse audit = auditTrailService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(audit));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHECKER', 'VIEWER')")
    public ResponseEntity<ApiResponse<PageResponse<AuditTrailResponse>>> getByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<AuditTrailResponse> result = auditTrailService.getByEntity(entityType, entityId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
    }
}
