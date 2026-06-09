package com.cliphub.util;

import com.cliphub.common.BusinessException;
import com.cliphub.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUserUtil {

    private CurrentUserUtil() {
    }

    public static UserPrincipal current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
        }
        return principal;
    }
}
