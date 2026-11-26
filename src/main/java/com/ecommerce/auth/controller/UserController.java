package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.request.AddressRequest;
import com.ecommerce.auth.dto.request.ChangePasswordRequest;
import com.ecommerce.auth.dto.request.ChangeUsernameRequest;
import com.ecommerce.auth.dto.response.AddressResponse;
import com.ecommerce.auth.dto.response.ApiResponse;
import com.ecommerce.auth.dto.response.UserProfileResponse;
import com.ecommerce.auth.service.AuthService;
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
@Tag(name = "User Account Management", description = "Endpoints for Profile, Address Book, and Post-Login Password/Username Updates")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping("/me")
    @Operation(summary = "Get Current User Profile", description = "Retrieves profile details and dynamic attributes for the authenticated user.")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(Authentication authentication) {
        UserProfileResponse response = userService.getCurrentUserProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", response));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change or Update Password", description = "Validates current password, checks RedisBloom for password history non-repetition, and revokes tokens forcing return to login.")
    public ResponseEntity<ApiResponse<String>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(
                "Password updated successfully. For security reasons, you have been logged out. Please log in with your new password.",
                "REDIRECT_LOGIN"
        ));
    }

    @PostMapping("/change-username")
    @Operation(summary = "Change Username", description = "Updates account username, verifies current password, and invalidates session forcing re-login.")
    public ResponseEntity<ApiResponse<String>> changeUsername(
            Authentication authentication,
            @Valid @RequestBody ChangeUsernameRequest request
    ) {
        authService.changeUsername(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(
                "Username updated successfully. Please log in with your new username.",
                "REDIRECT_LOGIN"
        ));
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
