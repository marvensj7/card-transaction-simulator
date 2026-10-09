package com.marvens.capstone.security;

import java.time.Instant;
import java.util.List;
import com.marvens.capstone.entity.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokens {
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final String audience;
    private final long lifetimeSeconds;

    public JwtTokens(JwtEncoder jwtEncoder, @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience,
            @Value("${app.jwt.lifetime-seconds}") long lifetimeSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.audience = audience;
        if (lifetimeSeconds < 1 || lifetimeSeconds > 3600) {
            throw new IllegalArgumentException("Access token lifetime must be 1 to 3600 seconds.");
        }
        this.lifetimeSeconds = lifetimeSeconds;
    }

    public Jwt issueAccessToken(AppUser user) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet identityClaims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(lifetimeSeconds))
                .claim("role", user.getRole().name())
                .build();
        JwsHeader signingHeader = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(signingHeader, identityClaims));
    }
}
