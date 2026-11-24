package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.request.*;
import com.ecommerce.auth.dto.response.*;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);

    TemporaryPasswordResponse forgotPassword(ForgotPasswordRequest request);

    void changePassword(String currentUserEmail, ChangePasswordRequest request);

    void changeUsername(String currentUserEmail, ChangeUsernameRequest request);

    PasswordCheckResponse checkPassword(String currentUserEmail, PasswordCheckRequest request);

    boolean unlockAccount(UnlockAccountRequest request);

    String triggerAccountUnlockOtp(String identifier, String challengeType, String ip, String device);
}
