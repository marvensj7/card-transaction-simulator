package com.marvens.capstone.security;

import java.io.IOException;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Sign-in is unfinished. Until it sets a server session, protected requests stay closed.
@Component
public class SessionAccessFilter extends OncePerRequestFilter {
    private final ObjectMapper json;

    public SessionAccessFilter(ObjectMapper json) {
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.equals("/api") && !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Object userId = null;
        if (session != null) {
            userId = session.getAttribute("userId");
        }
        if (!(userId instanceof Long) || (Long) userId < 1) {
            response.setStatus(401);
            response.setContentType("application/json");
            json.writeValue(response.getOutputStream(), Map.of("message", "Sign in to continue."));
            return;
        }
        chain.doFilter(request, response);
    }
}
