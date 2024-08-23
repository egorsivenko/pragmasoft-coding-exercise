package com.example.pragmasoft.exercise.bucket;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of the token bucket algorithm used for rate limiting.
 * Manages a bucket with a fixed capacity, refilling it with tokens at a specified rate.
 * Each token represents a permit for an action, and the action is allowed or denied based on token availability.
 * The class is thread-safe, ensuring correct behavior in concurrent environments.
 */
public class TokenBucket {

    private final long capacity;
    private final long nanosToRefillToken;
    private final AtomicReference<State> stateReference;

    /**
     * Inner class representing the state of the token bucket.
     * It includes the number of available tokens, the last refill time, and the last request time.
     */
    private static final class State {

        private long availableTokens;
        private long lastRefillNanoTime;
        private long lastRequestNanoTime;
    }

    /**
     * Constructs a new TokenBucket with the specified capacity and refill period.
     *
     * @param capacity     the maximum number of tokens the bucket can hold
     * @param refillPeriod the duration after which the bucket should be refilled
     */
    public TokenBucket(long capacity, Duration refillPeriod) {
        this.capacity = capacity;
        this.nanosToRefillToken = refillPeriod.toNanos() / capacity;

        State state = new State();
        state.availableTokens = capacity;
        state.lastRefillNanoTime = System.nanoTime();
        state.lastRequestNanoTime = System.nanoTime();

        this.stateReference = new AtomicReference<>(state);
    }

    /**
     * Checks if an action is allowed by consuming a token from the bucket.
     *
     * @return true if the action is allowed, false if the token bucket is empty
     */
    public boolean isAllowed() {
        State newState = new State();

        while (true) {
            long now = System.nanoTime();
            State previousState = stateReference.get();
            newState.availableTokens = previousState.availableTokens;
            newState.lastRefillNanoTime = previousState.lastRefillNanoTime;
            newState.lastRequestNanoTime = previousState.lastRequestNanoTime;
            refillBucket(newState, now);

            if (newState.availableTokens < 1) {
                return false;
            }
            newState.availableTokens -= 1;
            if (stateReference.compareAndSet(previousState, newState)) {
                newState.lastRequestNanoTime = now;
                return true;
            }
        }
    }

    /**
     * Refills the bucket with tokens if sufficient time has elapsed since the last refill.
     * The number of tokens added is calculated based on the time elapsed and the rate of refill.
     * If enough time has passed to add multiple tokens, they will be added, up to the bucket's capacity.
     *
     * @param state the current state of the token bucket
     * @param now   the current time in nanoseconds
     */
    private void refillBucket(State state, long now) {
        long elapsedNanoTime = now - state.lastRefillNanoTime;
        long tokensToRefill = elapsedNanoTime / nanosToRefillToken;

        if (tokensToRefill > 0) {
            state.availableTokens = Math.min(capacity, state.availableTokens + tokensToRefill);
            state.lastRefillNanoTime += tokensToRefill * nanosToRefillToken;
        }
    }

    /**
     * Returns the timestamp of the last successful token consumption request in nanoseconds.
     *
     * @return the time in nanoseconds of the last request that successfully consumed a token
     */
    public long getLastRequestNanoTime() {
        return stateReference.get().lastRequestNanoTime;
    }
}
