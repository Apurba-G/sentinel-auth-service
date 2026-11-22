package com.ecommerce.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Forgot Password & Temporary Password Generation Request")
public class ForgotPasswordRequest {

    @NotBlank(message = "Identifier (Email or Phone) is required")
    @Schema(description = "Registered Email or Phone Number", example = "user@example.com")
    private String identifier;

    @NotBlank(message = "Recovery method is required (RECOVERY_PHRASE, BACKUP_CODE, or SECURITY_QUESTION)")
    @Schema(description = "Recovery Method Chosen", example = "RECOVERY_PHRASE")
    private String recoveryMethod; // 'RECOVERY_PHRASE', 'BACKUP_CODE', 'SECURITY_QUESTION'

    @Schema(description = "Mnemonic 12-word paragraph or backup code")
    private String recoveryValue;

    @Schema(description = "Security Question ID (if method is SECURITY_QUESTION)")
    private UUID questionId;

    @Schema(description = "Security Question Answer (if method is SECURITY_QUESTION)")
    private String securityAnswer;

    @NotBlank(message = "Captcha ID is required")
    private String captchaId;

    @NotBlank(message = "Captcha Answer is required")
    private String captchaAnswer;
}
