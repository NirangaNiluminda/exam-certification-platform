package com.examplatform.certificateservice.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MinioStorageService {

    private final MinioClient uploadClient;
    private final MinioClient presignClient;
    private final String bucketName;

    public MinioStorageService(MinioClient uploadClient,
                               @Qualifier("presignClient") MinioClient presignClient,
                               @Value("${minio.bucket:certificates}") String bucketName) {
        this.uploadClient = uploadClient;
        this.presignClient = presignClient;
        this.bucketName = bucketName;
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            boolean exists = uploadClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName).build());
            if (!exists) {
                uploadClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to ensure MinIO bucket exists: {}", bucketName, e);
        }
    }

    public String upload(String objectKey, byte[] pdfBytes) {
        try {
            uploadClient.putObject(PutObjectArgs.builder()
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

    // Signed with the public-url endpoint so the signature matches when the browser hits localhost:9000
    public String generatePresignedUrl(String objectKey) {
        try {
            return presignClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
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
