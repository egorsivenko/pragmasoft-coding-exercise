package com.example.pragmasoft.exercise.ratelimit;

import com.example.pragmasoft.exercise.extractor.RequestHeaderClientKeyExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class RateLimitServiceTest {

    private RateLimitService rateLimitService;
    private RequestHeaderClientKeyExtractor keyExtractor;
    private HttpServletRequest request;

    @Value("${rate.limit.capacity}")
    private int capacity;

    @Value("${rate.limit.refillPeriod}")
    private long refillPeriod;

    @Value("${rate.limit.tokensPerPeriod}")
    private int tokensPerPeriod;

    @Value("${rate.limit.expirationTime}")
    private long expirationTime;

    @BeforeEach
    void setUp() {
        keyExtractor = mock(RequestHeaderClientKeyExtractor.class);
        rateLimitService = new RateLimitService(keyExtractor, capacity, refillPeriod, tokensPerPeriod, expirationTime);
        request = mock(HttpServletRequest.class);
    }

    @Test
    void shouldAllowRequestWhenTokensAvailable() {
        when(keyExtractor.extractClientKey(request)).thenReturn("127.0.0.1");

        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimitService.isAllowed(request));
        }
    }

    @Test
    void shouldBlockRequestWhenTokensDepleted() {
        when(keyExtractor.extractClientKey(request)).thenReturn("127.0.0.1");

        for (int i = 0; i < capacity; i++) {
            rateLimitService.isAllowed(request);
        }
        assertFalse(rateLimitService.isAllowed(request));
    }

    @Test
    void shouldRefillTokensAfterRefillPeriod() throws InterruptedException {
        when(keyExtractor.extractClientKey(request)).thenReturn("127.0.0.1");

        for (int i = 0; i < capacity; i++) {
            rateLimitService.isAllowed(request);
        }
        Thread.sleep(refillPeriod); // Simulate waiting for the refill period

        for (int i = 0; i < tokensPerPeriod; i++) {
            assertTrue(rateLimitService.isAllowed(request));
        }
    }

    @Test
    void shouldBlockRequestWhenTokensDepletedAfterRefill() throws InterruptedException {
        when(keyExtractor.extractClientKey(request)).thenReturn("127.0.0.1");

        for (int i = 0; i < capacity; i++) {
            rateLimitService.isAllowed(request);
        }
        Thread.sleep(refillPeriod); // Simulate waiting for the refill period

        for (int i = 0; i < tokensPerPeriod; i++) {
            rateLimitService.isAllowed(request);
        }
        assertFalse(rateLimitService.isAllowed(request));
    }
}
