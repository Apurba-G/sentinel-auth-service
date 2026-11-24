package com.ecommerce.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Change Username Request Payload")
public class ChangeUsernameRequest {

    @NotBlank(message = "New username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(description = "New desired username", example = "john_shield")
    private String newUsername;

    @NotBlank(message = "Current password is required to verify identity")
    @Schema(description = "Account password verification", example = "P@ssword123")
    private String currentPassword;
}
