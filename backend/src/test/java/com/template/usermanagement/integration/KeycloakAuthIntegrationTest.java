package com.template.usermanagement.integration;

import com.template.usermanagement.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Keycloak JWT authentication flow.
 *
 * Strategy: mock the JwtDecoder bean so the test context never contacts a real Keycloak
 * instance, while the real KeycloakJwtAuthenticationConverter still runs against the H2
 * test database — giving us genuine coverage of user-lookup and auto-creation logic.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class KeycloakAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private UserRepository userRepository;

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Jwt buildJwt(String username, String email, String name) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("preferred_username", username)
                .claim("email", email)
                .claim("name", name)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Unauthenticated request returns 401")
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Valid Keycloak JWT grants access to a protected endpoint (200)")
    void validJwt_returnsOk() throws Exception {
        Jwt jwt = buildJwt("admin", "admin@template.com", "System Administrator");
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    @DisplayName("New Keycloak user is auto-created with VIEWER role on first login")
    void newKeycloakUser_autoCreatedWithViewerRole() throws Exception {
        String newUsername = "brand_new_kc_user";
        Jwt jwt = buildJwt(newUsername, "brandnew@keycloak.example", "Brand New User");
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);

        assertThat(userRepository.findByUsernameAndDeletedFalse(newUsername)).isEmpty();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(newUsername));

        assertThat(userRepository.findByUsernameAndDeletedFalse(newUsername))
                .isPresent()
                .get()
                .satisfies(user -> {
                    assertThat(user.getEmail()).isEqualTo("brandnew@keycloak.example");
                    assertThat(user.getFullName()).isEqualTo("Brand New User");
                    assertThat(user.getRoles())
                            .extracting("name")
                            .containsExactly("VIEWER");
                });
    }

    @Test
    @DisplayName("Authenticated admin's roles are returned in /me response")
    void authenticatedAdmin_rolesReturnedInMeResponse() throws Exception {
        // "admin" is seeded by V3__seed_admin_user.sql with the ADMIN role
        Jwt jwt = buildJwt("admin", "admin@template.com", "System Administrator");
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"));
    }
}
