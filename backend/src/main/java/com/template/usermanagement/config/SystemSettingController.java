package com.template.usermanagement.config;

import com.template.usermanagement.common.ApiResponse;
import com.template.usermanagement.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService settingService;

    private static final List<String> GOOGLE_KEYS = List.of(
            "google.api.key", "google.api.cx", "validation.mode");

    /**
     * GET /api/v1/settings/validation
     * Returns current validation config (API key masked, CX visible, mode visible).
     */
    @GetMapping("/validation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getValidationSettings() {
        Map<String, String> settings = settingService.getSettingsForDisplay(GOOGLE_KEYS);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    /**
     * PUT /api/v1/settings/validation
     * Updates validation settings. Body: { "google.api.key": "...", "google.api.cx": "...", "validation.mode": "free|paid" }
     * Only non-null keys in the body are updated.
     */
    @PutMapping("/validation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateValidationSettings(
            @RequestBody Map<String, String> updates,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            if (GOOGLE_KEYS.contains(entry.getKey())) {
                settingService.setValue(entry.getKey(), entry.getValue(), userDetails.getUsername());
            }
        }
        Map<String, String> current = settingService.getSettingsForDisplay(GOOGLE_KEYS);
        return ResponseEntity.ok(ApiResponse.success("Settings updated", current));
    }
}
