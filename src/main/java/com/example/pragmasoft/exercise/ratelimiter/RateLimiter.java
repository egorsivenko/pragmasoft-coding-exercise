package com.example.pragmasoft.exercise.ratelimiter;

/**
 * An interface for implementing rate limiting mechanisms.
 *
 * @param <K> the type of the key used to identify each client
 */
@FunctionalInterface
public interface RateLimiter<K> {

    /**
     * Attempts to acquire permission for the specified client key based on the rate limiting policy.
     * If the request exceeds the rate limit, this method should throw an exception
     * or handle the limit breach appropriately.
     *
     * @param clientKey the key that identifies the client to be rate-limited
     */
    void tryAcquire(K clientKey);
}
