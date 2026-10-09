package com.marvens.capstone;

import java.time.Instant;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvens.capstone.controller.AccountController;
import com.marvens.capstone.entity.AppUser;
import com.marvens.capstone.security.AuthenticationRateLimitFilter;
import com.marvens.capstone.security.JwtTokens;
import com.marvens.capstone.security.SecurityConfiguration;
import com.marvens.capstone.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Import({SecurityConfiguration.class, JwtTokens.class})
class SecurityConfigurationTest extends SecurityTestSupport {
    @Autowired MockMvc mvc;
    @Autowired JwtTokens tokens;
    @Autowired JwtEncoder encoder;
    @Autowired JwtDecoder decoder;
    @Autowired ObjectMapper json;
    @MockitoBean AccountService accounts;

    @Test
    void signedTokenCarriesOnlyIntendedIdentityAndExpires() throws Exception {
        AppUser user = TestData.user(AppUser.Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        Jwt token = tokens.issueAccessToken(user);
        Jwt verified = decoder.decode(token.getTokenValue());
        assertThat(verified.getSubject()).isEqualTo("1");
        assertThat(verified.getAudience()).containsExactly("credit-circuit-api");
        assertThat(verified.getClaimAsString("role")).isEqualTo("USER");
        assertThat(verified.getClaims()).doesNotContainKeys("password", "email", "passwordHash");
        mvc.perform(get("/api/accounts").header("Authorization", "Bearer " + token.getTokenValue()))
                .andExpect(status().isOk()).andExpect(header().doesNotExist("Set-Cookie"));
        String[] parts = token.getTokenValue().split("\\.");
        String altered = parts[0] + "." + parts[1] + "." + (parts[2].startsWith("A") ? "B" : "A") + parts[2].substring(1);
        mvc.perform(get("/api/accounts").header("Authorization", "Bearer " + altered))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/accounts").sessionAttr("userId", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongIssuerAudienceRoleSubjectAndExpiredClaimsAreRejected() {
        Instant now = Instant.now();
        for (String field : List.of("iss", "aud", "role", "sub", "exp", "iat")) {
            JwtClaimsSet.Builder claims = JwtClaimsSet.builder().issuer("credit-circuit")
                    .audience(List.of("credit-circuit-api")).subject("1").claim("role", "USER")
                    .issuedAt(now.minusSeconds(120)).expiresAt(now.plusSeconds(600));
            if (field.equals("iss")) {
                claims.issuer("another-app");
            }
            if (field.equals("aud")) {
                claims.audience(List.of("another-api"));
            }
            if (field.equals("role")) {
                claims.claim("role", "SUPERUSER");
            }
            if (field.equals("sub")) {
                claims.subject("-1");
            }
            if (field.equals("exp")) {
                claims.expiresAt(now.minusSeconds(1));
            }
            if (field.equals("iat")) {
                claims.issuedAt(now.plusSeconds(60));
            }
            String value = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
            assertThatThrownBy(() -> decoder.decode(value)).isInstanceOf(JwtException.class);
        }
    }

    @Test
    void missingRequiredClaimsReturnUnauthorizedBeforeReachingTheService() throws Exception {
        Instant now = Instant.now();
        for (String field : List.of("iss", "aud", "role", "sub", "exp", "iat")) {
            JwtClaimsSet.Builder claims = JwtClaimsSet.builder().issuer("credit-circuit")
                    .audience(List.of("credit-circuit-api")).subject("1").claim("role", "USER")
                    .issuedAt(now.minusSeconds(120)).expiresAt(now.plusSeconds(600));
            claims.claims(values -> values.remove(field));
            String value = encoder.encode(JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
            mvc.perform(get("/api/accounts").header("Authorization", "Bearer " + value))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(accounts);
    }

    @Test
    void corsAcceptsOnlyConfiguredOriginsAndNoCookieCredentials() throws Exception {
        mvc.perform(options("/api/accounts").header("Origin", "http://127.0.0.1:5173")
                        .header("Access-Control-Request-Method", "GET").header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        mvc.perform(options("/api/accounts").header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET")).andExpect(status().isForbidden());
    }

    @Test
    void authenticationLimitCountsBothOperationsAndIgnoresSpoofedForwardedHeaders() throws Exception {
        AuthenticationRateLimitFilter filter = new AuthenticationRateLimitFilter(json, 2);
        for (int attempt = 0; attempt < 3; attempt++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST",
                    attempt == 1 ? "/api/auth/register" : "/api/auth/login");
            request.addHeader("X-Forwarded-For", "10.0.0." + attempt);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {});
            assertThat(response.getStatus()).isEqualTo(attempt < 2 ? 200 : 429);
            if (attempt == 2) {
                assertThat(response.getHeader("Retry-After")).isEqualTo("60");
            }
        }
    }
}
