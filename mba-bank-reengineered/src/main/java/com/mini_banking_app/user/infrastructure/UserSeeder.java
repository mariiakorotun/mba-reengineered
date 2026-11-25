package com.mini_banking_app.user.infrastructure;

import com.mini_banking_app.user.api.Role;
import com.mini_banking_app.user.domain.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Початкове наповнення таблиці користувачів.
 * <ul>
 *   <li>профіль local: демо-користувачі (mba.seed.demo-users=true);</li>
 *   <li>профіль prod: лише адміністратор, пароль якого береться зі змінної середовища
 *       MBA_BOOTSTRAP_ADMIN_PASSWORD (у першій версії паролі були в коді для всіх середовищ).</li>
 * </ul>
 */
@Component
public class UserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

    private final UserJpaRepository users;
    private final PasswordEncoder encoder;
    private final boolean demoUsers;
    private final String bootstrapAdminPassword;

    public UserSeeder(UserJpaRepository users,
                      PasswordEncoder encoder,
                      @Value("${mba.seed.demo-users:false}") boolean demoUsers,
                      @Value("${mba.bootstrap.admin-password:}") String bootstrapAdminPassword) {
        this.users = users;
        this.encoder = encoder;
        this.demoUsers = demoUsers;
        this.bootstrapAdminPassword = bootstrapAdminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        if (demoUsers) {
            users.save(new UserAccount("user", encoder.encode("uSe%r456"), Role.USER));
            users.save(new UserAccount("admin", encoder.encode("@Dm1n789"), Role.ADMIN));
            log.warn("Створено демо-користувачів user/admin — лише для локального запуску");
        } else if (!bootstrapAdminPassword.isBlank()) {
            users.save(new UserAccount("admin", encoder.encode(bootstrapAdminPassword), Role.ADMIN));
            log.info("Створено початкового адміністратора з паролем зі змінної середовища");
        } else {
            log.warn("Таблиця users порожня, а пароль адміністратора не задано (MBA_BOOTSTRAP_ADMIN_PASSWORD)");
        }
    }
}
