package com.cliphub.service;

import com.cliphub.security.UserPrincipal;

public interface AuditLogService {

    void log(UserPrincipal principal, String action, String targetType, String targetId, String detail);

    void log(Long userId, String username, String action, String targetType, String targetId, String detail);
}
