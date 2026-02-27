package com.template.usermanagement.workflow;

import com.template.usermanagement.TestFixtures;
import com.template.usermanagement.security.JwtAuthenticationFilter;
import com.template.usermanagement.security.JwtTokenProvider;
import com.template.usermanagement.security.UserDetailsImpl;
import com.template.usermanagement.security.UserDetailsServiceImpl;
import com.template.usermanagement.user.User;
import com.template.usermanagement.workflow.dto.PendingActionResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PendingActionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PendingActionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PendingActionService pendingActionService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    private void setAuth(Long id, String username, String... roles) {
        User user = TestFixtures.createUser(id, username, roles);
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @Test
    void getAll_shouldReturn200() throws Exception {
        setAuth(1L, "checker", "CHECKER");
        PendingActionResponse resp = PendingActionResponse.builder()
                .id(1L).entityType("USER").actionType("CREATE").status("PENDING")
                .makerId(2L).makerUsername("maker")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .payload(Map.of()).build();
        Page<PendingActionResponse> page = new PageImpl<>(List.of(resp));
        when(pendingActionService.getAll(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/pending-actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getById_shouldReturn200() throws Exception {
        setAuth(1L, "checker", "CHECKER");
        PendingActionResponse resp = PendingActionResponse.builder()
                .id(1L).entityType("USER").actionType("CREATE").status("PENDING")
                .makerId(2L).makerUsername("maker")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .payload(Map.of()).build();
        when(pendingActionService.getById(1L)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/pending-actions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void approve_shouldReturn200() throws Exception {
        setAuth(2L, "checker", "CHECKER");
        PendingActionResponse resp = PendingActionResponse.builder()
                .id(1L).entityType("USER").actionType("CREATE").status("APPROVED")
                .makerId(1L).makerUsername("maker").checkerId(2L).checkerUsername("checker")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .payload(Map.of()).build();
        when(pendingActionService.approve(eq(1L), eq(2L), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/pending-actions/1/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remarks\":\"Looks good\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void reject_shouldReturn200() throws Exception {
        setAuth(2L, "checker", "CHECKER");
        PendingActionResponse resp = PendingActionResponse.builder()
                .id(1L).entityType("USER").actionType("CREATE").status("REJECTED")
                .makerId(1L).makerUsername("maker").checkerId(2L).checkerUsername("checker")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .payload(Map.of()).build();
        when(pendingActionService.reject(eq(1L), eq(2L), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/pending-actions/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remarks\":\"Not valid\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void cancel_shouldReturn200() throws Exception {
        setAuth(1L, "maker", "MAKER");
        PendingActionResponse resp = PendingActionResponse.builder()
                .id(1L).entityType("USER").actionType("CREATE").status("CANCELLED")
                .makerId(1L).makerUsername("maker")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .payload(Map.of()).build();
        when(pendingActionService.cancel(1L, 1L)).thenReturn(resp);

        mockMvc.perform(post("/api/v1/pending-actions/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
