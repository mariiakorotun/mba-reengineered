# Розгортання MBA Bank 2.0 в Azure

Інфраструктура не змінилася відносно версії 1.0:

- **Мережа:** Application Gateway + WAF → Nginx → Ubuntu 24.04 VM.
- **Дані:** Azure SQL через Private Endpoint.
- **Автентифікація до БД:** Managed Identity.

Змінилося лише те, як застосунок отримує налаштування.

## Змінні середовища (профіль `prod`)

| Змінна | Обов'язкова | Опис |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | так | `prod` |
| `AZURE_SQL_URL` | так | `jdbc:sqlserver://<server>.database.windows.net:1433;databaseName=<db>;authentication=ActiveDirectoryManagedIdentity;msiClientId=<client-id>;encrypt=true;trustServerCertificate=false;` |
| `MBA_DDL_AUTO` | ні | `update` (за замовчуванням) або `validate` після ручної міграції `docs/migration-v2.sql` |
| `MBA_BOOTSTRAP_ADMIN_PASSWORD` | ні | пароль першого адміністратора, якщо таблиця `users` порожня |
| `MBA_ADMIN_EMAIL` | ні | отримувач листів про закриття рахунків |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MBA_MAIL_FROM` | ні | SMTP (пароль застосунку — лише тут, не в Git) |

## systemd-юніт на ВМ

`/etc/systemd/system/mba-bank.service`:

```ini
[Unit]
Description=MBA Bank 2.0
After=network-online.target

[Service]
User=mba
WorkingDirectory=/opt/mba-bank
EnvironmentFile=/etc/mba-bank/mba-bank.env
ExecStart=/usr/bin/java -XX:MaxRAMPercentage=75 -jar /opt/mba-bank/app.jar
Restart=on-failure
NoNewPrivileges=true
ProtectSystem=strict
ReadWritePaths=/opt/mba-bank

[Install]
WantedBy=multi-user.target
```

Файл `/etc/mba-bank/mba-bank.env` має права `600` і належить `root`. Секрети також можна зберігати в Azure Key Vault і зчитувати їх за Managed Identity.

## Docker (альтернатива ручному розгортанню)

```bash
docker build -t mba-bank:2.0 .
docker run -p 8080:8080 --env-file mba-bank.env mba-bank:2.0
```
