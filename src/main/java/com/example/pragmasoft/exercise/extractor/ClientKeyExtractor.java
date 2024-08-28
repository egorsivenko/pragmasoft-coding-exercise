package com.example.pragmasoft.exercise.extractor;

import jakarta.servlet.http.HttpServletRequest;

/**
 * An interface for extracting a client-specific key from an HTTP request.
 * This key is typically used to identify the client making the request,
 * and can be based on IP address, headers, or any other relevant request data.
 *
 * @param <T> the type of the key that will be extracted from the request
 */
@FunctionalInterface
public interface ClientKeyExtractor<T> {

    /**
     * Extracts a client-specific key from the given HTTP request.
     *
     * @param request the HTTP request from which to extract the client key
     * @return the extracted client key
     */
    T extractClientKey(HttpServletRequest request);
}
