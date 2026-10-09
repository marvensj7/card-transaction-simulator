package com.marvens.capstone.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

// A local, single-process limit. Never trust a caller-supplied forwarded IP header.
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_TRACKED_ADDRESSES = 10000;
    private static final long ATTEMPT_WINDOW_MILLIS = 60000;
    private final Map<String, AuthenticationAttemptWindow> attemptsByAddress = new HashMap<>();
    private final ObjectMapper objectMapper;
    private final int maxAttemptsPerMinute;

    public AuthenticationRateLimitFilter(ObjectMapper objectMapper, int maxAttemptsPerMinute) {
        if (maxAttemptsPerMinute < 1) {
            throw new IllegalArgumentException("Authentication limit must be positive.");
        }
        this.objectMapper = objectMapper;
        this.maxAttemptsPerMinute = maxAttemptsPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI().substring(request.getContextPath().length());
        return !"POST".equals(request.getMethod())
                || !(requestPath.equals("/api/auth/login") || requestPath.equals("/api/auth/register"));
    }

    // Checking and updating the shared map must happen together, even for simultaneous requests.
    private synchronized boolean recordAuthenticationAttempt(String clientAddress) {
        long currentTimeMillis = System.currentTimeMillis();
        attemptsByAddress.entrySet().removeIf(entry -> currentTimeMillis >= entry.getValue().expiresAtMillis);
        AuthenticationAttemptWindow attemptWindow = attemptsByAddress.get(clientAddress);
        if (attemptWindow == null) {
            if (attemptsByAddress.size() >= MAX_TRACKED_ADDRESSES) {
                return false;
            }
            attemptWindow = new AuthenticationAttemptWindow(currentTimeMillis + ATTEMPT_WINDOW_MILLIS);
            attemptsByAddress.put(clientAddress, attemptWindow);
        }
        // Stop at the limit so repeated rejected calls cannot overflow the counter.
        if (attemptWindow.attemptCount >= maxAttemptsPerMinute) {
            return false;
        }
        attemptWindow.attemptCount++;
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!recordAuthenticationAttempt(request.getRemoteAddr())) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            objectMapper.writeValue(response.getOutputStream(),
                    Map.of("message", "Too many sign-in attempts. Try again in a minute."));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static class AuthenticationAttemptWindow {
        final long expiresAtMillis;
        int attemptCount;

        AuthenticationAttemptWindow(long expiresAtMillis) {
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
