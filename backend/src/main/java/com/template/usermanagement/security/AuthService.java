package com.template.usermanagement.security;

import com.template.usermanagement.common.BusinessException;
import com.template.usermanagement.common.ErrorCode;
import com.template.usermanagement.config.JwtConfig;
import com.template.usermanagement.user.User;
import com.template.usermanagement.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtConfig jwtConfig;

    @Transactional
    public Map<String, Object> login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String accessToken = tokenProvider.generateAccessToken(userDetails);

        // Revoke old refresh tokens
        refreshTokenRepository.revokeAllByUserId(userDetails.getId());

        // Create new refresh token
        User user = userRepository.findById(userDetails.getId()).orElseThrow();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwtConfig.getRefreshTokenExpiration())))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken.getToken(),
                "user", buildUserInfo(userDetails)
        );
    }

    @Transactional
    public Map<String, Object> refresh(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenStr)
                .orElseThrow(() -> new BusinessException("Invalid refresh token", ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new BusinessException("Refresh token expired", ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
        }

        User user = refreshToken.getUser();
        if (!user.getIsActive() || user.getDeleted()) {
            throw new BusinessException("User account is inactive", ErrorCode.USER_INACTIVE);
        }

        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        String newAccessToken = tokenProvider.generateAccessToken(userDetails);

        // Rotate refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwtConfig.getRefreshTokenExpiration())))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        return Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken.getToken()
        );
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    public Map<String, Object> getCurrentUser(UserDetailsImpl userDetails) {
        return buildUserInfo(userDetails);
    }

    private Map<String, Object> buildUserInfo(UserDetailsImpl userDetails) {
        return Map.of(
                "id", userDetails.getId(),
                "username", userDetails.getUsername(),
                "email", userDetails.getEmail(),
                "fullName", userDetails.getFullName(),
                "roles", userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(a -> a.replace("ROLE_", ""))
                        .collect(Collectors.toList())
        );
    }
}
