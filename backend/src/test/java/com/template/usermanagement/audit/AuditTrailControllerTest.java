package com.template.usermanagement.audit;

import com.template.usermanagement.TestFixtures;
import com.template.usermanagement.audit.dto.AuditTrailResponse;
import com.template.usermanagement.security.KeycloakJwtAuthenticationConverter;
import com.template.usermanagement.security.UserDetailsImpl;
import com.template.usermanagement.security.UserDetailsServiceImpl;
import com.template.usermanagement.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditTrailController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditTrailControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    private void setAuth(String... roles) {
        User user = TestFixtures.createUser(1L, "admin", roles);
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @Test
    void getAll_shouldReturn200() throws Exception {
        setAuth("ADMIN");
        AuditTrailResponse resp = AuditTrailResponse.builder()
                .id(1L).entityType("USER").entityId(1L).action("SUBMIT_CREATE")
                .performedBy("admin").createdAt(LocalDateTime.now()).build();
        Page<AuditTrailResponse> page = new PageImpl<>(List.of(resp));
        when(auditTrailService.getAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/audit-trail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getById_shouldReturn200() throws Exception {
        setAuth("CHECKER");
        AuditTrailResponse resp = AuditTrailResponse.builder()
                .id(1L).entityType("USER").entityId(1L).action("APPROVE_CREATE")
                .performedBy("checker").createdAt(LocalDateTime.now()).build();
        when(auditTrailService.getById(1L)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/audit-trail/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getByEntity_shouldReturn200() throws Exception {
        setAuth("VIEWER");
        Page<AuditTrailResponse> page = new PageImpl<>(List.of());
        when(auditTrailService.getByEntity(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/audit-trail/entity/USER/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
