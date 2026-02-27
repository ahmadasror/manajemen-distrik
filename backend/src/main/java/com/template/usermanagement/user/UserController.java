package com.template.usermanagement.user;

import com.template.usermanagement.common.ApiResponse;
import com.template.usermanagement.common.PageResponse;
import com.template.usermanagement.security.UserDetailsImpl;
import com.template.usermanagement.user.dto.CreateUserRequest;
import com.template.usermanagement.user.dto.UpdateUserRequest;
import com.template.usermanagement.user.dto.UserResponse;
import com.template.usermanagement.workflow.PendingAction;
import com.template.usermanagement.workflow.dto.PendingActionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER', 'CHECKER', 'VIEWER')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Page<UserResponse> users = userService.getAllUsers(search, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(users)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER', 'CHECKER', 'VIEWER')")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    public ResponseEntity<ApiResponse<PendingActionResponse>> create(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PendingAction pa = userService.createUser(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User creation submitted for approval", PendingActionResponse.from(pa)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    public ResponseEntity<ApiResponse<PendingActionResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PendingAction pa = userService.updateUser(id, request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("User update submitted for approval", PendingActionResponse.from(pa)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    public ResponseEntity<ApiResponse<PendingActionResponse>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PendingAction pa = userService.deleteUser(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("User deletion submitted for approval", PendingActionResponse.from(pa)));
    }
}
