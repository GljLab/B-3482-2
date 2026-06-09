package com.cliphub.service.impl;

import com.cliphub.entity.OperationLog;
import com.cliphub.mapper.OperationLogMapper;
import com.cliphub.security.UserPrincipal;
import com.cliphub.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public void log(UserPrincipal principal, String action, String targetType, String targetId, String detail) {
        log(principal.getId(), principal.getUsername(), action, targetType, targetId, detail);
    }

    @Override
    public void log(Long userId, String username, String action, String targetType, String targetId, String detail) {
        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(userId);
        operationLog.setUsername(username);
        operationLog.setAction(action);
        operationLog.setTargetType(targetType);
        operationLog.setTargetId(targetId);
        operationLog.setDetail(detail);
        operationLog.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(operationLog);
    }
}
