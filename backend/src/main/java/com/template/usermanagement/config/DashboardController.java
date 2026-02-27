package com.template.usermanagement.config;

import com.template.usermanagement.audit.AuditTrailService;
import com.template.usermanagement.common.ApiResponse;
import com.template.usermanagement.user.UserService;
import com.template.usermanagement.workflow.PendingActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final PendingActionService pendingActionService;
    private final AuditTrailService auditTrailService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER', 'CHECKER', 'VIEWER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = Map.of(
                "totalUsers", userService.countTotal(),
                "activeUsers", userService.countActive(),
                "pendingActions", pendingActionService.countPending(),
                "totalAuditEntries", auditTrailService.countTotal()
        );
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
