package com.example.pragmasoft.exercise.ratelimiter;

import com.example.pragmasoft.exercise.bucket.TokenBucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.zalando.problem.Status.TOO_MANY_REQUESTS;

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
     * For better performance and scalability, centralized storage such as Redis
     * or a NoSQL database like MongoDB can be used. Redis is particularly suitable for distributed
     * rate limiting due to its in-memory storage and support for distributed locks and data structures.
     * Redis operations like GET, SET, EXISTS, and EXPIRE could be employed to manage client-specific
     * statistics, including the number of available tokens, the last refill and request time,
     * using the client key or authorization token for access.
     */
    private final Map<String, TokenBucket> tokenBuckets;

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
        this.tokenBuckets = Collections.synchronizedMap(new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, TokenBucket> eldest) {
                Set<Map.Entry<String, TokenBucket>> entries = entrySet();

                synchronized (this) {
                    Iterator<Map.Entry<String, TokenBucket>> iterator = entries.iterator();
                    long removalThreshold = System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(refillPeriod);

                    while (iterator.hasNext()) {
                        TokenBucket bucket = iterator.next().getValue();
                        if (bucket.getLastRefillNanoTime() < removalThreshold) {
                            iterator.remove();
                        } else {
                            break;
                        }
                    }
                }
                return false;
            }
        });
    }

    /**
     * Determines if a request is allowed based on the client's rate limit.
     * A token is consumed from the client's token bucket if available.
     * If the token bucket does not exist for the client, a new one is created.
     *
     * @param clientKey the key that identifies each separate client
     */
    @Override
    public synchronized void tryAcquire(String clientKey) {
        TokenBucket tokenBucket = tokenBuckets.computeIfAbsent(clientKey,
                key -> new TokenBucket(capacity, refillPeriod));
        if (!tokenBucket.isAllowed()) {
            throw Problem.builder()
                    .withTitle("Rate Limit Exceeded")
                    .withStatus(TOO_MANY_REQUESTS)
                    .withDetail("Exceeded the maximum number of requests. Try again later.")
                    .with("requests_number", capacity)
                    .with("wait_seconds", refillPeriod.toSeconds())
                    .build();
        }
    }
}
