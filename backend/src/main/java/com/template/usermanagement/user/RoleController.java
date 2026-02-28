package com.template.usermanagement.user;

import com.template.usermanagement.common.ApiResponse;
import com.template.usermanagement.user.dto.RoleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.success(roleService.getAllRoles()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(roleService.getRoleById(id)));
    }

    @PostMapping("/{roleId}/users/{userId}")
    public ResponseEntity<ApiResponse<RoleResponse>> assignUser(
            @PathVariable Long roleId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(roleService.assignUserToRole(roleId, userId)));
    }

    @DeleteMapping("/{roleId}/users/{userId}")
    public ResponseEntity<ApiResponse<RoleResponse>> removeUser(
            @PathVariable Long roleId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(roleService.removeUserFromRole(roleId, userId)));
    }
}
