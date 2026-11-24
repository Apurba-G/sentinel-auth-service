package com.ecommerce.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Authentication Success Response with JWT Token, Lockout & Recovery Keys")
public class AuthResponse {

    @Schema(description = "JWT Bearer Token")
    private String token;

    @Schema(description = "Refresh Token")
    private String refreshToken;

    @Schema(description = "Token Type", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "User Unique ID")
    private UUID id;

    @Schema(description = "User Email")
    private String email;

    @Schema(description = "User Username")
    private String username;

    @Schema(description = "Assigned Roles")
    private Set<String> roles;

    @Schema(description = "Flag requiring client to update temporary password immediately")
    @Builder.Default
    private Boolean mustChangePassword = false;

    @Schema(description = "12-word BIP-39 Master Recovery Phrase (Shown only once upon registration)")
    private String mnemonicRecoveryPhrase;

    @Schema(description = "Single-use Alphanumeric Backup Recovery Codes (Shown only once upon registration)")
    private List<String> backupRecoveryCodes;

    @Schema(description = "Dynamic Project-Specific Attributes")
    private Map<String, Object> dynamicAttributes;
}
