package com.template.usermanagement.security;

import com.template.usermanagement.TestFixtures;
import com.template.usermanagement.user.User;
import com.template.usermanagement.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("should return UserDetailsImpl when user found")
    void loadUserByUsername_Found() {
        User user = TestFixtures.createUser(1L, "admin", "ADMIN");

        when(userRepository.findByUsernameAndDeletedFalse("admin")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("admin");

        assertThat(result).isInstanceOf(UserDetailsImpl.class);
        UserDetailsImpl userDetails = (UserDetailsImpl) result;
        assertThat(userDetails.getId()).isEqualTo(1L);
        assertThat(userDetails.getUsername()).isEqualTo("admin");
        assertThat(userDetails.getEmail()).isEqualTo("admin@test.com");
        assertThat(userDetails.getFullName()).isEqualTo("admin FullName");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("should throw UsernameNotFoundException when user not found")
    void loadUserByUsername_NotFound() {
        when(userRepository.findByUsernameAndDeletedFalse("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}
