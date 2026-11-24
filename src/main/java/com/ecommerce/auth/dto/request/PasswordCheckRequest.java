package com.ecommerce.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Password Security Check Request")
public class PasswordCheckRequest {

    @NotBlank(message = "Password candidate is required")
    @Schema(description = "Password to check against RedisBloom & security metrics", example = "Testing123!")
    private String password;
}
