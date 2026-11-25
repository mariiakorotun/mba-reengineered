package com.mini_banking_app.user.api;

/**
 * Представлення користувача для адмін-панелі.
 * На відміну від першої версії, у модель сторінки не потрапляє JPA-сутність із хешем пароля.
 */
public record UserView(Long id, String username, Role role, boolean enabled) {

    public boolean admin() {
        return role == Role.ADMIN;
    }
}
