package com.examplatform.certificateservice.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

/**
 * Handles certificate PDF storage in MinIO (S3-compatible object store).
 * Creates the certificates bucket on startup if it doesn't exist.
 */
@Slf4j
@Service
public class MinioStorageService {

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioStorageService(MinioClient minioClient,
                               @Value("${minio.bucket:certificates}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to ensure MinIO bucket exists: {}", bucketName, e);
        }
    }

    /**
     * Uploads a PDF and returns the object key.
     */
    public String upload(String objectKey, byte[] pdfBytes) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(pdfBytes), pdfBytes.length, -1)
                    .contentType("application/pdf")
                    .build());
            log.info("Uploaded certificate to MinIO: {}/{}", bucketName, objectKey);
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload certificate to MinIO", e);
        }
    }

    /**
     * Generates a pre-signed download URL valid for 1 hour.
     */
    public String generatePresignedUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .method(Method.GET)
                    .expiry(1, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }
}
