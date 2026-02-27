package com.template.usermanagement.security;

import com.template.usermanagement.TestFixtures;
import com.template.usermanagement.config.JwtConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    // Secret must be at least 256 bits (32 bytes) for HMAC-SHA
    private static final String TEST_SECRET = "ThisIsATestSecretKeyThatIsAtLeast256BitsLongForHMACSHA";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000L; // 1 hour

    private JwtTokenProvider jwtTokenProvider;
    private JwtConfig jwtConfig;
    private SecretKey testKey;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret(TEST_SECRET);
        jwtConfig.setAccessTokenExpiration(ACCESS_TOKEN_EXPIRATION);

        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
        jwtTokenProvider.init();

        testKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("generateAccessToken returns a non-null JWT string")
    void generateAccessToken_shouldReturnValidJwtString() {
        UserDetailsImpl userDetails = TestFixtures.createUserDetails(1L, "admin", "ADMIN");

        String token = jwtTokenProvider.generateAccessToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isBlank());
        // JWT tokens have 3 parts separated by dots
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("generateAccessToken sets correct subject (username)")
    void generateAccessToken_shouldSetCorrectSubject() {
        UserDetailsImpl userDetails = TestFixtures.createUserDetails(1L, "john", "USER");

        String token = jwtTokenProvider.generateAccessToken(userDetails);

        String subject = Jwts.parser()
                .verifyWith(testKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        assertEquals("john", subject);
    }

    @Test
    @DisplayName("generateAccessToken sets userId claim")
    void generateAccessToken_shouldSetUserIdClaim() {
        UserDetailsImpl userDetails = TestFixtures.createUserDetails(42L, "alice", "ADMIN");

        String token = jwtTokenProvider.generateAccessToken(userDetails);

        Long userId = Jwts.parser()
                .verifyWith(testKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId", Long.class);

        assertEquals(42L, userId);
    }

    @Test
    @DisplayName("generateAccessToken sets roles claim with ROLE_ prefix")
    void generateAccessToken_shouldSetRolesClaim() {
        UserDetailsImpl userDetails = TestFixtures.createUserDetails(1L, "admin", "ADMIN", "USER");

        String token = jwtTokenProvider.generateAccessToken(userDetails);

        String roles = Jwts.parser()
                .verifyWith(testKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("roles", String.class);

        assertNotNull(roles);
        assertTrue(roles.contains("ROLE_ADMIN"));
        assertTrue(roles.contains("ROLE_USER"));
    }

    @Test
    @DisplayName("generateAccessToken sets fullName claim")
    void generateAccessToken_shouldSetFullNameClaim() {
        UserDetailsImpl userDetails = TestFixtures.createUserDetails(1L, "bob", "USER");

        String token = jwtTokenProvider.generateAccessToken(userDetails);

        String fullName = Jwts.parser()
                .verifyWith(testKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("fullName", String.class);

        assertEquals("bob FullName", fullName);
    }

    @Test
    @DisplayName("generateAccessToken via Authentication delegates to UserDetailsImpl overload")
    void generateAccessToken_withAuthentication_shouldWork() {
        UserDetailsImpl userDetails = TestFixtures.createUserDetails(1L, "admin", "ADMIN");
        var authentication = TestFixtures.createAuthentication(userDetails);

        String token = jwtTokenProvider.generateAccessToken(authentication);

        assertNotNull(token);
        assertEquals("admin", jwtTokenProvider.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("getUsernameFromToken extracts username from valid token")
    void getUsernameFromToken_validToken_shouldReturnUsername() {
        UserDetailsImpl userDetails = TestFixtures.createUserDetails(1L, "testuser", "USER");
        String token = jwtTokenProvider.generateAccessToken(userDetails);

        String username = jwtTokenProvider.getUsernameFromToken(token);

        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("validateToken returns true for a valid token")
    void validateToken_validToken_shouldReturnTrue() {
        UserDetailsImpl userDetails = TestFixtures.createUserDetails(1L, "admin", "ADMIN");
        String token = jwtTokenProvider.generateAccessToken(userDetails);

        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("validateToken returns false for an expired token")
    void validateToken_expiredToken_shouldReturnFalse() {
        // Create a provider with 0ms expiration to force immediate expiry
        JwtConfig expiredConfig = new JwtConfig();
        expiredConfig.setSecret(TEST_SECRET);
        expiredConfig.setAccessTokenExpiration(0L);

        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredConfig);
        expiredProvider.init();

        UserDetailsImpl userDetails = TestFixtures.createUserDetails(1L, "admin", "ADMIN");
        String token = expiredProvider.generateAccessToken(userDetails);

        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("validateToken returns false for a tampered token")
    void validateToken_tamperedToken_shouldReturnFalse() {
        UserDetailsImpl userDetails = TestFixtures.createUserDetails(1L, "admin", "ADMIN");
        String token = jwtTokenProvider.generateAccessToken(userDetails);

        // Tamper with the token by modifying the payload
        String[] parts = token.split("\\.");
        parts[1] = parts[1] + "tampered";
        String tamperedToken = String.join(".", parts);

        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("validateToken returns false for a token signed with different key")
    void validateToken_differentKeyToken_shouldReturnFalse() {
        JwtConfig otherConfig = new JwtConfig();
        otherConfig.setSecret("ACompletelyDifferentSecretKeyThatIsAlso256BitsLongForHMAC");
        otherConfig.setAccessTokenExpiration(ACCESS_TOKEN_EXPIRATION);

        JwtTokenProvider otherProvider = new JwtTokenProvider(otherConfig);
        otherProvider.init();

        UserDetailsImpl userDetails = TestFixtures.createUserDetails(1L, "admin", "ADMIN");
        String token = otherProvider.generateAccessToken(userDetails);

        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("validateToken returns false for null token")
    void validateToken_nullToken_shouldReturnFalse() {
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    @DisplayName("validateToken returns false for empty token")
    void validateToken_emptyToken_shouldReturnFalse() {
        assertFalse(jwtTokenProvider.validateToken(""));
    }

    @Test
    @DisplayName("validateToken returns false for malformed token")
    void validateToken_malformedToken_shouldReturnFalse() {
        assertFalse(jwtTokenProvider.validateToken("not.a.valid.jwt.token"));
    }

    @Test
    @DisplayName("generateAccessToken sets issuedAt and expiration dates")
    void generateAccessToken_shouldSetDates() {
        UserDetailsImpl userDetails = TestFixtures.createUserDetails(1L, "admin", "ADMIN");
        long beforeGeneration = System.currentTimeMillis();

        String token = jwtTokenProvider.generateAccessToken(userDetails);

        long afterGeneration = System.currentTimeMillis();

        var claims = Jwts.parser()
                .verifyWith(testKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();

        assertNotNull(issuedAt);
        assertNotNull(expiration);
        assertTrue(issuedAt.getTime() >= beforeGeneration - 1000);
        assertTrue(issuedAt.getTime() <= afterGeneration + 1000);
        // Expiration should be approximately ACCESS_TOKEN_EXPIRATION ms after issuedAt
        long expirationDiff = expiration.getTime() - issuedAt.getTime();
        assertTrue(expirationDiff > 0);
        assertTrue(expirationDiff <= ACCESS_TOKEN_EXPIRATION + 1000);
    }
}
