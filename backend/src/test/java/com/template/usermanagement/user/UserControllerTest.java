package com.template.usermanagement.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.usermanagement.TestFixtures;
import com.template.usermanagement.security.JwtAuthenticationFilter;
import com.template.usermanagement.security.JwtTokenProvider;
import com.template.usermanagement.security.UserDetailsImpl;
import com.template.usermanagement.security.UserDetailsServiceImpl;
import com.template.usermanagement.user.dto.UserResponse;
import com.template.usermanagement.workflow.PendingAction;
import com.template.usermanagement.workflow.dto.PendingActionResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

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

    private void setAuth(String... roles) {
        User user = TestFixtures.createUser(1L, "testuser", roles);
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @Test
    void getAll_shouldReturn200() throws Exception {
        setAuth("ADMIN");
        UserResponse ur = UserResponse.builder().id(1L).username("admin").email("a@t.com").fullName("Admin").build();
        Page<UserResponse> page = new PageImpl<>(List.of(ur));
        when(userService.getAllUsers(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].username").value("admin"));
    }

    @Test
    void getById_shouldReturn200() throws Exception {
        setAuth("VIEWER");
        UserResponse ur = UserResponse.builder().id(1L).username("admin").email("a@t.com").fullName("Admin").build();
        when(userService.getUserById(1L)).thenReturn(ur);

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void create_withValidBody_shouldReturn201() throws Exception {
        setAuth("MAKER");
        User maker = TestFixtures.createUser(1L, "maker", "MAKER");
        PendingAction pa = TestFixtures.createPendingAction(1L, "USER", null, "CREATE", "PENDING", maker, null);
        when(userService.createUser(any(), eq(1L))).thenReturn(pa);

        String json = """
                {"username":"newuser","email":"new@test.com","password":"pass123","fullName":"New User","roles":["MAKER"]}
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void create_withInvalidBody_shouldReturn400() throws Exception {
        setAuth("MAKER");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_shouldReturn200() throws Exception {
        setAuth("ADMIN");
        User maker = TestFixtures.createUser(1L, "admin", "ADMIN");
        PendingAction pa = TestFixtures.createPendingAction(1L, "USER", 5L, "UPDATE", "PENDING", maker, null);
        when(userService.updateUser(eq(5L), any(), eq(1L))).thenReturn(pa);

        mockMvc.perform(put("/api/v1/users/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"updated@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void delete_shouldReturn200() throws Exception {
        setAuth("MAKER");
        User maker = TestFixtures.createUser(1L, "maker", "MAKER");
        PendingAction pa = TestFixtures.createPendingAction(1L, "USER", 5L, "DELETE", "PENDING", maker, null);
        when(userService.deleteUser(eq(5L), eq(1L))).thenReturn(pa);

        mockMvc.perform(delete("/api/v1/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
