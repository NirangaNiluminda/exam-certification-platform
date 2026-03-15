package com.examplatform.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds jwt.public-key-path from application.yml.
 * The public key is used to verify RS256 JWT tokens issued by auth-service.
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String publicKeyPath = "keys/public_key.pem";

    public String getPublicKeyPath() { return publicKeyPath; }
    public void setPublicKeyPath(String publicKeyPath) { this.publicKeyPath = publicKeyPath; }
}
