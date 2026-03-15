package com.examplatform.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory token bucket rate limiter.
 * Limits each client IP to 100 requests per minute.
 *
 * For production with multiple gateway replicas, replace with
 * Spring Cloud Gateway's Redis-backed RequestRateLimiter:
 *   spring.cloud.gateway.default-filters[0]: RequestRateLimiter
 *
 * This implementation is intentionally kept simple for demo / single-instance use.
 */
@Slf4j
@Component
public class RateLimitFilter implements GatewayFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 100;

    // ip -> (windowStartEpochSecond, requestCount)
    private final Map<String, long[]> buckets = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = extractClientIp(exchange);
        long nowEpochSecond = Instant.now().getEpochSecond();
        long windowStart = nowEpochSecond - (nowEpochSecond % 60); // 1-minute window

        long[] bucket = buckets.compute(clientIp, (ip, existing) -> {
            if (existing == null || existing[0] != windowStart) {
                return new long[]{windowStart, 1};
            }
            existing[1]++;
            return existing;
        });

        long requestCount = bucket[1];

        if (requestCount > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for IP: {} ({} req/min)", clientIp, requestCount);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
            exchange.getResponse().getHeaders().add("Retry-After", "60");
            return exchange.getResponse().setComplete();
        }

        exchange.getResponse().getHeaders()
                .add("X-RateLimit-Remaining", String.valueOf(MAX_REQUESTS_PER_MINUTE - requestCount));
        return chain.filter(exchange);
    }

    private String extractClientIp(ServerWebExchange exchange) {
        // Respect X-Forwarded-For when behind a load balancer
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }
}
