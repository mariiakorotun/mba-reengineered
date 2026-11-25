package com.mini_banking_app.account.api;

/** Тип рахунку. У першій версії поле "Тип рахунку" на формі ігнорувалося контролером. */
public enum AccountKind {
    CURRENT("Поточний"),
    SAVINGS("Ощадний");

    private final String title;

    AccountKind(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
