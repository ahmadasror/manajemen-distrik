package com.template.usermanagement.security;

import com.template.usermanagement.user.Role;
import com.template.usermanagement.user.RoleRepository;
import com.template.usermanagement.user.User;
import com.template.usermanagement.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        String rawUsername = jwt.getClaimAsString("preferred_username");
        final String username = (rawUsername != null) ? rawUsername : jwt.getSubject();

        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .orElseGet(() -> autoCreateUser(username, jwt));

        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        return new UsernamePasswordAuthenticationToken(userDetails, jwt, userDetails.getAuthorities());
    }

    private User autoCreateUser(String username, Jwt jwt) {
        log.info("Auto-creating app DB user for Keycloak user: {}", username);

        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");

        String fullName = name != null ? name
                : (firstName != null && lastName != null) ? firstName + " " + lastName
                : username;

        Role viewerRole = roleRepository.findByName("VIEWER")
                .orElseThrow(() -> new RuntimeException("VIEWER role not found in database"));

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email != null ? email : username + "@keycloak.local");
        newUser.setFullName(fullName);
        newUser.setPasswordHash("");
        newUser.setIsActive(true);
        newUser.setDeleted(false);
        newUser.setVersion(0);
        HashSet<Role> roles = new HashSet<>();
        roles.add(viewerRole);
        newUser.setRoles(roles);

        return userRepository.save(newUser);
    }
}
