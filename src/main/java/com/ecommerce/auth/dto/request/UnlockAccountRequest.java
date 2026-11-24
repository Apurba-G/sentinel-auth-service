package com.ecommerce.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Account Unlock / Security Verification Challenge Request")
public class UnlockAccountRequest {

    @NotBlank(message = "Identifier (Email or Username) is required")
    private String identifier;

    @Schema(description = "Challenge Type (EMAIL_OTP, PHONE_SMS_OTP, or SECURITY_QUESTION)")
    private String challengeType;

    @Schema(description = "OTP code received via Email or Phone")
    private String otpCode;

    @Schema(description = "Security Question ID if answering question")
    private UUID questionId;

    @Schema(description = "Security Question Answer")
    private String securityAnswer;
}
