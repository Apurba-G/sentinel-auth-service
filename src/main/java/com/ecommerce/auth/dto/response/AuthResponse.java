package com.ecommerce.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Authentication Success Response with JWT Token")
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
}
