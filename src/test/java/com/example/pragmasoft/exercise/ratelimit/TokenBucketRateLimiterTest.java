package com.example.pragmasoft.exercise.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class TokenBucketRateLimiterTest {

    private static final String TEST_CLIENT_KEY = "127.0.0.1";

    private TokenBucketRateLimiter rateLimiter;

    @Value("${rate.limit.capacity}")
    private long capacity;

    @Value("${rate.limit.refillPeriod}")
    private long refillPeriod;

    @BeforeEach
    void setUp() {
        rateLimiter = new TokenBucketRateLimiter(capacity, refillPeriod);
    }

    @Test
    void shouldAllowRequestWhenTokensAvailable() {
        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimiter.tryAcquire(TEST_CLIENT_KEY));
        }
    }

    @Test
    void shouldBlockRequestWhenTokensDepleted() {
        for (int i = 0; i < capacity; i++) {
            rateLimiter.tryAcquire(TEST_CLIENT_KEY);
        }
        assertFalse(rateLimiter.tryAcquire(TEST_CLIENT_KEY));
    }

    @Test
    void shouldRefillTokensAfterRefillPeriod() throws InterruptedException {
        for (int i = 0; i < capacity; i++) {
            rateLimiter.tryAcquire(TEST_CLIENT_KEY);
        }
        Thread.sleep(refillPeriod); // Simulate waiting for the refill period

        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimiter.tryAcquire(TEST_CLIENT_KEY));
        }
    }

    @Test
    void shouldBlockRequestWhenTokensDepletedAfterRefill() throws InterruptedException {
        for (int i = 0; i < capacity; i++) {
            rateLimiter.tryAcquire(TEST_CLIENT_KEY);
        }
        Thread.sleep(refillPeriod); // Simulate waiting for the refill period

        for (int i = 0; i < capacity; i++) {
            rateLimiter.tryAcquire(TEST_CLIENT_KEY);
        }
        assertFalse(rateLimiter.tryAcquire(TEST_CLIENT_KEY));
    }

    @Test
    void shouldHandleConcurrentRequestsCorrectly() throws InterruptedException {
        int numberOfThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CyclicBarrier barrier = new CyclicBarrier(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        Runnable task = () -> {
            try {
                barrier.await();  // Ensure all threads start at the same time
                assertTrue(rateLimiter.tryAcquire(TEST_CLIENT_KEY));
            } catch (InterruptedException | BrokenBarrierException e) {
                fail("Test was interrupted");
            } finally {
                latch.countDown();
            }
        };

        IntStream.range(0, numberOfThreads).forEach(i -> executor.submit(task));

        latch.await();  // Wait for all threads to finish
        executor.shutdown();
    }

    @Test
    void shouldNotExceedRateLimitUnderConcurrentRequests() throws InterruptedException {
        int numberOfThreads = 50;  // More than the bucket capacity
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CyclicBarrier barrier = new CyclicBarrier(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successfulRequests = new AtomicInteger(0);

        Runnable task = () -> {
            try {
                barrier.await();  // Ensure all threads start at the same time
                if (rateLimiter.tryAcquire(TEST_CLIENT_KEY)) {
                    successfulRequests.incrementAndGet();
                }
            } catch (InterruptedException | BrokenBarrierException e) {
                fail("Test was interrupted");
            } finally {
                latch.countDown();
            }
        };

        IntStream.range(0, numberOfThreads).forEach(i -> executor.submit(task));

        latch.await();  // Wait for all threads to finish
        executor.shutdown();

        // Ensure the number of successful requests does not exceed the bucket capacity
        assertTrue(successfulRequests.get() <= capacity);
    }
}
