package com.marvens.capstone.controller;

import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvens.capstone.controller.dto.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/** Fail closed before MVC reads input. JWT verification does not exist yet. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class ApiIdentityFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ApiIdentityFilter.class);
    private final ObjectMapper json;

    public ApiIdentityFilter(ObjectMapper json) { this.json = json; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Mock requests may not have a servlet path; real requests exclude the context path.
        if (path.isEmpty()) {
            path = request.getRequestURI().substring(request.getContextPath().length());
        }
        return !path.equals("/api") && !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!(request.getUserPrincipal() instanceof AuthenticatedUser)) {
            log.info("API rejection: status=401 code=AUTHENTICATION_REQUIRED route=/api/** user=anonymous");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            json.writeValue(response.getOutputStream(), ApiError.authenticationRequired());
            return;
        }
        chain.doFilter(request, response);
    }
}
