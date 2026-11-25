package com.mini_banking_app.account.api;

/** Тип руху коштів. Значення DEPOSIT і WITHDRAW сумісні з даними першої версії. */
public enum MovementType {
    DEPOSIT(true),
    WITHDRAW(false),
    TRANSFER_IN(true),
    TRANSFER_OUT(false);

    private final boolean credit;

    MovementType(boolean credit) {
        this.credit = credit;
    }

    /** true — надходження на рахунок, false — списання. */
    public boolean isCredit() {
        return credit;
    }
}
