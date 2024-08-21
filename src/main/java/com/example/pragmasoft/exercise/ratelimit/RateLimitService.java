package com.example.pragmasoft.exercise.ratelimit;

import com.example.pragmasoft.exercise.bucket.TokenBucket;
import com.example.pragmasoft.exercise.extractor.RequestNetworkClientKeyExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling rate limiting based on the token bucket algorithm.
 */
@Service
public class RateLimitService {

    private static final int CAPACITY = 10;
    private static final long REFILL_PERIOD = 10000;

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
    private final RequestNetworkClientKeyExtractor keyExtractor;

    public RateLimitService(RequestNetworkClientKeyExtractor keyExtractor) {
        this.keyExtractor = keyExtractor;
    }

    /**
     * Determines if a request is allowed based on the client's rate limit.
     *
     * @param request the HttpServletRequest to check
     * @return true if the request is allowed, false if the rate limit is exceeded
     */
    public boolean isAllowed(HttpServletRequest request) {
        String clientKey = keyExtractor.extractClientKey(request);

        TokenBucket tokenBucket = tokenBuckets.computeIfAbsent(clientKey, k -> new TokenBucket(CAPACITY, REFILL_PERIOD));
        return tokenBucket.isAllowed();
    }
}
