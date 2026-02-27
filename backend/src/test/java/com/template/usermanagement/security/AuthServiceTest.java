package com.template.usermanagement.security;

import com.template.usermanagement.TestFixtures;
import com.template.usermanagement.common.BusinessException;
import com.template.usermanagement.common.ErrorCode;
import com.template.usermanagement.config.JwtConfig;
import com.template.usermanagement.user.User;
import com.template.usermanagement.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private UserDetailsImpl testUserDetails;

    @BeforeEach
    void setUp() {
        testUser = TestFixtures.createUser(1L, "admin", "ADMIN");
        testUserDetails = TestFixtures.createUserDetails(testUser);
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("should return tokens and user info on successful authentication")
        void login_Success() {
            Authentication authentication = TestFixtures.createAuthentication(testUserDetails);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(tokenProvider.generateAccessToken(testUserDetails)).thenReturn("access-token-123");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(jwtConfig.getRefreshTokenExpiration()).thenReturn(86400000L);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = authService.login("admin", "password");

            assertThat(result).containsKey("accessToken");
            assertThat(result.get("accessToken")).isEqualTo("access-token-123");
            assertThat(result).containsKey("refreshToken");
            assertThat(result.get("refreshToken")).isNotNull();
            assertThat(result).containsKey("user");

            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) result.get("user");
            assertThat(userInfo.get("id")).isEqualTo(1L);
            assertThat(userInfo.get("username")).isEqualTo("admin");
            assertThat(userInfo.get("email")).isEqualTo("admin@test.com");
            assertThat(userInfo.get("fullName")).isEqualTo("admin FullName");
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) userInfo.get("roles");
            assertThat(roles).containsExactly("ADMIN");

            verify(refreshTokenRepository).revokeAllByUserId(1L);
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("should throw BadCredentialsException for invalid credentials")
        void login_InvalidCredentials() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login("admin", "wrongpassword"))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("should return new tokens for valid refresh token")
        void refresh_ValidToken() {
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(1L)
                    .user(testUser)
                    .token("valid-refresh-token")
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByTokenAndRevokedFalse("valid-refresh-token"))
                    .thenReturn(Optional.of(refreshToken));
            when(tokenProvider.generateAccessToken(any(UserDetailsImpl.class))).thenReturn("new-access-token");
            when(jwtConfig.getRefreshTokenExpiration()).thenReturn(86400000L);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = authService.refresh("valid-refresh-token");

            assertThat(result.get("accessToken")).isEqualTo("new-access-token");
            assertThat(result).containsKey("refreshToken");
            assertThat(result.get("refreshToken")).isNotNull();

            // Verify old token is revoked
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository, times(2)).save(captor.capture());
            List<RefreshToken> savedTokens = captor.getAllValues();
            assertThat(savedTokens.get(0).getRevoked()).isTrue(); // old token revoked
            assertThat(savedTokens.get(1).getRevoked()).isFalse(); // new token not revoked
        }

        @Test
        @DisplayName("should throw BusinessException when refresh token not found")
        void refresh_InvalidToken() {
            when(refreshTokenRepository.findByTokenAndRevokedFalse("invalid-token"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("invalid-token"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
                    });
        }

        @Test
        @DisplayName("should revoke and throw when refresh token is expired")
        void refresh_ExpiredToken() {
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(1L)
                    .user(testUser)
                    .token("expired-refresh-token")
                    .expiresAt(LocalDateTime.now().minusHours(1))
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByTokenAndRevokedFalse("expired-refresh-token"))
                    .thenReturn(Optional.of(refreshToken));

            assertThatThrownBy(() -> authService.refresh("expired-refresh-token"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
                    });

            // Verify the expired token was revoked
            verify(refreshTokenRepository).save(refreshToken);
            assertThat(refreshToken.getRevoked()).isTrue();
        }

        @Test
        @DisplayName("should throw when user is inactive")
        void refresh_InactiveUser() {
            User inactiveUser = TestFixtures.createUser(2L, "inactive", "USER");
            inactiveUser.setIsActive(false);

            RefreshToken refreshToken = RefreshToken.builder()
                    .id(2L)
                    .user(inactiveUser)
                    .token("inactive-user-token")
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByTokenAndRevokedFalse("inactive-user-token"))
                    .thenReturn(Optional.of(refreshToken));

            assertThatThrownBy(() -> authService.refresh("inactive-user-token"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.USER_INACTIVE);
                    });
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("should revoke all refresh tokens for the user")
        void logout_RevokesAllTokens() {
            authService.logout(1L);

            verify(refreshTokenRepository).revokeAllByUserId(1L);
        }
    }

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUser {

        @Test
        @DisplayName("should return map with correct user fields and roles without ROLE_ prefix")
        void getCurrentUser_ReturnsCorrectFields() {
            Map<String, Object> result = authService.getCurrentUser(testUserDetails);

            assertThat(result.get("id")).isEqualTo(1L);
            assertThat(result.get("username")).isEqualTo("admin");
            assertThat(result.get("email")).isEqualTo("admin@test.com");
            assertThat(result.get("fullName")).isEqualTo("admin FullName");

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) result.get("roles");
            assertThat(roles).containsExactly("ADMIN");
            assertThat(roles).noneMatch(r -> r.startsWith("ROLE_"));
        }
    }
}
