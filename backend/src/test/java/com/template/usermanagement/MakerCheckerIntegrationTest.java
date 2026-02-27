package com.template.usermanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.usermanagement.audit.AuditTrail;
import com.template.usermanagement.audit.AuditTrailRepository;
import com.template.usermanagement.common.BusinessException;
import com.template.usermanagement.security.UserDetailsImpl;
import com.template.usermanagement.user.*;
import com.template.usermanagement.user.dto.CreateUserRequest;
import com.template.usermanagement.user.dto.UpdateUserRequest;
import com.template.usermanagement.workflow.PendingAction;
import com.template.usermanagement.workflow.PendingActionRepository;
import com.template.usermanagement.workflow.PendingActionService;
import com.template.usermanagement.workflow.dto.PendingActionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MakerCheckerIntegrationTest {

    @Autowired private UserService userService;
    @Autowired private PendingActionService pendingActionService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PendingActionRepository pendingActionRepository;
    @Autowired private AuditTrailRepository auditTrailRepository;
    @Autowired private ObjectMapper objectMapper;

    private User maker;
    private User checker;

    @BeforeEach
    void setUp() {
        Role makerRole = roleRepository.findByName("MAKER").orElseThrow();
        Role checkerRole = roleRepository.findByName("CHECKER").orElseThrow();

        // Create maker user directly (not through workflow)
        maker = User.builder()
                .username("integ_maker")
                .email("integ_maker@test.com")
                .passwordHash("$2a$10$hash")
                .fullName("Integration Maker")
                .isActive(true)
                .deleted(false)
                .version(0)
                .roles(Set.of(makerRole))
                .build();
        maker.setCreatedBy("system");
        maker.setUpdatedBy("system");
        maker = userRepository.save(maker);

        checker = User.builder()
                .username("integ_checker")
                .email("integ_checker@test.com")
                .passwordHash("$2a$10$hash")
                .fullName("Integration Checker")
                .isActive(true)
                .deleted(false)
                .version(0)
                .roles(Set.of(checkerRole))
                .build();
        checker.setCreatedBy("system");
        checker.setUpdatedBy("system");
        checker = userRepository.save(checker);

        setAuth(maker);
    }

    private void setAuth(User user) {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @Test
    void createUserFlow_submitThenApprove() {
        // Maker submits create request
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@test.com");
        request.setPassword("Password123!");
        request.setFullName("New User");
        request.setPhone("1234567890");
        request.setRoles(Set.of("VIEWER"));

        PendingAction pa = userService.createUser(request, maker.getId());

        assertThat(pa).isNotNull();
        assertThat(pa.getStatus()).isEqualTo("PENDING");
        assertThat(pa.getActionType()).isEqualTo("CREATE");
        assertThat(pa.getEntityType()).isEqualTo("USER");
        assertThat(pa.getEntityId()).isNull(); // New user, no ID yet

        // User should not exist yet
        assertThat(userRepository.findByUsernameAndDeletedFalse("newuser")).isEmpty();

        // Checker approves
        PendingActionResponse approved = pendingActionService.approve(pa.getId(), checker.getId(), "Looks good");

        assertThat(approved.getStatus()).isEqualTo("APPROVED");
        assertThat(approved.getEntityId()).isNotNull(); // Now has an entity ID

        // User should exist now
        User createdUser = userRepository.findByUsernameAndDeletedFalse("newuser").orElseThrow();
        assertThat(createdUser.getEmail()).isEqualTo("newuser@test.com");
        assertThat(createdUser.getFullName()).isEqualTo("New User");
        assertThat(createdUser.getRoles()).anyMatch(r -> r.getName().equals("VIEWER"));

        // Audit trail should have entries
        List<AuditTrail> audits = auditTrailRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                "USER", createdUser.getId());
        assertThat(audits).hasSizeGreaterThanOrEqualTo(1);
        assertThat(audits).anyMatch(a -> a.getAction().contains("APPROVE_CREATE"));
    }

    @Test
    void updateUserFlow_submitThenApprove() {
        // First create a user to update
        User existingUser = User.builder()
                .username("existinguser")
                .email("existing@test.com")
                .passwordHash("$2a$10$hash")
                .fullName("Existing User")
                .isActive(true)
                .deleted(false)
                .version(0)
                .roles(Set.of(roleRepository.findByName("VIEWER").orElseThrow()))
                .build();
        existingUser.setCreatedBy("system");
        existingUser.setUpdatedBy("system");
        existingUser = userRepository.save(existingUser);

        // Maker submits update request
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setEmail("updated@test.com");
        updateRequest.setFullName("Updated User");

        PendingAction pa = userService.updateUser(existingUser.getId(), updateRequest, maker.getId());

        assertThat(pa.getActionType()).isEqualTo("UPDATE");
        assertThat(pa.getEntityId()).isEqualTo(existingUser.getId());

        // Email should not be updated yet
        User unchanged = userRepository.findByIdAndDeletedFalse(existingUser.getId()).orElseThrow();
        assertThat(unchanged.getEmail()).isEqualTo("existing@test.com");

        // Checker approves
        pendingActionService.approve(pa.getId(), checker.getId(), "Approved");

        // Now email should be updated
        User updated = userRepository.findByIdAndDeletedFalse(existingUser.getId()).orElseThrow();
        assertThat(updated.getEmail()).isEqualTo("updated@test.com");
        assertThat(updated.getFullName()).isEqualTo("Updated User");
    }

    @Test
    void deleteUserFlow_submitThenApprove() {
        // Create user to delete
        User toDelete = User.builder()
                .username("todelete")
                .email("todelete@test.com")
                .passwordHash("$2a$10$hash")
                .fullName("To Delete")
                .isActive(true)
                .deleted(false)
                .version(0)
                .roles(Set.of(roleRepository.findByName("VIEWER").orElseThrow()))
                .build();
        toDelete.setCreatedBy("system");
        toDelete.setUpdatedBy("system");
        toDelete = userRepository.save(toDelete);

        PendingAction pa = userService.deleteUser(toDelete.getId(), maker.getId());
        assertThat(pa.getActionType()).isEqualTo("DELETE");

        // User should still be active
        assertThat(userRepository.findByIdAndDeletedFalse(toDelete.getId())).isPresent();

        // Checker approves
        pendingActionService.approve(pa.getId(), checker.getId(), "Delete approved");

        // User should be soft-deleted
        assertThat(userRepository.findByIdAndDeletedFalse(toDelete.getId())).isEmpty();
        // But still exists in DB
        assertThat(userRepository.findById(toDelete.getId())).isPresent();
    }

    @Test
    void makerCannotApproveOwnAction() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("selfapprove");
        request.setEmail("selfapprove@test.com");
        request.setPassword("Password123!");
        request.setFullName("Self Approve");
        request.setRoles(Set.of("VIEWER"));

        PendingAction pa = userService.createUser(request, maker.getId());

        assertThatThrownBy(() -> pendingActionService.approve(pa.getId(), maker.getId(), "Self approve"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Maker cannot approve/reject their own action");
    }

    @Test
    void makerCanCancelOwnPendingAction() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("cancelme");
        request.setEmail("cancelme@test.com");
        request.setPassword("Password123!");
        request.setFullName("Cancel Me");
        request.setRoles(Set.of("VIEWER"));

        PendingAction pa = userService.createUser(request, maker.getId());

        PendingActionResponse cancelled = pendingActionService.cancel(pa.getId(), maker.getId());
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void checkerCanRejectPendingAction() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("rejectme");
        request.setEmail("rejectme@test.com");
        request.setPassword("Password123!");
        request.setFullName("Reject Me");
        request.setRoles(Set.of("VIEWER"));

        PendingAction pa = userService.createUser(request, maker.getId());

        PendingActionResponse rejected = pendingActionService.reject(pa.getId(), checker.getId(), "Not acceptable");
        assertThat(rejected.getStatus()).isEqualTo("REJECTED");

        // User should not exist
        assertThat(userRepository.findByUsernameAndDeletedFalse("rejectme")).isEmpty();
    }

    @Test
    void cannotApproveAlreadyApprovedAction() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("doubleapprove");
        request.setEmail("doubleapprove@test.com");
        request.setPassword("Password123!");
        request.setFullName("Double Approve");
        request.setRoles(Set.of("VIEWER"));

        PendingAction pa = userService.createUser(request, maker.getId());
        pendingActionService.approve(pa.getId(), checker.getId(), "First approve");

        assertThatThrownBy(() -> pendingActionService.approve(pa.getId(), checker.getId(), "Second approve"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no longer pending");
    }

    @Test
    void auditTrailRecordedForCreateApproval() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("auditcheck");
        request.setEmail("auditcheck@test.com");
        request.setPassword("Password123!");
        request.setFullName("Audit Check");
        request.setRoles(Set.of("VIEWER"));

        PendingAction pa = userService.createUser(request, maker.getId());
        pendingActionService.approve(pa.getId(), checker.getId(), "Approved");

        // There should be audit entries: SUBMIT_CREATE and APPROVE_CREATE
        User createdUser = userRepository.findByUsernameAndDeletedFalse("auditcheck").orElseThrow();
        List<AuditTrail> audits = auditTrailRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                "USER", createdUser.getId());

        assertThat(audits).hasSizeGreaterThanOrEqualTo(1);
        assertThat(audits.stream().map(AuditTrail::getAction))
                .contains("APPROVE_CREATE");
    }
}
