package com.example.pragmasoft.exercise.ratelimit;

@FunctionalInterface
public interface RateLimiter<K> {

    boolean tryAcquire(K clientKey);
}
