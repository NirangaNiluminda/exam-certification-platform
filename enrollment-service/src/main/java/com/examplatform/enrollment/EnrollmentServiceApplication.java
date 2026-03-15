package com.examplatform.enrollment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.examplatform.enrollment", "com.examplatform.outbox"})
@EnableJpaRepositories(basePackages = {"com.examplatform.enrollment", "com.examplatform.outbox"})
@EntityScan(basePackages = {"com.examplatform.enrollment", "com.examplatform.outbox"})
public class EnrollmentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EnrollmentServiceApplication.class, args);
    }
}
