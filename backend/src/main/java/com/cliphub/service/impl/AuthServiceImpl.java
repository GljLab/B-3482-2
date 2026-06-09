package com.cliphub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cliphub.common.BusinessException;
import com.cliphub.dto.*;
import com.cliphub.entity.PasswordResetToken;
import com.cliphub.entity.User;
import com.cliphub.mapper.PasswordResetTokenMapper;
import com.cliphub.mapper.UserMapper;
import com.cliphub.security.JwtTokenProvider;
import com.cliphub.security.UserPrincipal;
import com.cliphub.service.AuditLogService;
import com.cliphub.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordResetTokenMapper resetTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        boolean usernameExists = userMapper.exists(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (usernameExists) {
            throw new BusinessException("用户名已存在");
        }

        boolean emailExists = userMapper.exists(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, request.getEmail()));
        if (emailExists) {
            throw new BusinessException("邮箱已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        user.setTeamId(1001L);
        user.setDisplayName(request.getDisplayName());
        user.setBio("");
        user.setAvatarUrl("");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        String token = issueToken(user);
        auditLogService.log(user.getId(), user.getUsername(), "REGISTER", "USER", String.valueOf(user.getId()), "新用户注册");
        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .teamId(user.getTeamId())
                .token(token)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = userMapper.selectById(principal.getId());
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "账号已被禁用");
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        String token = issueToken(user);
        auditLogService.log(user.getId(), user.getUsername(), "LOGIN", "USER", String.valueOf(user.getId()), "用户登录");
        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .teamId(user.getTeamId())
                .token(token)
                .build();
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        redisTemplate.delete("session:token:" + token);
    }

    @Override
    @Transactional
    public String createResetToken(ForgotPasswordRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, request.getEmail())
                .last("LIMIT 1"));
        if (user == null) {
            return "如果邮箱存在，重置链接已发送";
        }

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(user.getId());
        resetToken.setToken(UUID.randomUUID().toString().replace("-", ""));
        resetToken.setUsed(0);
        resetToken.setCreatedAt(LocalDateTime.now());
        resetToken.setExpireAt(LocalDateTime.now().plusMinutes(30));
        resetTokenMapper.insert(resetToken);

        auditLogService.log(user.getId(), user.getUsername(), "REQUEST_RESET_PASSWORD",
                "USER", String.valueOf(user.getId()), "申请重置密码");

        return resetToken.getToken();
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokenMapper.selectOne(new LambdaQueryWrapper<PasswordResetToken>()
                .eq(PasswordResetToken::getToken, request.getToken())
                .eq(PasswordResetToken::getUsed, 0)
                .last("LIMIT 1"));

        if (resetToken == null || resetToken.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("重置令牌无效或已过期");
        }

        User user = userMapper.selectById(resetToken.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .set(User::getPassword, passwordEncoder.encode(request.getNewPassword()))
                .set(User::getUpdatedAt, LocalDateTime.now())
                .eq(User::getId, user.getId()));

        resetTokenMapper.update(null, new LambdaUpdateWrapper<PasswordResetToken>()
                .set(PasswordResetToken::getUsed, 1)
                .eq(PasswordResetToken::getId, resetToken.getId()));

        auditLogService.log(user.getId(), user.getUsername(), "RESET_PASSWORD", "USER",
                String.valueOf(user.getId()), "用户重置密码");
    }

    private String issueToken(User user) {
        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername(), user.getRole(), user.getTeamId());
        redisTemplate.opsForValue().set(
                "session:token:" + token,
                user.getId().toString(),
                jwtTokenProvider.getExpireSeconds(),
                TimeUnit.SECONDS
        );
        return token;
    }
}
