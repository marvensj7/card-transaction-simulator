package com.marvens.capstone.security;

import java.time.Instant;
import java.util.List;
import com.marvens.capstone.entity.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class JwtTokens {
    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;
    private final long lifetimeSeconds;

    public JwtTokens(JwtEncoder encoder, @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience,
            @Value("${app.jwt.lifetime-seconds}") long lifetimeSeconds) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.audience = audience;
        if (lifetimeSeconds < 1 || lifetimeSeconds > 3600) {
            throw new IllegalArgumentException("Access token lifetime must be 1 to 3600 seconds.");
        }
        this.lifetimeSeconds = lifetimeSeconds;
    }

    public Jwt issue(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).audience(List.of(audience))
                .subject(user.getId().toString()).issuedAt(now).expiresAt(now.plusSeconds(lifetimeSeconds))
                .claim("role", user.getRole().name()).build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims));
    }
}
