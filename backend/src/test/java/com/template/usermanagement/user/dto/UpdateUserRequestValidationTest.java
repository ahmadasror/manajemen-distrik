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

class UpdateUserRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("All-null request is valid (all fields are optional)")
    void allNullFields_shouldBeValid() {
        UpdateUserRequest request = new UpdateUserRequest();

        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Valid email passes validation")
    void validEmail_shouldBeValid() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("user@example.com");

        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Invalid email produces violation")
    void invalidEmail_shouldHaveViolation() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("not-an-email");

        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    @DisplayName("Email longer than 100 characters produces violation")
    void longEmail_shouldHaveViolation() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("a".repeat(90) + "@example.com");

        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    @DisplayName("Valid password passes validation")
    void validPassword_shouldBeValid() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setPassword("newPassword123");

        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Password shorter than 6 characters produces violation")
    void shortPassword_shouldHaveViolation() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setPassword("12345");

        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    @DisplayName("Password with exactly 6 characters is valid")
    void passwordMinLength_shouldBeValid() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setPassword("123456");

        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Password longer than 100 characters produces violation")
    void longPassword_shouldHaveViolation() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setPassword("a".repeat(101));

        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    @DisplayName("Valid fullName passes validation")
    void validFullName_shouldBeValid() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("John Doe");

        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("FullName longer than 100 characters produces violation")
    void longFullName_shouldHaveViolation() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("A".repeat(101));

        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fullName")));
    }

    @Test
    @DisplayName("All valid fields together pass validation")
    void allValidFields_shouldBeValid() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("updated@example.com");
        request.setPassword("newPassword");
        request.setFullName("Updated Name");
        request.setPhone("9876543210");
        request.setIsActive(true);
        request.setRoles(Set.of("ADMIN"));
        request.setVersion(1);

        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Phone longer than 20 characters produces violation")
    void longPhone_shouldHaveViolation() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setPhone("1".repeat(21));

        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("phone")));
    }
}
