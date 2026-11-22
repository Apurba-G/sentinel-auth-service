package com.ecommerce.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User Login Request Payload with CAPTCHA Verification")
public class LoginRequest {

    @Schema(description = "User Registered Email or Username", example = "user@example.com")
    private String identifier;

    @Schema(description = "Legacy email field (fallback if identifier is omitted)", example = "user@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "User Password", example = "password123")
    private String password;

    @NotBlank(message = "Captcha ID is required")
    @Schema(description = "Captcha Challenge ID obtained from /api/v1/auth/captcha")
    private String captchaId;

    @NotBlank(message = "Captcha Answer is required")
    @Schema(description = "User entered Captcha solution", example = "7K3X9A")
    private String captchaAnswer;

    /**
     * Resolves the login handle (prefers identifier, falls back to email).
     */
    public String getEffectiveIdentifier() {
        if (identifier != null && !identifier.trim().isEmpty()) {
            return identifier.trim();
        }
        return email != null ? email.trim() : "";
    }
}
