package com.example.pragmasoft.exercise.ratelimit;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter that applies rate limiting to incoming requests using the {@link RateLimitService}.
 */
@Component
public class RateLimitFilter implements Filter {

    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    /**
     * Filters requests to apply rate limiting, responding with 429 status code if the limit is exceeded.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (rateLimitService.isAllowed((HttpServletRequest) request)) {
            chain.doFilter(request, response);
        } else {
            HttpServletResponse resp = (HttpServletResponse) response;
            resp.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Request rejected. Cause: rate limit exceeded");
        }
    }
}
