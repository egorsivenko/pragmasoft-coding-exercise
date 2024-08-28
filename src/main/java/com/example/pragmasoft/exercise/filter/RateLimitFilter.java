package com.example.pragmasoft.exercise.filter;

import com.example.pragmasoft.exercise.extractor.ClientKeyExtractor;
import com.example.pragmasoft.exercise.ratelimiter.RateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zalando.problem.ThrowableProblem;

import java.io.IOException;
import java.util.Objects;

/**
 * Filter that applies rate limiting to incoming requests using the {@link RateLimiter}.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter<String> rateLimiter;
    private final ClientKeyExtractor<String> keyExtractor;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimiter<String> rateLimiter,
                           ClientKeyExtractor<String> keyExtractor,
                           ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.keyExtractor = keyExtractor;
        this.objectMapper = objectMapper;
    }

    /**
     * Filters requests to apply rate limiting, responding with 429 status code if the limit is exceeded.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientKey = keyExtractor.extractClientKey(request);
        try {
            rateLimiter.tryAcquire(clientKey);
            filterChain.doFilter(request, response);
        } catch (ThrowableProblem problem) {
            response.setStatus(Objects.requireNonNull(problem.getStatus()).getStatusCode());
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(problem.getParameters().get("wait_seconds")));

            response.getWriter().write(objectMapper.writeValueAsString(problem));
            response.getWriter().flush();
        }
    }
}
