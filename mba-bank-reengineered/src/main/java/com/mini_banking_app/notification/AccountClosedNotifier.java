package com.mini_banking_app.notification;

import com.mini_banking_app.account.api.AccountClosedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Надсилає адміністратору лист про закриття рахунку.
 * <p>
 * Виконується лише після успішного коміту та асинхронно: повільний SMTP-сервер більше
 * не затримує відповідь користувачу, а лист не надсилається, якщо транзакцію відкотили.
 */
@Component
public class AccountClosedNotifier {

    private static final Logger log = LoggerFactory.getLogger(AccountClosedNotifier.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final NotificationProperties properties;

    public AccountClosedNotifier(ObjectProvider<JavaMailSender> mailSender, NotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(AccountClosedEvent event) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!properties.enabled() || sender == null) {
            log.info("Рахунок {} закрито користувачем {} (сповіщення поштою вимкнено)",
                    event.accountId(), event.closedBy());
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(properties.adminEmail());
        if (properties.from() != null && !properties.from().isBlank()) {
            message.setFrom(properties.from());
        }
        message.setSubject("Закриття рахунку: " + event.accountLabel());
        message.setText("Повідомлення системи MBA BANK.\n\n"
                + "Рахунок: " + event.accountLabel() + " (id " + event.accountId() + ")\n"
                + "Закрив: " + event.closedBy() + "\n"
                + "Час: " + event.closedAt() + "\n"
                + "Причина: " + event.reason());
        try {
            sender.send(message);
        } catch (MailException e) {
            log.warn("Не вдалося надіслати сповіщення про закриття рахунку {}: {}", event.accountId(), e.getMessage());
        }
    }
}
