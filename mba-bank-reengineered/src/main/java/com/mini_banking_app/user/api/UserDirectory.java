package com.mini_banking_app.user.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/** Довідник користувачів — контракт модуля user для інших модулів. */
public interface UserDirectory {

    Optional<UserRef> findByUsername(String username);

    /** @throws UserNotFoundException якщо користувача немає */
    UserRef require(String username);

    /** @throws UserNotFoundException якщо користувача немає */
    UserRef requireById(Long id);

    /** Пакетне отримання логінів (запобігає проблемі N+1 у звітах). */
    Map<Long, String> usernames(Collection<Long> ids);
}
