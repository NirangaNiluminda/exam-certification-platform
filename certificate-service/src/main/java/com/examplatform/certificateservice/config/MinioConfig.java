package com.examplatform.certificateservice.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MinioConfig {

    // Used for uploads — connects via internal Docker hostname
    @Bean
    @Primary
    public MinioClient minioClient(
            @Value("${minio.url:http://minio:9000}") String url,
            @Value("${minio.access-key:minioadmin}") String accessKey,
            @Value("${minio.secret-key:minioadmin}") String secretKey) {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }

    // Used for pre-signed URL generation — signs with browser-accessible hostname
    @Bean
    @Qualifier("presignClient")
    public MinioClient presignMinioClient(
            @Value("${minio.public-url:http://localhost:9000}") String publicUrl,
            @Value("${minio.access-key:minioadmin}") String accessKey,
            @Value("${minio.secret-key:minioadmin}") String secretKey) {
        return MinioClient.builder()
                .endpoint(publicUrl)
                .credentials(accessKey, secretKey)
                .region("us-east-1")
                .build();
    }
}
