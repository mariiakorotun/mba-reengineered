package com.mini_banking_app.account;

import com.mini_banking_app.user.api.UserDirectory;
import com.mini_banking_app.user.api.UserNotFoundException;
import com.mini_banking_app.user.api.UserRef;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Довідник користувачів для тестів модуля account. */
class FakeUserDirectory implements UserDirectory {

    private final Map<String, UserRef> byName = new LinkedHashMap<>();

    UserRef add(long id, String username, boolean admin) {
        UserRef ref = new UserRef(id, username, admin, true);
        byName.put(username, ref);
        return ref;
    }

    @Override
    public Optional<UserRef> findByUsername(String username) {
        return Optional.ofNullable(byName.get(username));
    }

    @Override
    public UserRef require(String username) {
        return findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
    }

    @Override
    public UserRef requireById(Long id) {
        return byName.values().stream().filter(u -> u.id().equals(id)).findFirst()
                .orElseThrow(() -> new UserNotFoundException("id " + id));
    }

    @Override
    public Map<Long, String> usernames(Collection<Long> ids) {
        return byName.values().stream().filter(u -> ids.contains(u.id()))
                .collect(Collectors.toMap(UserRef::id, UserRef::username));
    }
}
