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

    private final ConcurrentHashMap<String, TokenBucket> tokenBuckets = new ConcurrentHashMap<>();
    private final int capacity = 10;
    private final long refillPeriod = 10000;
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

        TokenBucket tokenBucket = tokenBuckets.computeIfAbsent(clientKey, k -> new TokenBucket(capacity, refillPeriod));
        return tokenBucket.isAllowed();
    }
}
