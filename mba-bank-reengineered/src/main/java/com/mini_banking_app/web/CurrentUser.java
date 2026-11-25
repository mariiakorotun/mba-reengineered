package com.mini_banking_app.web;

import org.springframework.security.core.Authentication;

final class CurrentUser {

    private CurrentUser() {
    }

    static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
