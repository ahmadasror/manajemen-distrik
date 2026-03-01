package com.template.usermanagement.config;

import com.template.usermanagement.TestFixtures;
import com.template.usermanagement.audit.AuditTrailService;
import com.template.usermanagement.security.KeycloakJwtAuthenticationConverter;
import com.template.usermanagement.security.UserDetailsImpl;
import com.template.usermanagement.security.UserDetailsServiceImpl;
import com.template.usermanagement.user.User;
import com.template.usermanagement.user.UserService;
import com.template.usermanagement.workflow.PendingActionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private PendingActionService pendingActionService;

    @MockBean
    private AuditTrailService auditTrailService;

    @MockBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @MockBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getStats_shouldReturn200WithAllStats() throws Exception {
        User user = TestFixtures.createUser(1L, "admin", "ADMIN");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        when(userService.countTotal()).thenReturn(50L);
        when(userService.countActive()).thenReturn(42L);
        when(pendingActionService.countPending()).thenReturn(3L);
        when(auditTrailService.countTotal()).thenReturn(200L);

        mockMvc.perform(get("/api/v1/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(50))
                .andExpect(jsonPath("$.data.activeUsers").value(42))
                .andExpect(jsonPath("$.data.pendingActions").value(3))
                .andExpect(jsonPath("$.data.totalAuditEntries").value(200));
    }
}
