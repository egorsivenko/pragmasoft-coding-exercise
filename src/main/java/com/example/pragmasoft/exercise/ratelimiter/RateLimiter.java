package com.example.pragmasoft.exercise.ratelimiter;

@FunctionalInterface
public interface RateLimiter<K> {

    void tryAcquire(K clientKey);
}
