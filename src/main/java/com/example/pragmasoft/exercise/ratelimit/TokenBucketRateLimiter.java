package com.example.pragmasoft.exercise.ratelimit;

import com.example.pragmasoft.exercise.bucket.TokenBucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling rate limiting using the token bucket algorithm.
 * This service manages multiple token buckets, one for each client, identified by a unique key.
 * The service ensures that requests are allowed or denied based on the availability of tokens
 * in the client's token bucket. It also includes a mechanism for cleaning up stale buckets
 * to manage memory usage effectively.
 */
@Service
public class TokenBucketRateLimiter implements RateLimiter<String> {

    /**
     * In this implementation, a thread-safe ConcurrentHashMap is used to store token buckets
     * for each client. For better performance and scalability, centralized storage such as Redis
     * or a NoSQL database like MongoDB can be used. Redis is particularly suitable for distributed
     * rate limiting due to its in-memory storage and support for distributed locks and data structures.
     * Redis operations like GET, SET, EXISTS, and EXPIRE could be employed to manage client-specific
     * statistics, including the number of available tokens, the last refill and request time,
     * using the client key or authorization token for access.
     */
    private final ConcurrentHashMap<String, TokenBucket> tokenBuckets = new ConcurrentHashMap<>();

    private final long capacity;
    private final Duration refillPeriod;

    /**
     * Constructs a new RateLimitService with the specified configuration values.
     *
     * @param capacity     the maximum number of tokens each token bucket can hold
     * @param refillPeriod the time period in milliseconds after which tokens are added to the bucket
     */
    public TokenBucketRateLimiter(@Value("${rate.limit.capacity}") long capacity,
                                  @Value("${rate.limit.refillPeriod}") long refillPeriod) {
        this.capacity = capacity;
        this.refillPeriod = Duration.ofMillis(refillPeriod);
    }

    /**
     * Determines if a request is allowed based on the client's rate limit.
     * A token is consumed from the client's token bucket if available.
     * If the token bucket does not exist for the client, a new one is created.
     *
     * @param clientKey the key that identifies each separate client
     * @return true if the request is allowed, false if the rate limit is exceeded
     */
    @Override
    public boolean tryAcquire(String clientKey) {
        TokenBucket tokenBucket = tokenBuckets.computeIfAbsent(clientKey,
                key -> new TokenBucket(capacity, refillPeriod));
        return tokenBucket.isAllowed();
    }

    /**
     * Periodically cleans up stale token buckets that have not been used for a specified period of time.
     * The method is invoked at fixed intervals and iterates through the {@link TokenBucketRateLimiter#tokenBuckets},
     * removing entries where the time since the last request exceeds the {@link TokenBucketRateLimiter#refillPeriod}.
     * This helps in managing memory usage and ensuring that the rate limiter
     * only retains relevant token buckets for active clients.
     */
    @Scheduled(fixedDelayString = "${rate.limit.refillPeriod}")
    public void cleanupStaleBuckets() {
        long currentNanoTime = System.nanoTime();

        tokenBuckets.entrySet().removeIf(entry -> {
            TokenBucket bucket = entry.getValue();
            return currentNanoTime - bucket.getLastRefillNanoTime() > refillPeriod.toNanos();
        });
    }
}
