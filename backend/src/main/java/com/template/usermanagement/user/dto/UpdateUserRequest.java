package com.template.usermanagement.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateUserRequest {

    @Email(message = "Email must be valid")
    @Size(max = 100)
    private String email;

    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @Size(max = 100)
    private String fullName;

    @Size(max = 20)
    private String phone;

    private Boolean isActive;

    private Set<String> roles;

    private Integer version;
}
