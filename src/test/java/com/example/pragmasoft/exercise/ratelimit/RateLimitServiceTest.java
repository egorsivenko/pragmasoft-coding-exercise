package com.example.pragmasoft.exercise.ratelimit;

import com.example.pragmasoft.exercise.extractor.RequestNetworkClientKeyExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitServiceTest {

    private RateLimitService rateLimitService;
    private RequestNetworkClientKeyExtractor keyExtractor;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        keyExtractor = mock(RequestNetworkClientKeyExtractor.class);
        rateLimitService = new RateLimitService(keyExtractor);
        request = mock(HttpServletRequest.class);
    }

    @Test
    void shouldAllowRequestWhenTokensAvailable() {
        when(keyExtractor.extractClientKey(request)).thenReturn("127.0.0.1");

        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitService.isAllowed(request));
        }
    }

    @Test
    void shouldBlockRequestWhenTokensDepleted() {
        when(keyExtractor.extractClientKey(request)).thenReturn("127.0.0.1");

        for (int i = 0; i < 10; i++) {
            rateLimitService.isAllowed(request);
        }
        assertFalse(rateLimitService.isAllowed(request));
    }

    @Test
    void shouldRefillTokensAfterRefillPeriod() throws InterruptedException {
        when(keyExtractor.extractClientKey(request)).thenReturn("127.0.0.1");

        for (int i = 0; i < 10; i++) {
            rateLimitService.isAllowed(request);
        }
        Thread.sleep(10000); // Simulate waiting for the refill period

        assertTrue(rateLimitService.isAllowed(request));
    }
}
