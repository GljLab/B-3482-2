package com.cliphub.service;

import com.cliphub.dto.*;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void logout(String token);

    String createResetToken(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
