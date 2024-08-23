package com.example.pragmasoft.exercise.ratelimit;

import com.example.pragmasoft.exercise.extractor.RequestHeaderClientKeyExtractor;
import jakarta.servlet.http.HttpServletRequest;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class RateLimitServiceTest {

    private RateLimitService rateLimitService;
    private HttpServletRequest request;

    @Value("${rate.limit.capacity}")
    private int capacity;

    @Value("${rate.limit.refillPeriod}")
    private long refillPeriod;

    @Value("${rate.limit.expirationTime}")
    private long expirationTime;

    @BeforeEach
    void setUp() {
        RequestHeaderClientKeyExtractor keyExtractor = mock(RequestHeaderClientKeyExtractor.class);
        rateLimitService = new RateLimitService(keyExtractor, capacity, refillPeriod, expirationTime);
        request = mock(HttpServletRequest.class);
        when(keyExtractor.extractClientKey(request)).thenReturn("127.0.0.1");
    }

    @Test
    void shouldAllowRequestWhenTokensAvailable() {
        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimitService.isAllowed(request));
        }
    }

    @Test
    void shouldBlockRequestWhenTokensDepleted() {
        for (int i = 0; i < capacity; i++) {
            rateLimitService.isAllowed(request);
        }
        assertFalse(rateLimitService.isAllowed(request));
    }

    @Test
    void shouldRefillTokensAfterRefillPeriod() throws InterruptedException {
        for (int i = 0; i < capacity; i++) {
            rateLimitService.isAllowed(request);
        }
        Thread.sleep(refillPeriod); // Simulate waiting for the refill period

        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimitService.isAllowed(request));
        }
    }

    @Test
    void shouldBlockRequestWhenTokensDepletedAfterRefill() throws InterruptedException {
        for (int i = 0; i < capacity; i++) {
            rateLimitService.isAllowed(request);
        }
        Thread.sleep(refillPeriod); // Simulate waiting for the refill period

        for (int i = 0; i < capacity; i++) {
            rateLimitService.isAllowed(request);
        }
        assertFalse(rateLimitService.isAllowed(request));
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
                assertTrue(rateLimitService.isAllowed(request));
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
                if (rateLimitService.isAllowed(request)) {
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
