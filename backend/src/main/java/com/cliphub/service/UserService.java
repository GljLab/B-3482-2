package com.cliphub.service;

import com.cliphub.dto.UpdateProfileRequest;
import com.cliphub.entity.User;
import com.cliphub.security.UserPrincipal;

import java.util.List;

public interface UserService {

    User getProfile(UserPrincipal principal);

    User updateProfile(UserPrincipal principal, UpdateProfileRequest request);

    List<User> listUsers();

    User updateRole(Long userId, String role, UserPrincipal operator);
}
