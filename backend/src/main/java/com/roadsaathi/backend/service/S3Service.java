package com.roadsaathi.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final long presignedUrlExpiryMinutes;

    public S3Service(S3Client s3Client, S3Presigner s3Presigner,
                     @Value("${app.s3.bucket}") String bucket,
                     @Value("${app.s3.presigned-url-expiry-minutes}") long presignedUrlExpiryMinutes) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
        this.presignedUrlExpiryMinutes = presignedUrlExpiryMinutes;
    }

    public String generatePresignedPutUrl(String key, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpiryMinutes))
                .putObjectRequest(objectRequest)
                .build();

        URL url = s3Presigner.presignPutObject(presignRequest).url();
        return url.toString();
    }

    public String uploadPhoto(String key, InputStream inputStream, String contentType) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) bytes.length)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));

            S3Utilities utilities = s3Client.utilities();
            URL url = utilities.getUrl(builder -> builder.bucket(bucket).key(key));
            return url.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload photo to S3", e);
        }
    }

    public void deletePhoto(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
        log.info("Deleted S3 object: {}", key);
    }

    public String generateKey(String prefix, String extension) {
        return prefix + "/" + UUID.randomUUID() + "." + extension;
    }
}
