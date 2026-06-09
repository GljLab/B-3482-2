package com.cliphub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cliphub.common.BusinessException;
import com.cliphub.dto.UpdateProfileRequest;
import com.cliphub.entity.User;
import com.cliphub.mapper.UserMapper;
import com.cliphub.security.UserPrincipal;
import com.cliphub.service.AuditLogService;
import com.cliphub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "VIP", "ADMIN");

    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    @Override
    public User getProfile(UserPrincipal principal) {
        User user = userMapper.selectById(principal.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional
    public User updateProfile(UserPrincipal principal, UpdateProfileRequest request) {
        User user = userMapper.selectById(principal.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setDisplayName(request.getDisplayName());
        user.setBio(request.getBio());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        user.setPassword(null);

        auditLogService.log(principal, "UPDATE_PROFILE", "USER", String.valueOf(user.getId()), "更新个人资料");
        return user;
    }

    @Override
    public List<User> listUsers() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt));
        users.forEach(user -> user.setPassword(null));
        return users;
    }

    @Override
    @Transactional
    public User updateRole(Long userId, String role, UserPrincipal operator) {
        if (!ALLOWED_ROLES.contains(role)) {
            throw new BusinessException("仅支持 USER/VIP/ADMIN");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        user.setPassword(null);

        auditLogService.log(operator, "UPDATE_ROLE", "USER", String.valueOf(userId), "更新角色为 " + role);
        return user;
    }
}
