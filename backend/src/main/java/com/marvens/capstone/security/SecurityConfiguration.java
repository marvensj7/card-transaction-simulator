package com.marvens.capstone.security;

import java.time.Duration;
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
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
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
    SecretKey signingKey(@Value("${app.jwt.secret}") String encoded) {
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(encoded); }
        catch (IllegalArgumentException failure) { throw new IllegalArgumentException("JWT key must be Base64."); }
        if (bytes.length < 32) throw new IllegalArgumentException("JWT key needs at least 32 random bytes.");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key, @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> claims = jwt -> {
            String subject = jwt.getSubject();
            String role = jwt.getClaimAsString("role");
            boolean valid = subject != null && subject.matches("[1-9]\\d{0,17}")
                    && ("USER".equals(role) || "ADMIN".equals(role))
                    && jwt.getAudience().equals(List.of(audience))
                    && jwt.getIssuedAt() != null && !jwt.getIssuedAt().isAfter(java.time.Instant.now())
                    && jwt.getExpiresAt() != null && jwt.getIssuedAt() != null
                    && jwt.getExpiresAt().isAfter(jwt.getIssuedAt());
            if (valid) return OAuth2TokenValidatorResult.success();
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid access token claims.", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO), new JwtIssuerValidator(issuer), claims));
        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.origins}") List<String> origins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cors.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, JwtDecoder decoder, ObjectMapper json,
            @Value("${app.auth.attempts-per-minute}") int attempts) throws Exception {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("role");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        http.cors(cors -> {})
                // API identity comes only from an explicitly attached Authorization header.
                // No session, authentication cookie, Basic login, or automatically sent credential is accepted.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).logout(logout -> logout.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/accounts/**", "/api/transactions/**").hasRole("USER")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(decoder).jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint((request, response, failure) -> {
                            response.setStatus(401);
                            response.setHeader("WWW-Authenticate", "Bearer");
                            response.setContentType("application/json");
                            json.writeValue(response.getOutputStream(), Map.of("message", "Sign in to continue."));
                        }).accessDeniedHandler((request, response, failure) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            json.writeValue(response.getOutputStream(), Map.of("message", "This user role cannot perform this operation."));
                        }))
                .addFilterBefore(new AuthenticationRateLimitFilter(json, attempts), BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
