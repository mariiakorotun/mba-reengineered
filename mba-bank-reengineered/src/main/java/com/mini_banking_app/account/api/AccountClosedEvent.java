package com.mini_banking_app.account.api;

import java.time.LocalDateTime;

/** Доменна подія: рахунок закрито. Обробляється модулем notification після коміту. */
public record AccountClosedEvent(Long accountId,
                                 String accountLabel,
                                 Long ownerId,
                                 String closedBy,
                                 String reason,
                                 LocalDateTime closedAt) {
}
