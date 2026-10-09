package com.marvens.capstone.security;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationRateLimitFilterTest {
    @Test
    void onlyAuthenticationPostsCountAndRejectedAttemptsStayRejected() throws Exception {
        AuthenticationRateLimitFilter filter = new AuthenticationRateLimitFilter(new ObjectMapper(), 1);
        assertThat(attempt(filter, "GET", "/api/auth/login", "client")).isEqualTo(200);
        assertThat(attempt(filter, "POST", "/api/accounts/1/purchases", "client")).isEqualTo(200);
        assertThat(attempt(filter, "POST", "/api/auth/register", "client")).isEqualTo(200);
        for (int retry = 0; retry < 5; retry++) {
            assertThat(attempt(filter, "POST", "/api/auth/login", "client")).isEqualTo(429);
        }
        assertThat(attempt(filter, "POST", "/api/auth/login", "another-client")).isEqualTo(200);
        assertThatThrownBy(() -> new AuthenticationRateLimitFilter(new ObjectMapper(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void trackedAddressesAreBoundedAndExpiredWindowsReleaseSpace() throws Exception {
        AuthenticationRateLimitFilter filter = new AuthenticationRateLimitFilter(new ObjectMapper(), 1);
        for (int address = 0; address < 10000; address++) {
            assertThat(attempt(filter, "POST", "/api/auth/login", "client-" + address)).isEqualTo(200);
        }
        Map<?, ?> trackedAddresses = (Map<?, ?>) ReflectionTestUtils.getField(filter, "attemptsByAddress");
        assertThat(trackedAddresses).hasSize(10000);
        assertThat(attempt(filter, "POST", "/api/auth/login", "new-client")).isEqualTo(429);

        // Move the stored deadline into the past without making the test wait one minute.
        ReflectionTestUtils.setField(trackedAddresses.get("client-0"), "expiresAtMillis", 0L);
        assertThat(attempt(filter, "POST", "/api/auth/register", "new-client")).isEqualTo(200);
        assertThat(trackedAddresses).hasSize(10000);
        assertThat(trackedAddresses.containsKey("client-0")).isFalse();
    }

    @Test
    void concurrentRequestsShareOneProtectedAttemptCounter() throws Exception {
        AuthenticationRateLimitFilter filter = new AuthenticationRateLimitFilter(new ObjectMapper(), 10);
        CountDownLatch startRequests = new CountDownLatch(1);
        var requestWorkers = Executors.newFixedThreadPool(8);
        try {
            List<Future<Integer>> requestResults = new ArrayList<>();
            for (int requestNumber = 0; requestNumber < 30; requestNumber++) {
                requestResults.add(requestWorkers.submit(() -> {
                    startRequests.await();
                    return attempt(filter, "POST", "/api/auth/login", "same-client");
                }));
            }
            startRequests.countDown();
            int allowedRequests = 0;
            int rejectedRequests = 0;
            for (Future<Integer> requestResult : requestResults) {
                if (requestResult.get(10, TimeUnit.SECONDS) == 200) {
                    allowedRequests++;
                } else {
                    rejectedRequests++;
                }
            }
            assertThat(allowedRequests).isEqualTo(10);
            assertThat(rejectedRequests).isEqualTo(20);
        } finally {
            requestWorkers.shutdownNow();
        }
    }

    private int attempt(AuthenticationRateLimitFilter filter, String method, String path,
            String clientAddress) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(clientAddress);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (filteredRequest, filteredResponse) -> {});
        return response.getStatus();
    }
}
