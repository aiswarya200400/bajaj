package com.bfh.qualifier;

import com.bfh.qualifier.webhook_sql_bot.QualifierService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WebhookSqlBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookSqlBotApplication.class, args);
    }

    @Bean
    CommandLineRunner run(QualifierService service) {
        return args -> {
            System.out.println("✅ Spring Boot app started!");
            service.runFlow(); // 🔥 This actually calls your service
        };
    }
}
