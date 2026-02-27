package com.template.usermanagement.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User savedUser;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // Roles are seeded via V2 migration; fetch them
        adminRole = roleRepository.findByName("ADMIN").orElseThrow();

        User user = User.builder()
                .username("testuser")
                .email("testuser@test.com")
                .passwordHash("$2a$10$hashedPassword")
                .fullName("Test User")
                .phone("1234567890")
                .isActive(true)
                .deleted(false)
                .version(0)
                .roles(Set.of(adminRole))
                .build();
        user.setCreatedBy("system");
        user.setUpdatedBy("system");
        savedUser = userRepository.save(user);
    }

    @Test
    void findByUsernameAndDeletedFalse_shouldReturnUser() {
        Optional<User> result = userRepository.findByUsernameAndDeletedFalse("testuser");
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void findByUsernameAndDeletedFalse_shouldNotReturnDeletedUser() {
        savedUser.setDeleted(true);
        userRepository.save(savedUser);

        Optional<User> result = userRepository.findByUsernameAndDeletedFalse("testuser");
        assertThat(result).isEmpty();
    }

    @Test
    void findByIdAndDeletedFalse_shouldReturnUser() {
        Optional<User> result = userRepository.findByIdAndDeletedFalse(savedUser.getId());
        assertThat(result).isPresent();
    }

    @Test
    void findByIdAndDeletedFalse_shouldNotReturnDeletedUser() {
        savedUser.setDeleted(true);
        userRepository.save(savedUser);

        Optional<User> result = userRepository.findByIdAndDeletedFalse(savedUser.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void existsByUsernameAndDeletedFalse_shouldReturnTrue() {
        assertThat(userRepository.existsByUsernameAndDeletedFalse("testuser")).isTrue();
    }

    @Test
    void existsByUsernameAndDeletedFalse_shouldReturnFalseForDeleted() {
        savedUser.setDeleted(true);
        userRepository.save(savedUser);

        assertThat(userRepository.existsByUsernameAndDeletedFalse("testuser")).isFalse();
    }

    @Test
    void existsByUsernameAndDeletedFalse_shouldReturnFalseForNonExistent() {
        assertThat(userRepository.existsByUsernameAndDeletedFalse("nonexistent")).isFalse();
    }

    @Test
    void existsByEmailAndDeletedFalse_shouldReturnTrue() {
        assertThat(userRepository.existsByEmailAndDeletedFalse("testuser@test.com")).isTrue();
    }

    @Test
    void existsByEmailAndDeletedFalse_shouldReturnFalseForDeleted() {
        savedUser.setDeleted(true);
        userRepository.save(savedUser);

        assertThat(userRepository.existsByEmailAndDeletedFalse("testuser@test.com")).isFalse();
    }

    @Test
    void findAllActive_shouldReturnActiveUsers() {
        Page<User> result = userRepository.findAllActive(null, PageRequest.of(0, 10));
        // Admin user from seed + testuser
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void findAllActive_shouldSearchByUsername() {
        Page<User> result = userRepository.findAllActive("testuser", PageRequest.of(0, 10));
        assertThat(result.getContent()).anyMatch(u -> u.getUsername().equals("testuser"));
    }

    @Test
    void findAllActive_shouldSearchByFullName() {
        Page<User> result = userRepository.findAllActive("Test User", PageRequest.of(0, 10));
        assertThat(result.getContent()).anyMatch(u -> u.getFullName().equals("Test User"));
    }

    @Test
    void findAllActive_shouldSearchByEmail() {
        Page<User> result = userRepository.findAllActive("testuser@test.com", PageRequest.of(0, 10));
        assertThat(result.getContent()).anyMatch(u -> u.getEmail().equals("testuser@test.com"));
    }

    @Test
    void findAllActive_shouldNotReturnDeletedUsers() {
        savedUser.setDeleted(true);
        userRepository.save(savedUser);

        Page<User> result = userRepository.findAllActive("testuser", PageRequest.of(0, 10));
        assertThat(result.getContent()).noneMatch(u -> u.getUsername().equals("testuser"));
    }

    @Test
    void countByDeletedFalse_shouldCountActiveUsers() {
        long count = userRepository.countByDeletedFalse();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void countByIsActiveTrueAndDeletedFalse_shouldCountActiveEnabledUsers() {
        long count = userRepository.countByIsActiveTrueAndDeletedFalse();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void countByIsActiveTrueAndDeletedFalse_shouldNotCountInactiveUsers() {
        savedUser.setIsActive(false);
        userRepository.save(savedUser);

        long beforeCount = userRepository.countByIsActiveTrueAndDeletedFalse();

        savedUser.setIsActive(true);
        userRepository.save(savedUser);

        long afterCount = userRepository.countByIsActiveTrueAndDeletedFalse();
        assertThat(afterCount).isGreaterThan(beforeCount);
    }
}
