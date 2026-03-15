package com.examplatform.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.examplatform.paymentservice", "com.examplatform.outbox"})
@EnableJpaRepositories(basePackages = {"com.examplatform.paymentservice", "com.examplatform.outbox"})
@EntityScan(basePackages = {"com.examplatform.paymentservice", "com.examplatform.outbox"})
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
