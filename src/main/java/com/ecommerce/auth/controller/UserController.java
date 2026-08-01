package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.request.AddressRequest;
import com.ecommerce.auth.dto.response.AddressResponse;
import com.ecommerce.auth.dto.response.ApiResponse;
import com.ecommerce.auth.dto.response.UserProfileResponse;
import com.ecommerce.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Module", description = "Endpoints for User Profile and Delivery Address Book Management")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get Current User Profile", description = "Retrieves profile details for the authenticated user based on JWT Bearer token.")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(Authentication authentication) {
        UserProfileResponse response = userService.getCurrentUserProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", response));
    }

    @PostMapping("/addresses")
    @Operation(summary = "Add Delivery Address", description = "Adds a new delivery address to the authenticated user's address book.")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            Authentication authentication,
            @Valid @RequestBody AddressRequest request
    ) {
        AddressResponse response = userService.addAddress(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address added successfully", response));
    }

    @GetMapping("/addresses")
    @Operation(summary = "Get User Delivery Addresses", description = "Retrieves all saved delivery addresses for the authenticated user.")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getUserAddresses(Authentication authentication) {
        List<AddressResponse> response = userService.getUserAddresses(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Addresses retrieved successfully", response));
    }
}
