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
    private final Map<String, Window> attempts = new HashMap<>();
    private final ObjectMapper json;
    private final int limit;

    public AuthenticationRateLimitFilter(ObjectMapper json, int limit) {
        if (limit < 1) throw new IllegalArgumentException("Authentication limit must be positive.");
        this.json = json;
        this.limit = limit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !"POST".equals(request.getMethod())
                || !(path.equals("/api/auth/login") || path.equals("/api/auth/register"));
    }

    private synchronized boolean allow(String address) {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(entry -> now >= entry.getValue().endsAt);
        Window window = attempts.get(address);
        if (window == null) {
            // Bound memory even if many different clients contact the application.
            if (attempts.size() >= 10000) return false;
            window = new Window(now + 60000);
            attempts.put(address, window);
        }
        window.count++;
        return window.count <= limit;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        if (!allow(request.getRemoteAddr())) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            json.writeValue(response.getOutputStream(), Map.of("message", "Too many sign-in attempts. Try again in a minute."));
            return;
        }
        chain.doFilter(request, response);
    }

    private static class Window {
        final long endsAt;
        int count;
        Window(long endsAt) { this.endsAt = endsAt; }
    }
}
