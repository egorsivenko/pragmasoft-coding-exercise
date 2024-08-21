package com.example.pragmasoft.exercise.extractor;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link ClientKeyExtractor} that extracts the client key
 * based on the network address (IP address) of the request.
 */
@Component
public class RequestNetworkClientKeyExtractor implements ClientKeyExtractor<String> {

    /**
     * Extracts the client key based on the remote IP address of the HTTP request.
     *
     * @param request the HTTP request from which to extract the client key
     * @return the remote IP address of the client making the request
     */
    @Override
    public String extractClientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
