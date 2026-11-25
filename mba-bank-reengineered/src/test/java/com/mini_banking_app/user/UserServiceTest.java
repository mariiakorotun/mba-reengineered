package com.mini_banking_app.user;

import com.mini_banking_app.user.api.Role;
import com.mini_banking_app.user.api.UserOperationNotAllowedException;
import com.mini_banking_app.user.api.UserView;
import com.mini_banking_app.user.application.UserService;
import com.mini_banking_app.user.domain.UserAccount;
import com.mini_banking_app.user.infrastructure.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    private final List<UserAccount> data = new ArrayList<>();
    private UserService service;

    @BeforeEach
    void setUp() {
        data.clear();
        add(1, "user", Role.USER);
        add(2, "admin", Role.ADMIN);
        service = new UserService(repository());
    }

    private void add(long id, String name, Role role) {
        UserAccount u = new UserAccount(name, "{bcrypt}hash", role);
        try {
            Field f = UserAccount.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        data.add(u);
    }

    /** Мінімальна реалізація репозиторію через динамічний проксі — лише методи, які використовує сервіс. */
    private UserJpaRepository repository() {
        return (UserJpaRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{UserJpaRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> data.stream().filter(u -> u.getId().equals(args[0])).findFirst();
                    case "findByUsername" -> data.stream().filter(u -> u.getUsername().equals(args[0])).findFirst();
                    case "countByRoleAndEnabledTrue" -> data.stream()
                            .filter(u -> u.getRole() == args[0] && u.isEnabled()).count();
                    case "findAllByOrderByIdAsc" -> List.copyOf(data);
                    case "findAllById" -> data.stream().filter(u -> ((Collection<?>) args[0]).contains(u.getId())).toList();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    @Test
    @DisplayName("Адміністратор блокує та розблоковує клієнта")
    void togglesClient() {
        UserView blocked = service.toggleEnabled(1L, "admin");
        assertFalse(blocked.enabled());
        assertTrue(service.toggleEnabled(1L, "admin").enabled());
    }

    @Test
    @DisplayName("Не можна змінити статус власного облікового запису")
    void cannotToggleSelf() {
        assertThrows(UserOperationNotAllowedException.class, () -> service.toggleEnabled(2L, "admin"));
    }

    @Test
    @DisplayName("Не можна заблокувати останнього активного адміністратора")
    void keepsLastAdmin() {
        add(3, "admin2", Role.ADMIN);
        service.toggleEnabled(3L, "admin");                 // admin2 заблоковано — admin залишився
        assertThrows(UserOperationNotAllowedException.class, () -> service.toggleEnabled(2L, "admin2"));
    }

    @Test
    @DisplayName("Представлення користувача не містить хешу пароля")
    void viewHasNoPassword() {
        assertEquals(List.of("id", "username", "role", "enabled"),
                java.util.Arrays.stream(UserView.class.getRecordComponents()).map(c -> c.getName()).toList());
        assertEquals(2, service.allUsers().size());
        assertEquals(Map.of(1L, "user"), service.usernames(List.of(1L)));
        assertEquals(Optional.empty(), service.findByUsername("ghost"));
    }
}
