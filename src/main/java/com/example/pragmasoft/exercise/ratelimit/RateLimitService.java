package com.example.pragmasoft.exercise.ratelimit;

import com.example.pragmasoft.exercise.bucket.TokenBucket;
import com.example.pragmasoft.exercise.extractor.RequestHeaderClientKeyExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling rate limiting based on the token bucket algorithm.
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
    private final long expirationTime;

    public RateLimitService(RequestHeaderClientKeyExtractor keyExtractor,
                            @Value("${rate.limit.capacity}") int capacity,
                            @Value("${rate.limit.refillPeriod}") long refillPeriod,
                            @Value("${rate.limit.expirationTime}") long expirationTime) {
        this.keyExtractor = keyExtractor;
        this.capacity = capacity;
        this.refillPeriod = refillPeriod;
        this.expirationTime = expirationTime;
    }

    /**
     * Determines if a request is allowed based on the client's rate limit.
     *
     * @param request the HttpServletRequest to check
     * @return true if the request is allowed, false if the rate limit is exceeded
     */
    public boolean isAllowed(HttpServletRequest request) {
        String clientKey = keyExtractor.extractClientKey(request);

        TokenBucket tokenBucket = tokenBuckets.computeIfAbsent(clientKey, k -> new TokenBucket(capacity, refillPeriod));
        return tokenBucket.isAllowed();
    }

    @Scheduled(fixedDelayString = "${rate.limit.cleanupInterval}")
    public void cleanupStaleBuckets() {
        long currentTime = System.currentTimeMillis();

        tokenBuckets.entrySet().removeIf(entry -> {
            TokenBucket bucket = entry.getValue();
            return currentTime - bucket.getLastRequestTime() > expirationTime;
        });
    }
}
