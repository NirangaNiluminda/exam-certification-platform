package com.examplatform.certificateservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.examplatform.certificateservice", "com.examplatform.outbox"})
@EnableJpaRepositories(basePackages = {"com.examplatform.certificateservice", "com.examplatform.outbox"})
@EntityScan(basePackages = {"com.examplatform.certificateservice", "com.examplatform.outbox"})
public class CertificateServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CertificateServiceApplication.class, args);
    }
}
