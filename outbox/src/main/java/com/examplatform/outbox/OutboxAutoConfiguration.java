package com.examplatform.outbox;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;

/**
 * Spring Boot auto-configuration for the shared Outbox library.
 *
 * Registers {@code com.examplatform.outbox} as an additional auto-configuration
 * package so that Spring Boot's JPA entity scan AND Spring Data repository scan
 * both include this package — regardless of which service imports the library.
 *
 * This is the standard Spring Boot 3.x pattern for shared library auto-configuration.
 * Registered via META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 */
@AutoConfiguration
@AutoConfigurationPackage(basePackages = "com.examplatform.outbox")
public class OutboxAutoConfiguration {
}
