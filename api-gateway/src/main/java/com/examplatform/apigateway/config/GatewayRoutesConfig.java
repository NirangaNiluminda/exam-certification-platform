package com.examplatform.apigateway.config;

import com.examplatform.apigateway.filter.JwtAuthGatewayFilter;
import com.examplatform.apigateway.filter.RateLimitFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Defines all gateway routes. Each route:
 * 1. Matches on URI prefix
 * 2. Applies JWT auth filter (validates token, injects user headers)
 * 3. Applies rate limit filter
 * 4. Strips the /api/v1 prefix (optional — services keep it here for clarity)
 * 5. Forwards to the downstream service
 */
@Configuration
public class GatewayRoutesConfig {

    private final JwtAuthGatewayFilter jwtAuthGatewayFilter;
    private final RateLimitFilter rateLimitFilter;

    public GatewayRoutesConfig(JwtAuthGatewayFilter jwtAuthGatewayFilter,
                                RateLimitFilter rateLimitFilter) {
        this.jwtAuthGatewayFilter = jwtAuthGatewayFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()

            // ── Auth Service (public) ──────────────────────────────────
            .route("auth-service", r -> r
                .path("/api/v1/auth/**")
                .filters(f -> f
                    .filter(jwtAuthGatewayFilter)
                    .filter(rateLimitFilter)
                    .addRequestHeader("X-Gateway", "exam-platform")
                )
                .uri("http://auth-service:8081")
            )

            // ── Enrollment Service ─────────────────────────────────────
            .route("enrollment-service", r -> r
                .path("/api/v1/enrollments/**")
                .filters(f -> f
                    .filter(jwtAuthGatewayFilter)
                    .filter(rateLimitFilter)
                    .addRequestHeader("X-Gateway", "exam-platform")
                )
                .uri("http://enrollment-service:8082")
            )

            // ── Exam Service (student endpoints) ───────────────────────
            .route("exam-service-student", r -> r
                .path("/api/v1/exams/**")
                .filters(f -> f
                    .filter(jwtAuthGatewayFilter)
                    .filter(rateLimitFilter)
                    .addRequestHeader("X-Gateway", "exam-platform")
                )
                .uri("http://exam-service:8084")
            )

            // ── Exam Service (admin endpoints) ─────────────────────────
            .route("exam-service-admin", r -> r
                .path("/api/v1/admin/**")
                .filters(f -> f
                    .filter(jwtAuthGatewayFilter)
                    .filter(rateLimitFilter)
                    .addRequestHeader("X-Gateway", "exam-platform")
                )
                .uri("http://exam-service:8084")
            )

            // ── Certificate Service ────────────────────────────────────
            .route("certificate-service", r -> r
                .path("/api/v1/certificates/**")
                .filters(f -> f
                    .filter(jwtAuthGatewayFilter)
                    .filter(rateLimitFilter)
                    .addRequestHeader("X-Gateway", "exam-platform")
                )
                .uri("http://certificate-service:8086")
            )

            .build();
    }
}
