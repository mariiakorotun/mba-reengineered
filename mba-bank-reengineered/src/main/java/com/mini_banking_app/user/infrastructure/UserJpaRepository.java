package com.mini_banking_app.user.infrastructure;

import com.mini_banking_app.user.api.Role;
import com.mini_banking_app.user.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    long countByRoleAndEnabledTrue(Role role);

    List<UserAccount> findAllByOrderByIdAsc();
}
