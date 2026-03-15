package com.examplatform.apigateway.filter;

import com.examplatform.apigateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

/**
 * JWT validation gateway filter.
 * Validates the Bearer token from Authorization header using RSA public key.
 * On success, injects claims as headers for downstream services:
 *   X-Student-Id, X-Student-Email, X-Student-Name, X-Student-Role
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthGatewayFilter implements GatewayFilter {

    private final JwtProperties jwtProperties;
    private PublicKey publicKey;

    // Paths that bypass JWT validation
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/certificates/verify/",
            "/actuator/health",
            "/actuator/info"
    );

    @PostConstruct
    public void loadPublicKey() {
        try {
            String keyPath = jwtProperties.getPublicKeyPath();
            String pem;
            // Support both absolute filesystem paths (docker volume mount: /keys/public_key.pem)
            // and classpath-relative paths (local dev: keys/public_key.pem)
            if (Paths.get(keyPath).isAbsolute()) {
                pem = Files.readString(Paths.get(keyPath));
            } else {
                ClassPathResource resource = new ClassPathResource(keyPath);
                pem = new String(resource.getInputStream().readAllBytes());
            }
            pem = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                     .replace("-----END PUBLIC KEY-----", "")
                     .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(pem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
            log.info("JWT public key loaded from {}", keyPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JWT public key", e);
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Skip validation for public endpoints
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing Authorization header");
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Inject claims as headers for downstream services
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Student-Id", claims.getSubject())
                    .header("X-Student-Email", claims.get("email", String.class))
                    .header("X-Student-Name", claims.get("name", String.class))
                    .header("X-Student-Role", claims.get("role", String.class))
                    .build();

            log.debug("JWT valid for user: {}, role: {}", claims.getSubject(), claims.get("role"));
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        log.debug("Unauthorized: {}", reason);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
