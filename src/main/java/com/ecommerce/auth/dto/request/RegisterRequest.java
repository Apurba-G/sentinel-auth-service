package com.ecommerce.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Extensible User Registration Payload with Security Questions & Custom Project Fields")
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "User Email Address", example = "user@example.com")
    private String email;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(description = "Unique Username", example = "john_doe")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    @Schema(description = "Account Password", example = "StrongP@ssw0rd!")
    private String password;

    @Schema(description = "First Name", example = "John")
    private String firstName;

    @Schema(description = "Last Name", example = "Doe")
    private String lastName;

    @Schema(description = "Phone Number", example = "+19876543210")
    private String phone;

    @Schema(description = "Requested Roles (Default: ROLE_CUSTOMER)", example = "[\"ROLE_CUSTOMER\"]")
    private Set<String> roles;

    @Schema(description = "Security Question Answers Map: Question UUID -> Answer Text")
    private Map<UUID, String> securityAnswers;

    @Schema(description = "Dynamic Project-Specific Fields (JSON Key-Value pairs without DB schema changes)")
    private Map<String, Object> dynamicAttributes;
}
