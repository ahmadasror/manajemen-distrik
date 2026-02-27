package com.template.usermanagement;

import com.template.usermanagement.audit.AuditTrail;
import com.template.usermanagement.security.UserDetailsImpl;
import com.template.usermanagement.user.Role;
import com.template.usermanagement.user.User;
import com.template.usermanagement.workflow.PendingAction;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TestFixtures {

    private TestFixtures() {}

    public static Role createRole(Long id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setDescription(name + " role");
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        return role;
    }

    public static User createUser(Long id, String username, String... roleNames) {
        Set<Role> roles = new HashSet<>();
        long roleId = 1L;
        for (String roleName : roleNames) {
            roles.add(createRole(roleId++, roleName));
        }

        User user = User.builder()
                .username(username)
                .email(username + "@test.com")
                .passwordHash("$2a$10$encodedPasswordHash")
                .fullName(username + " FullName")
                .phone("1234567890")
                .isActive(true)
                .deleted(false)
                .version(0)
                .roles(roles)
                .build();
        user.setId(id);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setCreatedBy("system");
        user.setUpdatedBy("system");
        return user;
    }

    public static UserDetailsImpl createUserDetails(User user) {
        return new UserDetailsImpl(user);
    }

    public static UserDetailsImpl createUserDetails(Long id, String username, String... roleNames) {
        return createUserDetails(createUser(id, username, roleNames));
    }

    public static Authentication createAuthentication(UserDetailsImpl userDetails) {
        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
    }

    public static PendingAction createPendingAction(Long id, String entityType, Long entityId,
                                                      String actionType, String status,
                                                      User maker, User checker) {
        PendingAction pa = PendingAction.builder()
                .entityType(entityType)
                .entityId(entityId)
                .actionType(actionType)
                .payload(Map.of("username", "testuser"))
                .previousState(null)
                .status(status)
                .maker(maker)
                .checker(checker)
                .build();
        pa.setId(id);
        pa.setCreatedAt(LocalDateTime.now());
        pa.setUpdatedAt(LocalDateTime.now());
        return pa;
    }

    public static AuditTrail createAuditTrail(Long id, String entityType, Long entityId, String action) {
        return AuditTrail.builder()
                .id(id)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .beforeState(Map.of("field", "before"))
                .afterState(Map.of("field", "after"))
                .changedFields(List.of("field"))
                .performedBy("admin")
                .ipAddress("127.0.0.1")
                .correlationId("test-correlation-id")
                .pendingActionId(1L)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
