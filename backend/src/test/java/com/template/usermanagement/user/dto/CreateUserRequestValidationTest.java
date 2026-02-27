package com.template.usermanagement.user.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CreateUserRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private CreateUserRequest createValidRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("john_doe");
        request.setEmail("john@example.com");
        request.setPassword("secret123");
        request.setFullName("John Doe");
        request.setRoles(Set.of("ADMIN"));
        return request;
    }

    @Test
    @DisplayName("Valid request has no violations")
    void validRequest_shouldHaveNoViolations() {
        CreateUserRequest request = createValidRequest();

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    // --- Username validations ---

    @Test
    @DisplayName("Blank username produces violation")
    void blankUsername_shouldHaveViolation() {
        CreateUserRequest request = createValidRequest();
        request.setUsername("");

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    @DisplayName("Null username produces violation")
    void nullUsername_shouldHaveViolation() {
        CreateUserRequest request = createValidRequest();
        request.setUsername(null);

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    @DisplayName("Username shorter than 3 characters produces violation")
    void shortUsername_shouldHaveViolation() {
        CreateUserRequest request = createValidRequest();
        request.setUsername("ab");

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    @DisplayName("Username longer than 50 characters produces violation")
    void longUsername_shouldHaveViolation() {
        CreateUserRequest request = createValidRequest();
        request.setUsername("a".repeat(51));

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    @DisplayName("Username with exactly 3 characters is valid")
    void usernameMinLength_shouldBeValid() {
        CreateUserRequest request = createValidRequest();
        request.setUsername("abc");

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Username with exactly 50 characters is valid")
    void usernameMaxLength_shouldBeValid() {
        CreateUserRequest request = createValidRequest();
        request.setUsername("a".repeat(50));

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Username with special characters (not in allowed set) produces violation")
    void usernameWithSpecialChars_shouldHaveViolation() {
        CreateUserRequest request = createValidRequest();
        request.setUsername("john doe!");

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    @DisplayName("Username with @ symbol produces violation")
    void usernameWithAtSymbol_shouldHaveViolation() {
        CreateUserRequest request = createValidRequest();
        request.setUsername("john@doe");

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    @DisplayName("Username with allowed special chars (dot, hyphen, underscore) is valid")
    void usernameWithAllowedSpecialChars_shouldBeValid() {
        CreateUserRequest request = createValidRequest();
        request.setUsername("john.doe-name_123");

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    // --- Email validations ---

    @Test
    @DisplayName("Blank email produces violation")
    void blankEmail_shouldHaveViolation() {
        CreateUserRequest request = createValidRequest();
        request.setEmail("");

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    @DisplayName("Invalid email format produces violation")
    void invalidEmail_shouldHaveViolation() {
        CreateUserRequest request = createValidRequest();
        request.setEmail("not-an-email");

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    @DisplayName("Email longer than 100 characters produces violation")
    void longEmail_shouldHaveViolation() {
        CreateUserRequest request = createValidRequest();
        request.setEmail("a".repeat(90) + "@example.com");

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    // --- Password validations ---

    @Test
    @DisplayName("Blank password produces violation")
    void blankPassword_shouldHaveViolation() {
        CreateUserRequest request = createValidRequest();
        request.setPassword("");

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    @DisplayName("Password shorter than 6 characters produces violation")
    void shortPassword_shouldHaveViolation() {
        CreateUserRequest request = createValidRequest();
        request.setPassword("12345");

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    @DisplayName("Password with exactly 6 characters is valid")
    void passwordMinLength_shouldBeValid() {
        CreateUserRequest request = createValidRequest();
        request.setPassword("123456");

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Password longer than 100 characters produces violation")
    void longPassword_shouldHaveViolation() {
        CreateUserRequest request = createValidRequest();
        request.setPassword("a".repeat(101));

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    // --- FullName validations ---

    @Test
    @DisplayName("Blank fullName produces violation")
    void blankFullName_shouldHaveViolation() {
        CreateUserRequest request = createValidRequest();
        request.setFullName("");

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fullName")));
    }

    @Test
    @DisplayName("Null fullName produces violation")
    void nullFullName_shouldHaveViolation() {
        CreateUserRequest request = createValidRequest();
        request.setFullName(null);

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fullName")));
    }

    @Test
    @DisplayName("FullName longer than 100 characters produces violation")
    void longFullName_shouldHaveViolation() {
        CreateUserRequest request = createValidRequest();
        request.setFullName("A".repeat(101));

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fullName")));
    }

    // --- Roles validations ---

    @Test
    @DisplayName("Empty roles set produces violation")
    void emptyRoles_shouldHaveViolation() {
        CreateUserRequest request = createValidRequest();
        request.setRoles(Set.of());

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("roles")));
    }

    @Test
    @DisplayName("Null roles produces violation")
    void nullRoles_shouldHaveViolation() {
        CreateUserRequest request = createValidRequest();
        request.setRoles(null);

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("roles")));
    }

    @Test
    @DisplayName("Multiple roles are valid")
    void multipleRoles_shouldBeValid() {
        CreateUserRequest request = createValidRequest();
        request.setRoles(Set.of("ADMIN", "USER", "VIEWER"));

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}
