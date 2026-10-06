package com.marvens.capstone.controller;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.*;

class ApiIdentityFilterTest {
    private final ApiIdentityFilter filter = new ApiIdentityFilter();

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
            assertThat(response.getContentAsString()).isEmpty();
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
        assertThatThrownBy(() -> CurrentUser.id(request.getUserPrincipal()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
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
        assertThat(CurrentUser.id(request.getUserPrincipal())).isEqualTo(9L);
        assertThatThrownBy(() -> new AuthenticatedUser(0L)).isInstanceOf(IllegalArgumentException.class);
    }
}
