package com.template.usermanagement.user;

import com.template.usermanagement.TestFixtures;
import com.template.usermanagement.common.BusinessException;
import com.template.usermanagement.common.ErrorCode;
import com.template.usermanagement.user.dto.RoleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleService roleService;

    private Role adminRole;
    private Role makerRole;
    private User adminUser;
    private User makerUser;

    @BeforeEach
    void setUp() {
        adminRole = TestFixtures.createRole(1L, "ADMIN");
        makerRole = TestFixtures.createRole(2L, "MAKER");
        adminUser = TestFixtures.createUser(1L, "admin", "ADMIN");
        makerUser = TestFixtures.createUser(2L, "maker", "MAKER");
    }

    @Nested
    @DisplayName("getAllRoles")
    class GetAllRoles {

        @Test
        @DisplayName("should return list of RoleResponse with user counts")
        void getAllRoles_ReturnsList() {
            when(roleRepository.findAll()).thenReturn(List.of(adminRole, makerRole));
            when(userRepository.findAllByRoleId(1L)).thenReturn(List.of(adminUser));
            when(userRepository.findAllByRoleId(2L)).thenReturn(List.of(makerUser));

            List<RoleResponse> result = roleService.getAllRoles();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("ADMIN");
            assertThat(result.get(0).getUserCount()).isEqualTo(1);
            assertThat(result.get(0).getUsers()).isNull();
            assertThat(result.get(1).getName()).isEqualTo("MAKER");
            assertThat(result.get(1).getUserCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return empty list when no roles exist")
        void getAllRoles_Empty() {
            when(roleRepository.findAll()).thenReturn(List.of());

            List<RoleResponse> result = roleService.getAllRoles();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getRoleById")
    class GetRoleById {

        @Test
        @DisplayName("should return role with users when found")
        void getRoleById_Found() {
            when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
            when(userRepository.findAllByRoleId(1L)).thenReturn(List.of(adminUser));

            RoleResponse result = roleService.getRoleById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("ADMIN");
            assertThat(result.getUserCount()).isEqualTo(1);
            assertThat(result.getUsers()).hasSize(1);
            assertThat(result.getUsers().get(0).getUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("should throw BusinessException when role not found")
        void getRoleById_NotFound() {
            when(roleRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.getRoleById(99L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("assignUserToRole")
    class AssignUserToRole {

        @Test
        @DisplayName("should assign user to role and return updated role")
        void assignUserToRole_Success() {
            // adminUser has ADMIN role (id=1). Assign makerRole (id=2) to adminUser — no id conflict.
            when(roleRepository.findById(2L)).thenReturn(Optional.of(makerRole));
            when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(adminUser));
            when(userRepository.save(any(User.class))).thenReturn(adminUser);
            when(userRepository.findAllByRoleId(2L)).thenReturn(List.of(adminUser, makerUser));

            RoleResponse result = roleService.assignUserToRole(2L, 1L);

            assertThat(result.getUserCount()).isEqualTo(2);
            verify(userRepository).save(adminUser);
        }

        @Test
        @DisplayName("should throw BusinessException when role not found")
        void assignUserToRole_RoleNotFound() {
            when(roleRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.assignUserToRole(99L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw BusinessException when user not found")
        void assignUserToRole_UserNotFound() {
            when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
            when(userRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.assignUserToRole(1L, 99L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw BusinessException when user already has the role")
        void assignUserToRole_AlreadyHasRole() {
            // adminUser already has ADMIN role (id=1)
            when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
            when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(adminUser));

            assertThatThrownBy(() -> roleService.assignUserToRole(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("removeUserFromRole")
    class RemoveUserFromRole {

        @Test
        @DisplayName("should remove user from role when user has multiple roles")
        void removeUserFromRole_Success() {
            // give makerUser a second role so removal is allowed
            User multiRoleUser = TestFixtures.createUser(2L, "maker", "MAKER", "VIEWER");
            when(roleRepository.findById(2L)).thenReturn(Optional.of(makerRole));
            when(userRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(multiRoleUser));
            when(userRepository.save(any(User.class))).thenReturn(multiRoleUser);
            when(userRepository.findAllByRoleId(2L)).thenReturn(List.of());

            RoleResponse result = roleService.removeUserFromRole(2L, 2L);

            assertThat(result.getUserCount()).isEqualTo(0);
            verify(userRepository).save(multiRoleUser);
        }

        @Test
        @DisplayName("should throw BusinessException when role not found")
        void removeUserFromRole_RoleNotFound() {
            when(roleRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.removeUserFromRole(99L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw BusinessException when user not found")
        void removeUserFromRole_UserNotFound() {
            when(roleRepository.findById(2L)).thenReturn(Optional.of(makerRole));
            when(userRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.removeUserFromRole(2L, 99L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw BusinessException when user does not have the role")
        void removeUserFromRole_UserDoesNotHaveRole() {
            // makerUser has MAKER (id=2), try to remove ADMIN (id=1) which they don't have
            when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
            when(userRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(makerUser));

            assertThatThrownBy(() -> roleService.removeUserFromRole(1L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_IN_ROLE);
        }

        @Test
        @DisplayName("should throw BusinessException when user has only one role")
        void removeUserFromRole_OnlyRole() {
            // makerUser has only MAKER — cannot remove it
            when(roleRepository.findById(2L)).thenReturn(Optional.of(makerRole));
            when(userRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(makerUser));

            assertThatThrownBy(() -> roleService.removeUserFromRole(2L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_IN_ROLE);
        }
    }
}
