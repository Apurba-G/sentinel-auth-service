package com.ecommerce.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Authenticated User Profile Details")
public class UserProfileResponse {
    private UUID id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String phone;
    private String profileImage;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private Boolean twoFactorEnabled;
    private String provider;
    private String accountStatus;
    private OffsetDateTime lastLogin;
    private Set<String> roles;
    private OffsetDateTime createdAt;
}
