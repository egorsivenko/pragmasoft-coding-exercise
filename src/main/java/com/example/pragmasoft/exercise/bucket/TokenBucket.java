package com.example.pragmasoft.exercise.bucket;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of the token bucket algorithm, used for rate limiting.
 * Refills a fixed number of tokens at a defined rate, and allows or denies an action based on token availability.
 */
public class TokenBucket {

    private final int capacity;
    private final long refillPeriod;

    private final AtomicInteger tokens;
    private final AtomicLong lastRefillTime;
    private final AtomicLong lastRequestTime;

    /**
     * Constructs a new TokenBucket with the specified capacity and refill period.
     *
     * @param capacity     the maximum number of tokens the bucket can hold
     * @param refillPeriod the time (in milliseconds) after which the bucket should be refilled
     */
    public TokenBucket(int capacity, long refillPeriod) {
        this.capacity = capacity;
        this.refillPeriod = refillPeriod;
        this.tokens = new AtomicInteger(capacity);
        this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
        this.lastRequestTime = new AtomicLong(System.currentTimeMillis());
    }

    /**
     * Checks if an action is allowed by consuming a token from the bucket.
     *
     * @return true if the action is allowed, false if the token bucket is empty
     */
    public boolean isAllowed() {
        refillBucket();
        if (tokens.get() > 0) {
            tokens.decrementAndGet();
            lastRequestTime.set(System.currentTimeMillis());
            return true;
        }
        return false;
    }

    /**
     * Refills the bucket with tokens if the refill period has elapsed since the last refill.
     */
    private void refillBucket() {
        long currentTime = System.currentTimeMillis();
        long periodSinceLastRefill = currentTime - lastRefillTime.get();

        if (periodSinceLastRefill >= refillPeriod) {
            tokens.set(capacity);
            lastRefillTime.set(currentTime);
        }
    }

    /**
     * Returns the timestamp of the last successful token consumption request.
     *
     * @return the time in milliseconds of the last request that successfully consumed a token
     */
    public long getLastRequestTime() {
        return lastRequestTime.get();
    }
}
