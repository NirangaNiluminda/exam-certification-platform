package com.examplatform.gradingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.examplatform.gradingservice", "com.examplatform.outbox"})
@EnableJpaRepositories(basePackages = {"com.examplatform.gradingservice", "com.examplatform.outbox"})
@EntityScan(basePackages = {"com.examplatform.gradingservice", "com.examplatform.outbox"})
public class GradingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GradingServiceApplication.class, args);
    }
}
