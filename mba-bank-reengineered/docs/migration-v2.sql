-- Міграція схеми Azure SQL: MBA Bank 1.0 -> 2.0
-- Усі зміни адитивні: наявні дані й зовнішні ключі не змінюються.
-- Виконувати до першого запуску версії 2.0 з MBA_DDL_AUTO=validate
-- (або не виконувати, якщо використовується MBA_DDL_AUTO=update — Hibernate додасть стовпці сам).

-- 0. Перевірка: валюти мають бути з довідника UAH / USD / EUR
SELECT DISTINCT currency FROM accounts WHERE currency NOT IN ('UAH', 'USD', 'EUR');

BEGIN TRANSACTION;

-- 1. Рахунки: тип, статус, дата закриття, версія для оптимістичного блокування
ALTER TABLE accounts ADD kind      NVARCHAR(20) NOT NULL CONSTRAINT df_accounts_kind    DEFAULT 'CURRENT';
ALTER TABLE accounts ADD status    NVARCHAR(20) NOT NULL CONSTRAINT df_accounts_status  DEFAULT 'ACTIVE';
ALTER TABLE accounts ADD closed_at DATETIME2(6) NULL;
ALTER TABLE accounts ADD version   BIGINT       NOT NULL CONSTRAINT df_accounts_version DEFAULT 0;

-- 2. Журнал: валюта, залишок після операції, ідентифікатор переказу
ALTER TABLE transactions ADD currency      VARCHAR(3)     NULL;
ALTER TABLE transactions ADD balance_after DECIMAL(19, 2) NULL;
ALTER TABLE transactions ADD reference     VARCHAR(36)    NULL;
GO

-- 3. Заповнення валюти для історичних записів
UPDATE t SET t.currency = a.currency
FROM transactions t JOIN accounts a ON a.id = t.account_id
WHERE t.currency IS NULL;

-- 4. Індекси під запити версії 2.0
CREATE INDEX ix_accounts_owner_status        ON accounts (user_id, status);
CREATE INDEX ix_transactions_account_created ON transactions (account_id, created_at);

COMMIT;
