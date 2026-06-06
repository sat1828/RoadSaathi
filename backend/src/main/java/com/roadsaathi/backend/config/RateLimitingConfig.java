package com.roadsaathi.backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitingConfig {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.reports-per-hour}")
    private int reportsPerHour;

    public Bucket resolveBucket(String userId) {
        return buckets.computeIfAbsent(userId, this::createBucket);
    }

    private Bucket createBucket(String userId) {
        Bandwidth limit = Bandwidth.classic(reportsPerHour, Refill.greedy(reportsPerHour, Duration.ofHours(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
