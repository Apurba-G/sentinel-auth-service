package com.ecommerce.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Password Security Analysis Response")
public class PasswordCheckResponse {

    @Schema(description = "Overall security score (0 to 100)")
    private int score;

    @Schema(description = "Strength Rating: WEAK, MODERATE, STRONG, EXCELLENT")
    private String rating;

    @Schema(description = "Whether password was found in the RedisBloom breached dictionary")
    private boolean foundInBreachedList;

    @Schema(description = "Whether password was repeated in user's historical Bloom filter")
    private boolean repeatedInHistory;

    @Schema(description = "Security improvement suggestions")
    private List<String> feedback;
}
