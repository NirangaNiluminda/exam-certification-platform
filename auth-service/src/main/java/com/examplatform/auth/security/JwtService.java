package com.examplatform.auth.security;

import com.examplatform.auth.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Issues and validates RS256 JWT tokens.
 * Uses asymmetric key pair:
 *   - Private key: auth-service only (signs tokens)
 *   - Public key: all services (verify tokens)
 *
 * Lecture reference: Cloud Security.pdf — JWT structure (Header.Payload.Signature)
 */
@Slf4j
@Service
public class JwtService {

    @Value("${jwt.private-key-path}")
    private String privateKeyPath;

    @Value("${jwt.public-key-path}")
    private String publicKeyPath;

    @Value("${jwt.expiration-seconds:86400}")
    private long expirationSeconds;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    void loadKeys() throws Exception {
        this.privateKey = loadPrivateKey(privateKeyPath);
        this.publicKey  = loadPublicKey(publicKeyPath);
        log.info("JWT RS256 keys loaded successfully");
    }

    /** Generates a signed JWT for the given user. */
    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role",  user.getRole().name())
                .claim("name",  user.getFullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /** Parses and validates a JWT, returns claims. Throws if invalid/expired. */
    public Claims validateAndParseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ── Key loading helpers ──────────────────────────────────────

    private PrivateKey loadPrivateKey(String path) throws Exception {
        String pem = Files.readString(Paths.get(path))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private PublicKey loadPublicKey(String path) throws Exception {
        String pem = Files.readString(Paths.get(path))
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(keyBytes));
    }
}
