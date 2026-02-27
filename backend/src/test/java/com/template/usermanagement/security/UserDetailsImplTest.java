package com.template.usermanagement.security;

import com.template.usermanagement.TestFixtures;
import com.template.usermanagement.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class UserDetailsImplTest {

    @Test
    @DisplayName("getAuthorities returns authorities with ROLE_ prefix")
    void getAuthorities_shouldReturnRolePrefixedAuthorities() {
        User user = TestFixtures.createUser(1L, "admin", "ADMIN", "USER");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        List<String> authorityStrings = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .collect(Collectors.toList());

        assertEquals(2, authorityStrings.size());
        assertTrue(authorityStrings.contains("ROLE_ADMIN"));
        assertTrue(authorityStrings.contains("ROLE_USER"));
    }

    @Test
    @DisplayName("getAuthorities returns single authority for user with one role")
    void getAuthorities_singleRole_shouldReturnOneAuthority() {
        User user = TestFixtures.createUser(1L, "viewer", "VIEWER");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        assertEquals(1, authorities.size());
        assertEquals("ROLE_VIEWER", authorities.iterator().next().getAuthority());
    }

    @Test
    @DisplayName("isAccountNonLocked returns true when user is active")
    void isAccountNonLocked_activeUser_shouldReturnTrue() {
        User user = TestFixtures.createUser(1L, "active_user", "USER");
        user.setIsActive(true);
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        assertTrue(userDetails.isAccountNonLocked());
    }

    @Test
    @DisplayName("isAccountNonLocked returns false when user is inactive")
    void isAccountNonLocked_inactiveUser_shouldReturnFalse() {
        User user = TestFixtures.createUser(1L, "inactive_user", "USER");
        user.setIsActive(false);
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        assertFalse(userDetails.isAccountNonLocked());
    }

    @Test
    @DisplayName("isEnabled returns true when user is active")
    void isEnabled_activeUser_shouldReturnTrue() {
        User user = TestFixtures.createUser(1L, "active_user", "USER");
        user.setIsActive(true);
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        assertTrue(userDetails.isEnabled());
    }

    @Test
    @DisplayName("isEnabled returns false when user is inactive")
    void isEnabled_inactiveUser_shouldReturnFalse() {
        User user = TestFixtures.createUser(1L, "inactive_user", "USER");
        user.setIsActive(false);
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        assertFalse(userDetails.isEnabled());
    }

    @Test
    @DisplayName("isAccountNonExpired always returns true")
    void isAccountNonExpired_shouldAlwaysReturnTrue() {
        User user = TestFixtures.createUser(1L, "testuser", "USER");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        assertTrue(userDetails.isAccountNonExpired());
    }

    @Test
    @DisplayName("isCredentialsNonExpired always returns true")
    void isCredentialsNonExpired_shouldAlwaysReturnTrue() {
        User user = TestFixtures.createUser(1L, "testuser", "USER");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        assertTrue(userDetails.isCredentialsNonExpired());
    }

    @Test
    @DisplayName("getters return correct values from User entity")
    void getters_shouldReturnCorrectValuesFromUser() {
        User user = TestFixtures.createUser(1L, "john", "ADMIN");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        assertEquals(1L, userDetails.getId());
        assertEquals("john", userDetails.getUsername());
        assertEquals("$2a$10$encodedPasswordHash", userDetails.getPassword());
        assertEquals("john@test.com", userDetails.getEmail());
        assertEquals("john FullName", userDetails.getFullName());
    }
}
