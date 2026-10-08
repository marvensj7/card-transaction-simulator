package com.marvens.capstone;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

abstract class SecurityTestSupport {
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void jwtSettings(DynamicPropertyRegistry properties) {
        properties.add("app.jwt.secret", () -> Base64.getEncoder().encodeToString(KEY));
        properties.add("app.auth.attempts-per-minute", () -> 1000);
    }

    static RequestPostProcessor identity(Long id, String role) {
        return jwt().jwt(builder -> builder.subject(id.toString()).claim("role", role))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
