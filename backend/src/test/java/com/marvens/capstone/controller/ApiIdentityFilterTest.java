package com.marvens.capstone.controller;

import com.marvens.capstone.security.AuthenticatedUser;
import com.marvens.capstone.security.ApiIdentityFilter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.Instant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.*;

class ApiIdentityFilterTest {
    private final ObjectMapper json = new ObjectMapper();
    private final ApiIdentityFilter filter = new ApiIdentityFilter(json);

    @Test
    void callerSuppliedIdsRolesAndBearerTextCannotOpenTheApi() throws Exception {
        for (String path : new String[] {"/api/accounts", "/api/admin/accounts", "/api/accounts/7/purchases"}) {
            var request = new MockHttpServletRequest("POST", path);
            request.addHeader("X-User-Id", "1");
            request.addHeader("X-Role", "ADMIN");
            request.addHeader("Authorization", "Bearer unverified-test-token");
            request.addParameter("userId", "1");
            request.setContent("{\"userId\":1,\"role\":\"ADMIN\"}".getBytes());
            var response = new MockHttpServletResponse();
            var called = new AtomicBoolean();
            filter.doFilter(request, response, (req, res) -> called.set(true));
            assertThat(response.getStatus()).isEqualTo(401);
            var error = json.readTree(response.getContentAsString());
            assertThat(error.size()).isEqualTo(4);
            assertThat(error.path("status").asInt()).isEqualTo(401);
            assertThat(error.path("code").asText()).isEqualTo("AUTHENTICATION_REQUIRED");
            assertThat(error.path("message").asText()).isEqualTo("Authentication is required.");
            assertThat(error.path("timestamp").asText()).endsWith("Z");
            assertThat(Instant.parse(error.path("timestamp").asText())).isBeforeOrEqualTo(Instant.now());
            assertThat(response.getContentType()).startsWith("application/json");
            assertThat(response.getContentAsString()).doesNotContain("unverified-test-token", "userId", "ADMIN");
            assertThat(called).isFalse();
        }
    }

    @Test
    void numericPrincipalNameAloneIsNotTrusted() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/accounts");
        request.setUserPrincipal(() -> "1");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Unexpected controller access"));
        assertThat(response.getStatus()).isEqualTo(401);
        assertThatThrownBy(() -> AuthenticatedUser.id(request.getUserPrincipal()))
                .isInstanceOf(com.marvens.capstone.exception.AuthenticationRequiredException.class);
    }

    @Test
    void serverEstablishedPrincipalSuppliesTheServiceIdentity() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/accounts");
        request.setContextPath("/simulator");
        request.setRequestURI("/simulator/api/accounts");
        request.setUserPrincipal(new AuthenticatedUser(9L));
        request.addHeader("X-User-Id", "1");
        var called = new AtomicBoolean();
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> called.set(true));
        assertThat(called).isTrue();
        assertThat(AuthenticatedUser.id(request.getUserPrincipal())).isEqualTo(9L);
        assertThatThrownBy(() -> new AuthenticatedUser(0L)).isInstanceOf(IllegalArgumentException.class);
    }
}
