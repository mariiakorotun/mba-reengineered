package com.mini_banking_app.user.api;

/** Мінімальні відомості про користувача, потрібні іншим модулям. */
public record UserRef(Long id, String username, boolean admin, boolean enabled) {
}
