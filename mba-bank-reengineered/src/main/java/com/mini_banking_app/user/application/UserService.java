package com.mini_banking_app.user.application;

import com.mini_banking_app.user.api.Role;
import com.mini_banking_app.user.api.UserAdministration;
import com.mini_banking_app.user.api.UserDirectory;
import com.mini_banking_app.user.api.UserNotFoundException;
import com.mini_banking_app.user.api.UserOperationNotAllowedException;
import com.mini_banking_app.user.api.UserRef;
import com.mini_banking_app.user.api.UserView;
import com.mini_banking_app.user.domain.UserAccount;
import com.mini_banking_app.user.infrastructure.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserService implements UserDirectory, UserAdministration {

    private final UserJpaRepository users;

    public UserService(UserJpaRepository users) {
        this.users = users;
    }

    @Override
    public Optional<UserRef> findByUsername(String username) {
        return users.findByUsername(username).map(UserService::toRef);
    }

    @Override
    public UserRef require(String username) {
        return findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
    }

    @Override
    public UserRef requireById(Long id) {
        return users.findById(id).map(UserService::toRef)
                .orElseThrow(() -> new UserNotFoundException("id " + id));
    }

    @Override
    public Map<Long, String> usernames(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return users.findAllById(ids).stream()
                .collect(Collectors.toMap(UserAccount::getId, UserAccount::getUsername));
    }

    @Override
    public List<UserView> allUsers() {
        return users.findAllByOrderByIdAsc().stream().map(UserService::toView).toList();
    }

    /**
     * Правила замість жорстко закодованого логіна "admin" у першій версії:
     * не можна змінити статус самому собі і не можна заблокувати останнього активного адміністратора.
     */
    @Override
    @Transactional
    public UserView toggleEnabled(Long userId, String actorUsername) {
        UserAccount target = users.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("id " + userId));
        if (target.getUsername().equals(actorUsername)) {
            throw new UserOperationNotAllowedException("Неможливо змінити статус власного облікового запису");
        }
        if (target.isAdmin() && target.isEnabled() && users.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw new UserOperationNotAllowedException("Неможливо заблокувати останнього активного адміністратора");
        }
        target.toggleEnabled();
        return toView(target);
    }

    private static UserRef toRef(UserAccount u) {
        return new UserRef(u.getId(), u.getUsername(), u.isAdmin(), u.isEnabled());
    }

    private static UserView toView(UserAccount u) {
        return new UserView(u.getId(), u.getUsername(), u.getRole(), u.isEnabled());
    }
}
