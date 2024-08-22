package com.example.pragmasoft.exercise.extractor;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.StringTokenizer;

/**
 * Implementation of {@link ClientKeyExtractor} interface that is responsible
 * for determining the client's IP address from the request header or the remote address.
 * <p>
 * The IP address is extracted in the following order:
 * <ol>
 *     <li>If the "X-Forwarded-For" header is present, the first (client's) IP address in the list is used.</li>
 *     <li>If the "X-Forwarded-For" header is absent, the remote address of the request is used.</li>
 * </ol>
 */
@Component
public class RequestHeaderClientKeyExtractor implements ClientKeyExtractor<String> {

    /**
     * Extracts the client IP address from the given {@link HttpServletRequest}.
     * <p>
     * The method first checks the "X-Forwarded-For" header to see if it contains any IP addresses.
     * If the header is present, the first IP address in the comma-separated list is returned.
     * If the header is absent or blank, the method returns the remote address of the request.
     *
     * @param request the {@link HttpServletRequest} from which to extract the client IP address
     * @return the client IP address as a {@link String}
     */
    @Override
    public String extractClientKey(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null || xForwardedForHeader.isBlank()) {
            return request.getRemoteAddr();
        } else {
            return new StringTokenizer(xForwardedForHeader, ",").nextToken().strip();
        }
    }
}
