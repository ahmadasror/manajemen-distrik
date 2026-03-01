package com.template.usermanagement.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.usermanagement.TestFixtures;
import com.template.usermanagement.common.BusinessException;
import com.template.usermanagement.common.ErrorCode;
import com.template.usermanagement.security.KeycloakJwtAuthenticationConverter;
import com.template.usermanagement.security.UserDetailsImpl;
import com.template.usermanagement.security.UserDetailsServiceImpl;
import com.template.usermanagement.user.dto.RoleResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoleService roleService;

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

    private RoleResponse buildRoleResponse(Long id, String name, long userCount) {
        return RoleResponse.builder()
                .id(id)
                .name(name)
                .description(name + " role")
                .userCount(userCount)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/roles — should return list of roles")
    void getAllRoles_Returns200() throws Exception {
        setAuth("ADMIN");
        List<RoleResponse> roles = List.of(
                buildRoleResponse(1L, "ADMIN", 1),
                buildRoleResponse(2L, "MAKER", 0)
        );
        when(roleService.getAllRoles()).thenReturn(roles);

        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("ADMIN"))
                .andExpect(jsonPath("$.data[0].userCount").value(1))
                .andExpect(jsonPath("$.data[1].name").value("MAKER"));
    }

    @Test
    @DisplayName("GET /api/v1/roles/{id} — should return role detail")
    void getRoleById_Returns200() throws Exception {
        setAuth("ADMIN");
        RoleResponse roleDetail = RoleResponse.builder()
                .id(1L)
                .name("ADMIN")
                .description("ADMIN role")
                .userCount(1)
                .users(List.of(
                        RoleResponse.RoleUserSummary.builder()
                                .id(1L).username("admin").email("admin@test.com")
                                .fullName("admin FullName").isActive(true).build()
                ))
                .build();
        when(roleService.getRoleById(1L)).thenReturn(roleDetail);

        mockMvc.perform(get("/api/v1/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("ADMIN"))
                .andExpect(jsonPath("$.data.users[0].username").value("admin"));
    }

    @Test
    @DisplayName("GET /api/v1/roles/{id} — should return 400 when role not found")
    void getRoleById_NotFound_Returns400() throws Exception {
        setAuth("ADMIN");
        when(roleService.getRoleById(99L))
                .thenThrow(new BusinessException("Role not found", ErrorCode.ROLE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/roles/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.ROLE_NOT_FOUND));
    }

    @Test
    @DisplayName("POST /api/v1/roles/{roleId}/users/{userId} — should assign user and return updated role")
    void assignUser_Returns200() throws Exception {
        setAuth("ADMIN");
        RoleResponse updated = buildRoleResponse(1L, "ADMIN", 2);
        when(roleService.assignUserToRole(1L, 2L)).thenReturn(updated);

        mockMvc.perform(post("/api/v1/roles/1/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userCount").value(2));
    }

    @Test
    @DisplayName("DELETE /api/v1/roles/{roleId}/users/{userId} — should remove user and return updated role")
    void removeUser_Returns200() throws Exception {
        setAuth("ADMIN");
        RoleResponse updated = buildRoleResponse(1L, "ADMIN", 0);
        when(roleService.removeUserFromRole(1L, 2L)).thenReturn(updated);

        mockMvc.perform(delete("/api/v1/roles/1/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userCount").value(0));
    }

    @Test
    @DisplayName("POST /api/v1/roles/{roleId}/users/{userId} — should return 400 when user already has role")
    void assignUser_AlreadyHasRole_Returns400() throws Exception {
        setAuth("ADMIN");
        when(roleService.assignUserToRole(1L, 1L))
                .thenThrow(new BusinessException("User already has role ADMIN", ErrorCode.USER_ALREADY_EXISTS));

        mockMvc.perform(post("/api/v1/roles/1/users/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.USER_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("DELETE /api/v1/roles/{roleId}/users/{userId} — should return 400 when user has only one role")
    void removeUser_OnlyRole_Returns400() throws Exception {
        setAuth("ADMIN");
        when(roleService.removeUserFromRole(2L, 2L))
                .thenThrow(new BusinessException("Cannot remove the user's only role", ErrorCode.USER_NOT_IN_ROLE));

        mockMvc.perform(delete("/api/v1/roles/2/users/2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.USER_NOT_IN_ROLE));
    }
}
