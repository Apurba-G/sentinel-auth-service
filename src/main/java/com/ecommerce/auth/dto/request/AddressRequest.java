package com.ecommerce.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Customer Delivery Address Request Payload")
public class AddressRequest {

    @Schema(description = "Address Type (HOME, WORK, OTHER)", example = "HOME")
    @Builder.Default
    private String addressType = "HOME";

    @NotBlank(message = "Full name is required")
    @Schema(description = "Recipient Full Name", example = "John Doe")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Schema(description = "Recipient Phone Number", example = "+919876543210")
    private String phone;

    @NotBlank(message = "Address line 1 is required")
    @Schema(description = "Address Line 1", example = "123 Main Street, Apt 4B")
    private String addressLine1;

    @Schema(description = "Address Line 2", example = "Near City Park")
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Schema(description = "City", example = "Mumbai")
    private String city;

    @NotBlank(message = "State is required")
    @Schema(description = "State / Province", example = "Maharashtra")
    private String state;

    @Schema(description = "Country", example = "India")
    @Builder.Default
    private String country = "India";

    @Schema(description = "Postal / ZIP Code", example = "400001")
    private String postalCode;

    @Schema(description = "Is Default Address Flag", example = "true")
    @Builder.Default
    private Boolean isDefault = false;

    private Double latitude;
    private Double longitude;
}
