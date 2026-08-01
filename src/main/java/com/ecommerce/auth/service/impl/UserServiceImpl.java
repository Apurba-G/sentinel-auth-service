package com.ecommerce.auth.service.impl;

import com.ecommerce.auth.dto.request.AddressRequest;
import com.ecommerce.auth.dto.response.AddressResponse;
import com.ecommerce.auth.dto.response.UserProfileResponse;
import com.ecommerce.auth.entity.Address;
import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.exception.ResourceNotFoundException;
import com.ecommerce.auth.repository.AddressRepository;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .profileImage(user.getProfileImage())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .provider(user.getProvider())
                .accountStatus(user.getAccountStatus())
                .lastLogin(user.getLastLogin())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public AddressResponse addAddress(String userEmail, AddressRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.findByUserAndIsDefaultTrue(user).ifPresent(addr -> {
                addr.setIsDefault(false);
                addressRepository.save(addr);
            });
        }

        Address address = Address.builder()
                .user(user)
                .addressType(request.getAddressType() != null ? request.getAddressType() : "HOME")
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry() != null ? request.getCountry() : "India")
                .postalCode(request.getPostalCode())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        Address saved = addressRepository.save(address);
        return mapToAddressResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        return addressRepository.findByUser(user).stream()
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    private AddressResponse mapToAddressResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .userId(address.getUser().getId())
                .addressType(address.getAddressType())
                .fullName(address.getFullName())
                .phone(address.getPhone())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .country(address.getCountry())
                .postalCode(address.getPostalCode())
                .isDefault(address.getIsDefault())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .createdAt(address.getCreatedAt())
                .build();
    }
}
