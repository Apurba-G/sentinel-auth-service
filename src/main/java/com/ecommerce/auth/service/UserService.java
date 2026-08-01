package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.request.AddressRequest;
import com.ecommerce.auth.dto.response.AddressResponse;
import com.ecommerce.auth.dto.response.UserProfileResponse;

import java.util.List;

public interface UserService {
    UserProfileResponse getCurrentUserProfile(String userEmail);
    AddressResponse addAddress(String userEmail, AddressRequest request);
    List<AddressResponse> getUserAddresses(String userEmail);
}
