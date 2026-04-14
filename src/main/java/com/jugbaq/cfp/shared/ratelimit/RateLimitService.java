package com.jugbaq.cfp.shared.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Registro: 5 intentos por IP cada 10 minutos.
     */
    public Bucket registrationBucket(String ip) {
        return buckets.computeIfAbsent("register:" + ip, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(5)
                        .refillIntervally(5, Duration.ofMinutes(10))
                        .build())
                .build());
    }

    /**
     * Login: 10 intentos por IP cada 5 minutos.
     */
    public Bucket loginBucket(String ip) {
        return buckets.computeIfAbsent("login:" + ip, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(10)
                        .refillIntervally(10, Duration.ofMinutes(5))
                        .build())
                .build());
    }

    /**
     * Submission: 20 por usuario cada hora.
     */
    public Bucket submissionBucket(String userId) {
        return buckets.computeIfAbsent("submit:" + userId, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(20)
                        .refillIntervally(20, Duration.ofHours(1))
                        .build())
                .build());
    }

    /**
     * Upload: 10 por usuario cada hora.
     */
    public Bucket uploadBucket(String userId) {
        return buckets.computeIfAbsent("upload:" + userId, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(10)
                        .refillIntervally(10, Duration.ofHours(1))
                        .build())
                .build());
    }

    public void clearBucket(String key) {
        buckets.remove(key);
    }
}
