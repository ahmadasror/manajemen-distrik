package com.template.usermanagement.user;

import com.template.usermanagement.common.BusinessException;
import com.template.usermanagement.common.ErrorCode;
import com.template.usermanagement.user.dto.RoleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(role -> {
                    long count = userRepository.findAllByRoleId(role.getId()).size();
                    return RoleResponse.fromList(role, count);
                })
                .collect(Collectors.toList());
    }

    public RoleResponse getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Role not found with id: " + id, ErrorCode.ROLE_NOT_FOUND));
        List<User> users = userRepository.findAllByRoleId(id);
        return RoleResponse.fromDetail(role, users);
    }

    @Transactional
    public RoleResponse assignUserToRole(Long roleId, Long userId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("Role not found with id: " + roleId, ErrorCode.ROLE_NOT_FOUND));
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException("User not found with id: " + userId, ErrorCode.USER_NOT_FOUND));

        boolean alreadyHasRole = user.getRoles().stream()
                .anyMatch(r -> r.getId().equals(roleId));
        if (alreadyHasRole) {
            throw new BusinessException(
                    "User already has role " + role.getName(),
                    ErrorCode.USER_ALREADY_EXISTS);
        }

        user.getRoles().add(role);
        userRepository.save(user);

        List<User> users = userRepository.findAllByRoleId(roleId);
        return RoleResponse.fromDetail(role, users);
    }

    @Transactional
    public RoleResponse removeUserFromRole(Long roleId, Long userId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("Role not found with id: " + roleId, ErrorCode.ROLE_NOT_FOUND));
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException("User not found with id: " + userId, ErrorCode.USER_NOT_FOUND));

        boolean hasRole = user.getRoles().stream()
                .anyMatch(r -> r.getId().equals(roleId));
        if (!hasRole) {
            throw new BusinessException(
                    "User does not have role " + role.getName(),
                    ErrorCode.USER_NOT_IN_ROLE);
        }

        if (user.getRoles().size() <= 1) {
            throw new BusinessException(
                    "Cannot remove the user's only role",
                    ErrorCode.USER_NOT_IN_ROLE);
        }

        user.getRoles().removeIf(r -> r.getId().equals(roleId));
        userRepository.save(user);

        List<User> users = userRepository.findAllByRoleId(roleId);
        return RoleResponse.fromDetail(role, users);
    }
}
