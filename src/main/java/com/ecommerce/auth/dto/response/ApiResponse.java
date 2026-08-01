package com.ecommerce.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Standard API Wrapper Response")
public class ApiResponse<T> {
    @Schema(description = "Success Status Flag", example = "true")
    private boolean success;

    @Schema(description = "Response Message", example = "Operation completed successfully")
    private String message;

    @Schema(description = "Response Payload Data")
    private T data;

    @Builder.Default
    @Schema(description = "Response Timestamp")
    private OffsetDateTime timestamp = OffsetDateTime.now();

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(OffsetDateTime.now())
                .build();
    }
}
