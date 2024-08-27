package com.example.pragmasoft.exercise.ratelimit;

import com.example.pragmasoft.exercise.extractor.ClientKeyExtractor;
import com.example.pragmasoft.exercise.ratelimit.dto.RateLimitError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Filter that applies rate limiting to incoming requests using the {@link RateLimiter}.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter<String> rateLimiter;
    private final ClientKeyExtractor<String> keyExtractor;
    private final ObjectMapper objectMapper;

    private final long capacity;
    private final long refillPeriod;

    public RateLimitFilter(RateLimiter<String> rateLimiter,
                           ClientKeyExtractor<String> keyExtractor,
                           ObjectMapper objectMapper,
                           @Value("${rate.limit.capacity}") long capacity,
                           @Value("${rate.limit.refillPeriod}") long refillPeriod) {
        this.rateLimiter = rateLimiter;
        this.keyExtractor = keyExtractor;
        this.objectMapper = objectMapper;
        this.capacity = capacity;
        this.refillPeriod = refillPeriod;
    }

    /**
     * Filters requests to apply rate limiting, responding with 429 status code if the limit is exceeded.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientKey = keyExtractor.extractClientKey(request);

        if (rateLimiter.tryAcquire(clientKey)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");

            long secondsToWait = TimeUnit.MILLISECONDS.toSeconds(refillPeriod);
            response.setHeader("Retry-After", String.valueOf(secondsToWait));

            RateLimitError error = new RateLimitError(
                    "rate_limit_exceeded",
                    String.format(
                            "Exceeded the maximum number of requests - %d requests per %d seconds. Try again later.",
                            capacity, secondsToWait
                    )
            );
            response.getWriter().write(objectMapper.writeValueAsString(error));
            response.getWriter().flush();
        }
    }
}
