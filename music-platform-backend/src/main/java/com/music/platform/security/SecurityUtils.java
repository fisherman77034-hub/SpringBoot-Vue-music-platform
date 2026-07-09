package com.music.platform.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class SecurityUtils {
    public static JwtUser currentJwtUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtUser ju)) {
            throw new IllegalArgumentException("未登录");
        }
        return ju;
    }

    public static Optional<JwtUser> optionalJwtUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtUser ju) {
            return Optional.of(ju);
        }
        return Optional.empty();
    }
}

