package com.ecommerce.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User Login Request Payload")
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "User Registered Email", example = "user@ecommerce.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "User Password", example = "password123")
    private String password;
}
