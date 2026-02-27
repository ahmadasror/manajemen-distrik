package com.template.usermanagement.workflow;

import com.template.usermanagement.common.ApiResponse;
import com.template.usermanagement.common.PageResponse;
import com.template.usermanagement.security.UserDetailsImpl;
import com.template.usermanagement.workflow.dto.PendingActionResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pending-actions")
@RequiredArgsConstructor
public class PendingActionController {

    private final PendingActionService pendingActionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER', 'CHECKER')")
    public ResponseEntity<ApiResponse<PageResponse<PendingActionResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String entityType) {

        Page<PendingActionResponse> result = pendingActionService.getAll(status, entityType,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER', 'CHECKER')")
    public ResponseEntity<ApiResponse<PendingActionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(pendingActionService.getById(id)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHECKER')")
    public ResponseEntity<ApiResponse<PendingActionResponse>> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ActionRemarkRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String remarks = request != null ? request.getRemarks() : null;
        return ResponseEntity.ok(ApiResponse.success("Action approved",
                pendingActionService.approve(id, userDetails.getId(), remarks)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHECKER')")
    public ResponseEntity<ApiResponse<PendingActionResponse>> reject(
            @PathVariable Long id,
            @RequestBody(required = false) ActionRemarkRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String remarks = request != null ? request.getRemarks() : null;
        return ResponseEntity.ok(ApiResponse.success("Action rejected",
                pendingActionService.reject(id, userDetails.getId(), remarks)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    public ResponseEntity<ApiResponse<PendingActionResponse>> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Action cancelled",
                pendingActionService.cancel(id, userDetails.getId())));
    }

    @Data
    public static class ActionRemarkRequest {
        private String remarks;
    }
}
