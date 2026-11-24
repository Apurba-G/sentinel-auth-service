package com.ecommerce.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Change or Update Password Request Payload")
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @Schema(description = "Existing password or temporary password", example = "OldPass!123")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "New password must be at least 8 characters")
    @Schema(description = "New password (Must pass RedisBloom history check)", example = "BrandNew@Secur3Pass!")
    private String newPassword;

    @NotBlank(message = "Confirmation password is required")
    @Schema(description = "Confirm new password", example = "BrandNew@Secur3Pass!")
    private String confirmPassword;
}
