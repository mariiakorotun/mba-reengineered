package com.mini_banking_app.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Налаштування сповіщень. Адреса отримувача береться з конфігурації,
 * а не з прихованого поля форми, як у першій версії (там її міг підмінити будь-хто).
 */
@ConfigurationProperties(prefix = "mba.notifications")
public record NotificationProperties(String adminEmail, String from) {

    public boolean enabled() {
        return adminEmail != null && !adminEmail.isBlank();
    }
}
