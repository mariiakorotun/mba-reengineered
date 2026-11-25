package com.mini_banking_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Clock;

/**
 * MBA Bank 2.0 — модульний моноліт.
 * <p>
 * Модулі: {@code user}, {@code account}, {@code ledger}, {@code notification};
 * шар представлення — {@code web}; спільне ядро — {@code shared}.
 * Модулі взаємодіють лише через пакети {@code api} та доменні події.
 */
@SpringBootApplication
@EnableAsync
@ConfigurationPropertiesScan
public class MiniBankingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniBankingApplication.class, args);
    }

    /** Системний годинник як залежність — дає змогу підміняти час у тестах. */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
