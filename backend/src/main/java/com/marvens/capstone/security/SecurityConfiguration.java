package com.marvens.capstone.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecretKey signingKey(@Value("${app.jwt.secret}") String encodedSigningKey) {
        byte[] signingKeyBytes;
        try {
            signingKeyBytes = Base64.getDecoder().decode(encodedSigningKey);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("JWT key must be Base64.");
        }
        if (signingKeyBytes.length < 32) {
            throw new IllegalArgumentException("JWT key needs at least 32 random bytes.");
        }
        return new SecretKeySpec(signingKeyBytes, "HmacSHA256");
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey signingKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(signingKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey signingKey, @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(signingKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Spring verifies the signature, expiry and issuer. These checks constrain our identity claims.
        OAuth2TokenValidator<Jwt> identityClaimsValidator = jwt -> {
            String subject = jwt.getSubject();
            String role = jwt.getClaimAsString("role");
            Instant issuedAt = jwt.getIssuedAt();
            Instant expiresAt = jwt.getExpiresAt();
            boolean validSubject = subject != null && subject.matches("[1-9]\\d{0,17}");
            boolean validRole = "USER".equals(role) || "ADMIN".equals(role);
            boolean validAudience = jwt.getAudience() != null && jwt.getAudience().equals(List.of(audience));
            boolean validIssueTime = issuedAt != null && !issuedAt.isAfter(Instant.now());
            boolean validExpiration = expiresAt != null && issuedAt != null && expiresAt.isAfter(issuedAt);
            if (validSubject && validRole && validAudience && validIssueTime && validExpiration) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid access token claims.", null));
        };
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO), new JwtIssuerValidator(issuer), identityClaimsValidator));
        return jwtDecoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.origins}") List<String> allowedOrigins) {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(allowedOrigins);
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        corsConfiguration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        corsConfiguration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource corsSource = new UrlBasedCorsConfigurationSource();
        corsSource.registerCorsConfiguration("/api/**", corsConfiguration);
        return corsSource;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder, ObjectMapper objectMapper,
            @Value("${app.auth.attempts-per-minute}") int maxAttemptsPerMinute) throws Exception {
        JwtGrantedAuthoritiesConverter roleAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        roleAuthoritiesConverter.setAuthoritiesClaimName("role");
        roleAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(roleAuthoritiesConverter);

        http.cors(cors -> {})
                // API identity comes only from an explicitly attached Authorization header.
                // Cookie, session, Basic and form authentication are disabled.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(authorization -> authorization
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/accounts/**", "/api/transactions/**").hasRole("USER")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(authenticationConverter))
                        .authenticationEntryPoint((request, response, failure) -> {
                            response.setStatus(401);
                            response.setHeader("WWW-Authenticate", "Bearer");
                            response.setContentType("application/json");
                            objectMapper.writeValue(response.getOutputStream(),
                                    Map.of("message", "Sign in to continue."));
                        })
                        .accessDeniedHandler((request, response, failure) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            objectMapper.writeValue(response.getOutputStream(),
                                    Map.of("message", "This user role cannot perform this operation."));
                        }))
                .addFilterBefore(new AuthenticationRateLimitFilter(objectMapper, maxAttemptsPerMinute),
                        BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
