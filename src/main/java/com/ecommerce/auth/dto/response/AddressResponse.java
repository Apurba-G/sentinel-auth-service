package com.ecommerce.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Customer Delivery Address Response")
public class AddressResponse {
    private UUID id;
    private UUID userId;
    private String addressType;
    private String fullName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private Boolean isDefault;
    private Double latitude;
    private Double longitude;
    private OffsetDateTime createdAt;
}
