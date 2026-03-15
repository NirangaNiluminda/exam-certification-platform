package com.examplatform.examservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.examplatform.examservice", "com.examplatform.outbox"})
@EnableJpaRepositories(basePackages = {"com.examplatform.examservice", "com.examplatform.outbox"})
@EntityScan(basePackages = {"com.examplatform.examservice", "com.examplatform.outbox"})
@EnableScheduling
public class ExamServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExamServiceApplication.class, args);
    }
}
