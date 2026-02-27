package com.template.usermanagement.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.usermanagement.TestFixtures;
import com.template.usermanagement.common.BusinessException;
import com.template.usermanagement.common.ErrorCode;
import com.template.usermanagement.common.ResourceNotFoundException;
import com.template.usermanagement.user.dto.CreateUserRequest;
import com.template.usermanagement.user.dto.UpdateUserRequest;
import com.template.usermanagement.user.dto.UserResponse;
import com.template.usermanagement.workflow.PendingAction;
import com.template.usermanagement.workflow.PendingActionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PendingActionService pendingActionService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        testUser = TestFixtures.createUser(1L, "testuser", "ADMIN");
        adminRole = TestFixtures.createRole(1L, "ADMIN");
    }

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        @DisplayName("should return page of UserResponse")
        void getAllUsers_ReturnsPage() {
            Pageable pageable = PageRequest.of(0, 10);
            User user2 = TestFixtures.createUser(2L, "user2", "USER");
            Page<User> userPage = new PageImpl<>(List.of(testUser, user2), pageable, 2);

            when(userRepository.findAllActive(null, pageable)).thenReturn(userPage);

            Page<UserResponse> result = userService.getAllUsers(null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getUsername()).isEqualTo("testuser");
            assertThat(result.getContent().get(1).getUsername()).isEqualTo("user2");
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("should return UserResponse when user found")
        void getUserById_Found() {
            when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testUser));

            UserResponse result = userService.getUserById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getEmail()).isEqualTo("testuser@test.com");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void getUserById_NotFound() {
            when(userRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("should create pending action for new user")
        void createUser_Success() {
            CreateUserRequest request = new CreateUserRequest();
            request.setUsername("newuser");
            request.setEmail("new@test.com");
            request.setPassword("password123");
            request.setFullName("New User");
            request.setPhone("1234567890");
            request.setIsActive(true);
            request.setRoles(Set.of("ADMIN"));

            Map<String, Object> payload = Map.of(
                    "username", "newuser",
                    "email", "new@test.com",
                    "password", "password123",
                    "fullName", "New User",
                    "phone", "1234567890",
                    "isActive", true,
                    "roles", List.of("ADMIN")
            );

            PendingAction expectedPa = TestFixtures.createPendingAction(
                    1L, "USER", null, "CREATE", "PENDING",
                    testUser, null);

            when(userRepository.existsByUsernameAndDeletedFalse("newuser")).thenReturn(false);
            when(userRepository.existsByEmailAndDeletedFalse("new@test.com")).thenReturn(false);
            when(objectMapper.convertValue(eq(request), eq(Map.class))).thenReturn(new HashMap<>(payload));
            when(pendingActionService.createPendingAction(
                    eq("USER"), isNull(), eq("CREATE"), any(Map.class), isNull(), eq(10L)))
                    .thenReturn(expectedPa);

            PendingAction result = userService.createUser(request, 10L);

            assertThat(result).isEqualTo(expectedPa);
            verify(pendingActionService).createPendingAction(
                    eq("USER"), isNull(), eq("CREATE"), any(Map.class), isNull(), eq(10L));
        }

        @Test
        @DisplayName("should throw BusinessException when username already exists")
        void createUser_DuplicateUsername() {
            CreateUserRequest request = new CreateUserRequest();
            request.setUsername("testuser");
            request.setEmail("new@test.com");

            when(userRepository.existsByUsernameAndDeletedFalse("testuser")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
                    });
        }

        @Test
        @DisplayName("should throw BusinessException when email already exists")
        void createUser_DuplicateEmail() {
            CreateUserRequest request = new CreateUserRequest();
            request.setUsername("uniqueuser");
            request.setEmail("existing@test.com");

            when(userRepository.existsByUsernameAndDeletedFalse("uniqueuser")).thenReturn(false);
            when(userRepository.existsByEmailAndDeletedFalse("existing@test.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
                    });
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("should create pending action for existing user update")
        void updateUser_Success() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("updated@test.com");
            request.setFullName("Updated Name");

            Map<String, Object> payload = new HashMap<>();
            payload.put("email", "updated@test.com");
            payload.put("fullName", "Updated Name");
            payload.put("phone", null);

            Map<String, Object> previousState = Map.of(
                    "id", 1L,
                    "username", "testuser",
                    "email", "testuser@test.com"
            );

            PendingAction expectedPa = TestFixtures.createPendingAction(
                    2L, "USER", 1L, "UPDATE", "PENDING",
                    testUser, null);

            when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testUser));
            when(objectMapper.convertValue(eq(request), eq(Map.class))).thenReturn(payload);
            when(objectMapper.convertValue(any(UserResponse.class), eq(Map.class))).thenReturn(new HashMap<>(previousState));
            when(pendingActionService.createPendingAction(
                    eq("USER"), eq(1L), eq("UPDATE"), any(Map.class), any(Map.class), eq(10L)))
                    .thenReturn(expectedPa);

            PendingAction result = userService.updateUser(1L, request, 10L);

            assertThat(result).isEqualTo(expectedPa);
            verify(pendingActionService).createPendingAction(
                    eq("USER"), eq(1L), eq("UPDATE"), any(Map.class), any(Map.class), eq(10L));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void updateUser_NotFound() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("updated@test.com");

            when(userRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(99L, request, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("should create pending action with empty payload for existing user")
        void deleteUser_Success() {
            Map<String, Object> previousState = Map.of(
                    "id", 1L,
                    "username", "testuser"
            );

            PendingAction expectedPa = TestFixtures.createPendingAction(
                    3L, "USER", 1L, "DELETE", "PENDING",
                    testUser, null);

            when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testUser));
            when(objectMapper.convertValue(any(UserResponse.class), eq(Map.class))).thenReturn(new HashMap<>(previousState));
            when(pendingActionService.createPendingAction(
                    eq("USER"), eq(1L), eq("DELETE"), eq(Map.of()), any(Map.class), eq(10L)))
                    .thenReturn(expectedPa);

            PendingAction result = userService.deleteUser(1L, 10L);

            assertThat(result).isEqualTo(expectedPa);
            verify(pendingActionService).createPendingAction(
                    eq("USER"), eq(1L), eq("DELETE"), eq(Map.of()), any(Map.class), eq(10L));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void deleteUser_NotFound() {
            when(userRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("applyCreate")
    class ApplyCreate {

        @Test
        @DisplayName("should encode password, set roles and save user")
        void applyCreate_Success() {
            Map<String, Object> payload = new HashMap<>();
            payload.put("username", "newuser");
            payload.put("email", "new@test.com");
            payload.put("password", "plainPassword");
            payload.put("fullName", "New User");
            payload.put("phone", "1234567890");
            payload.put("isActive", true);
            payload.put("roles", List.of("ADMIN"));

            Set<Role> roles = Set.of(adminRole);

            when(passwordEncoder.encode("plainPassword")).thenReturn("$2a$10$encodedHash");
            when(roleRepository.findByNameIn(Set.of("ADMIN"))).thenReturn(roles);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(5L);
                return u;
            });

            User result = userService.applyCreate(payload, "checker1");

            assertThat(result.getUsername()).isEqualTo("newuser");
            assertThat(result.getEmail()).isEqualTo("new@test.com");
            assertThat(result.getPasswordHash()).isEqualTo("$2a$10$encodedHash");
            assertThat(result.getFullName()).isEqualTo("New User");
            assertThat(result.getRoles()).isEqualTo(roles);
            assertThat(result.getCreatedBy()).isEqualTo("checker1");
            assertThat(result.getUpdatedBy()).isEqualTo("checker1");

            verify(passwordEncoder).encode("plainPassword");
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("applyUpdate")
    class ApplyUpdate {

        @Test
        @DisplayName("should update only provided fields")
        void applyUpdate_PartialUpdate() {
            Map<String, Object> payload = new HashMap<>();
            payload.put("email", "updated@test.com");
            payload.put("fullName", "Updated Name");

            when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.applyUpdate(1L, payload, "checker1");

            assertThat(result.getEmail()).isEqualTo("updated@test.com");
            assertThat(result.getFullName()).isEqualTo("Updated Name");
            assertThat(result.getUpdatedBy()).isEqualTo("checker1");
            // phone should remain unchanged
            assertThat(result.getPhone()).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("should encode password if present in payload")
        void applyUpdate_WithPassword() {
            Map<String, Object> payload = new HashMap<>();
            payload.put("password", "newPassword");

            when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("newPassword")).thenReturn("$2a$10$newEncodedHash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.applyUpdate(1L, payload, "checker1");

            assertThat(result.getPasswordHash()).isEqualTo("$2a$10$newEncodedHash");
            verify(passwordEncoder).encode("newPassword");
        }

        @Test
        @DisplayName("should update roles if present in payload")
        void applyUpdate_WithRoles() {
            Map<String, Object> payload = new HashMap<>();
            payload.put("roles", List.of("USER"));

            Role userRole = TestFixtures.createRole(2L, "USER");
            Set<Role> newRoles = Set.of(userRole);

            when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testUser));
            when(roleRepository.findByNameIn(Set.of("USER"))).thenReturn(newRoles);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.applyUpdate(1L, payload, "checker1");

            assertThat(result.getRoles()).isEqualTo(newRoles);
        }
    }

    @Nested
    @DisplayName("applyDelete")
    class ApplyDelete {

        @Test
        @DisplayName("should set deleted=true and isActive=false")
        void applyDelete_Success() {
            when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.applyDelete(1L, "checker1");

            assertThat(testUser.getDeleted()).isTrue();
            assertThat(testUser.getIsActive()).isFalse();
            assertThat(testUser.getUpdatedBy()).isEqualTo("checker1");
            verify(userRepository).save(testUser);
        }
    }
}
