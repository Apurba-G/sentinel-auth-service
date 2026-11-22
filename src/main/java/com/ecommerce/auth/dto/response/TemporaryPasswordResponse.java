package com.ecommerce.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Temporary Password Response")
public class TemporaryPasswordResponse {

    @Schema(description = "Generated high-entropy temporary password (Valid for 24 hours)")
    private String temporaryPassword;

    @Schema(description = "Expiration timestamp")
    private OffsetDateTime expiresAt;

    @Schema(description = "Instruction message")
    private String message;
}
