package com.mini_banking_app.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Спільні атрибути моделі для всіх сторінок.
 * У першій версії однаковий код визначення ролі дублювався в чотирьох контролерах.
 */
@ControllerAdvice
public class WebModelAdvice {

    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication authentication) {
        return CurrentUser.isAdmin(authentication);
    }

    @ModelAttribute("username")
    public String username(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }
}
