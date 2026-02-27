package com.template.usermanagement.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.usermanagement.common.BusinessException;
import com.template.usermanagement.common.ErrorCode;
import com.template.usermanagement.common.ResourceNotFoundException;
import com.template.usermanagement.user.dto.CreateUserRequest;
import com.template.usermanagement.user.dto.UpdateUserRequest;
import com.template.usermanagement.user.dto.UserResponse;
import com.template.usermanagement.workflow.PendingAction;
import com.template.usermanagement.workflow.PendingActionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final PendingActionService pendingActionService;

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String search, Pageable pageable) {
        return userRepository.findAllActive(search, pageable).map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    @Transactional
    public PendingAction createUser(CreateUserRequest request, Long makerId) {
        if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
            throw new BusinessException("Username already exists", ErrorCode.USER_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new BusinessException("Email already exists", ErrorCode.USER_ALREADY_EXISTS);
        }

        Map<String, Object> payload = objectMapper.convertValue(request, Map.class);
        return pendingActionService.createPendingAction("USER", null, "CREATE", payload, null, makerId);
    }

    @Transactional
    public PendingAction updateUser(Long userId, UpdateUserRequest request, Long makerId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

        Map<String, Object> payload = objectMapper.convertValue(request, Map.class);
        payload.entrySet().removeIf(e -> e.getValue() == null);
        Map<String, Object> previousState = objectMapper.convertValue(UserResponse.from(user), Map.class);

        return pendingActionService.createPendingAction("USER", userId, "UPDATE", payload, previousState, makerId);
    }

    @Transactional
    public PendingAction deleteUser(Long userId, Long makerId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

        Map<String, Object> previousState = objectMapper.convertValue(UserResponse.from(user), Map.class);
        return pendingActionService.createPendingAction("USER", userId, "DELETE", Map.of(), previousState, makerId);
    }

    // Direct apply methods called by UserEntityApplier
    @Transactional
    public User applyCreate(Map<String, Object> payload, String performedBy) {
        String password = (String) payload.get("password");
        Set<String> roleNames = Set.copyOf((java.util.Collection<String>) payload.get("roles"));
        Set<Role> roles = roleRepository.findByNameIn(roleNames);

        User user = User.builder()
                .username((String) payload.get("username"))
                .email((String) payload.get("email"))
                .passwordHash(passwordEncoder.encode(password))
                .fullName((String) payload.get("fullName"))
                .phone((String) payload.get("phone"))
                .isActive(payload.get("isActive") != null ? (Boolean) payload.get("isActive") : true)
                .roles(roles)
                .build();
        user.setCreatedBy(performedBy);
        user.setUpdatedBy(performedBy);

        return userRepository.save(user);
    }

    @Transactional
    public User applyUpdate(Long entityId, Map<String, Object> payload, String performedBy) {
        User user = userRepository.findByIdAndDeletedFalse(entityId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

        if (payload.containsKey("email")) user.setEmail((String) payload.get("email"));
        if (payload.containsKey("fullName")) user.setFullName((String) payload.get("fullName"));
        if (payload.containsKey("phone")) user.setPhone((String) payload.get("phone"));
        if (payload.containsKey("isActive")) user.setIsActive((Boolean) payload.get("isActive"));
        if (payload.containsKey("password")) {
            user.setPasswordHash(passwordEncoder.encode((String) payload.get("password")));
        }
        if (payload.containsKey("roles")) {
            Set<String> roleNames = Set.copyOf((java.util.Collection<String>) payload.get("roles"));
            user.setRoles(roleRepository.findByNameIn(roleNames));
        }
        user.setUpdatedBy(performedBy);

        return userRepository.save(user);
    }

    @Transactional
    public void applyDelete(Long entityId, String performedBy) {
        User user = userRepository.findByIdAndDeletedFalse(entityId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));
        user.setDeleted(true);
        user.setIsActive(false);
        user.setUpdatedBy(performedBy);
        userRepository.save(user);
    }

    public long countTotal() {
        return userRepository.countByDeletedFalse();
    }

    public long countActive() {
        return userRepository.countByIsActiveTrueAndDeletedFalse();
    }
}
