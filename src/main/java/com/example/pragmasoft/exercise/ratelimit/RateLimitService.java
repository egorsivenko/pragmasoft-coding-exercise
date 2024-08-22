package com.example.pragmasoft.exercise.ratelimit;

import com.example.pragmasoft.exercise.bucket.TokenBucket;
import com.example.pragmasoft.exercise.extractor.RequestHeaderClientKeyExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling rate limiting using the token bucket algorithm.
 * This service manages multiple token buckets, one for each client, identified by a unique key.
 * The service ensures that requests are allowed or denied based on the availability of tokens
 * in the client's token bucket. It also includes a mechanism for cleaning up stale buckets
 * to manage memory usage effectively.
 */
@Service
public class RateLimitService {

    /**
     * In this case, the thread-safe ConcurrentHashMap is used, but to improve the performance and scalability,
     * we may use some centralized storage, e.g. Redis cache or a NoSQL database like Mongo.
     * As for me, Redis would be better as it's a popular choice for distributed rate limiting
     * due to its fast in-memory storage and support for distributed locks and data structures.
     * In that case, we can store user-specific statistics, i.e. number of tokens and the time of the last refill,
     * by accessing them with a client key or authorization token.
     * The main operations used would be GET, SET, EXISTS, and EXPIRE.
     */
    private final ConcurrentHashMap<String, TokenBucket> tokenBuckets = new ConcurrentHashMap<>();
    private final RequestHeaderClientKeyExtractor keyExtractor;

    private final int capacity;
    private final long refillPeriod;
    private final int tokensPerPeriod;
    private final long expirationTime;

    /**
     * Constructs a new RateLimitService with the specified configuration values.
     *
     * @param keyExtractor    the component responsible for extracting client keys from requests
     * @param capacity        the maximum number of tokens each token bucket can hold
     * @param refillPeriod    the time period in milliseconds after which tokens are added to the bucket
     * @param tokensPerPeriod the number of tokens added to the bucket after each refill period
     * @param expirationTime  the time in milliseconds after which unused token buckets are considered stale and are removed
     */
    public RateLimitService(RequestHeaderClientKeyExtractor keyExtractor,
                            @Value("${rate.limit.capacity}") int capacity,
                            @Value("${rate.limit.refillPeriod}") long refillPeriod,
                            @Value("${rate.limit.tokensPerPeriod}") int tokensPerPeriod,
                            @Value("${rate.limit.expirationTime}") long expirationTime) {
        this.keyExtractor = keyExtractor;
        this.capacity = capacity;
        this.refillPeriod = refillPeriod;
        this.tokensPerPeriod = tokensPerPeriod;
        this.expirationTime = expirationTime;
    }

    /**
     * Determines if a request is allowed based on the client's rate limit.
     * A token is consumed from the client's token bucket if available.
     * If the token bucket does not exist for the client, a new one is created.
     *
     * @param request the HttpServletRequest to check
     * @return true if the request is allowed, false if the rate limit is exceeded
     */
    public boolean isAllowed(HttpServletRequest request) {
        String clientKey = keyExtractor.extractClientKey(request);

        TokenBucket tokenBucket = tokenBuckets.computeIfAbsent(clientKey,
                k -> new TokenBucket(capacity, refillPeriod, tokensPerPeriod));
        return tokenBucket.isAllowed();
    }

    /**
     * Periodically cleans up stale token buckets that have not been used for a specified period of time.
     * The method is invoked at fixed intervals and iterates through the {@link RateLimitService#tokenBuckets},
     * removing entries where the time since the last request exceeds the {@link RateLimitService#expirationTime}.
     * This helps in managing memory usage and ensuring that the rate limiter
     * only retains relevant token buckets for active clients.
     */
    @Scheduled(fixedDelayString = "${rate.limit.cleanupInterval}")
    public void cleanupStaleBuckets() {
        long currentTime = System.currentTimeMillis();

        tokenBuckets.entrySet().removeIf(entry -> {
            TokenBucket bucket = entry.getValue();
            return currentTime - bucket.getLastRequestTime() > expirationTime;
        });
    }
}
