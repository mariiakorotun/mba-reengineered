package com.mini_banking_app.user.api;

import java.util.List;

/** Адміністративні сценарії модуля user. */
public interface UserAdministration {

    List<UserView> allUsers();

    /** Блокує або розблоковує користувача. */
    UserView toggleEnabled(Long userId, String actorUsername);
}
