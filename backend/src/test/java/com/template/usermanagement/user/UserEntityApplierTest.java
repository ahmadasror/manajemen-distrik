package com.template.usermanagement.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.usermanagement.TestFixtures;
import com.template.usermanagement.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEntityApplierTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UserEntityApplier userEntityApplier;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = TestFixtures.createUser(1L, "testuser", "ADMIN");
    }

    @Test
    @DisplayName("getEntityType should return USER")
    void getEntityType_ReturnsUser() {
        assertThat(userEntityApplier.getEntityType()).isEqualTo("USER");
    }

    @Nested
    @DisplayName("applyCreate")
    class ApplyCreate {

        @Test
        @DisplayName("should delegate to userService.applyCreate and return id")
        void applyCreate_DelegatesAndReturnsId() {
            Map<String, Object> payload = Map.of("username", "newuser");
            User createdUser = TestFixtures.createUser(5L, "newuser", "USER");

            when(userService.applyCreate(payload, "checker1")).thenReturn(createdUser);

            Long result = userEntityApplier.applyCreate(payload, "checker1");

            assertThat(result).isEqualTo(5L);
            verify(userService).applyCreate(payload, "checker1");
        }
    }

    @Nested
    @DisplayName("applyUpdate")
    class ApplyUpdate {

        @Test
        @DisplayName("should delegate to userService.applyUpdate")
        void applyUpdate_Delegates() {
            Map<String, Object> payload = Map.of("email", "updated@test.com");

            userEntityApplier.applyUpdate(1L, payload, "checker1");

            verify(userService).applyUpdate(1L, payload, "checker1");
        }
    }

    @Nested
    @DisplayName("applyDelete")
    class ApplyDelete {

        @Test
        @DisplayName("should delegate to userService.applyDelete")
        void applyDelete_Delegates() {
            userEntityApplier.applyDelete(1L, "checker1");

            verify(userService).applyDelete(1L, "checker1");
        }
    }

    @Nested
    @DisplayName("getCurrentState")
    class GetCurrentState {

        @Test
        @DisplayName("should return map of user state when user exists")
        void getCurrentState_Existing() {
            Map<String, Object> expectedState = Map.of(
                    "id", 1L,
                    "username", "testuser",
                    "email", "testuser@test.com"
            );

            when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testUser));
            when(objectMapper.convertValue(any(UserResponse.class), eq(Map.class))).thenReturn(expectedState);

            Map<String, Object> result = userEntityApplier.getCurrentState(1L);

            assertThat(result).isEqualTo(expectedState);
            assertThat(result).containsEntry("username", "testuser");
        }

        @Test
        @DisplayName("should return null when user not found")
        void getCurrentState_NotFound() {
            when(userRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            Map<String, Object> result = userEntityApplier.getCurrentState(99L);

            assertThat(result).isNull();
        }
    }
}
