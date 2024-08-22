package com.example.pragmasoft.exercise.bucket;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of the token bucket algorithm used for rate limiting.
 * Manages a bucket with a fixed capacity, refilling it with tokens at a specified rate.
 * Each token represents a permit for an action, and the action is allowed or denied based on token availability.
 * The class is thread-safe, ensuring correct behavior in concurrent environments.
 */
public class TokenBucket {

    private final int capacity;
    private final long refillPeriod;
    private final int tokensPerPeriod;

    private final AtomicInteger tokens;
    private final AtomicLong lastRefillTime;
    private final AtomicLong lastRequestTime;

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Constructs a new TokenBucket with the specified capacity, refill period and tokens per period.
     *
     * @param capacity        the maximum number of tokens the bucket can hold
     * @param refillPeriod    the time in milliseconds after which the bucket should be refilled
     * @param tokensPerPeriod the number of tokens to add to the bucket after each refill period
     */
    public TokenBucket(int capacity, long refillPeriod, int tokensPerPeriod) {
        this.capacity = capacity;
        this.refillPeriod = refillPeriod;
        this.tokensPerPeriod = tokensPerPeriod;
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
        lock.lock();
        try {
            refillBucket();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                lastRequestTime.set(System.currentTimeMillis());
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Refills the bucket with tokens if the refill period has elapsed since the last refill.
     * The number of tokens added is calculated based on the time elapsed and the tokens per period.
     * If multiple refill periods have passed, multiple tokens will be added, up to the bucket's capacity.
     */
    private void refillBucket() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastRefillTime.get();
        long elapsedPeriods = elapsedTime / refillPeriod;
        int tokensToAdd = (int) (elapsedPeriods * tokensPerPeriod);

        if (tokensToAdd > 0) {
            tokens.set(Math.min(tokens.addAndGet(tokensToAdd), capacity));
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
