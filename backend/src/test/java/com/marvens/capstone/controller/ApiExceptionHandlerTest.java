package com.marvens.capstone.controller;

import java.time.Instant;
import java.util.stream.Stream;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvens.capstone.exception.*;
import com.marvens.capstone.service.AccountService;
import com.marvens.capstone.service.TransactionService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AccountController.class, TransactionController.class, AdminController.class})
@ExtendWith(OutputCaptureExtension.class)
class ApiExceptionHandlerTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockitoBean private TransactionService transactions;
    @MockitoBean private AccountService accounts;
    private static final String SECRET = "fictional-secret-marker";

    static Stream<Arguments> serviceErrors() {
        return Stream.of(
                Arguments.of(new AuthenticationRequiredException(), 401, "AUTHENTICATION_REQUIRED"),
                Arguments.of(new InvalidPurchaseException(SECRET), 400, "INVALID_PURCHASE"),
                Arguments.of(new InvalidRequestException(SECRET), 400, "INVALID_REQUEST"),
                Arguments.of(new AccessDeniedException(), 403, "ACCESS_DENIED"),
                Arguments.of(new ResourceNotFoundException(SECRET), 404, "RESOURCE_NOT_FOUND"),
                Arguments.of(new RequestConflictException(), 409, "REQUEST_CONFLICT"),
                Arguments.of(new RefundNotEligibleException(SECRET), 409, "REFUND_NOT_ELIGIBLE"));
    }

    @Test
    void filterReturnsSafeJsonBeforeMvcReadsMalformedInput(CapturedOutput output) throws Exception {
        String body = mvc.perform(post("/api/accounts/7/purchases").header("X-User-Id", "9")
                        .header("X-Role", "ADMIN").header("Authorization", "Bearer " + SECRET)
                        .param("userId", "9").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testCardNumber\":\"" + RequestDtoTest.testNumber() + "\",\"testSecurityCode\":\"" + SECRET))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        assertError(body, 401, "AUTHENTICATION_REQUIRED");
        assertThat(body).doesNotContain(SECRET, RequestDtoTest.testNumber(), "ADMIN", "userId");
        assertThat(output.getAll()).contains("status=401 code=AUTHENTICATION_REQUIRED route=/api/** user=anonymous")
                .doesNotContain(SECRET, RequestDtoTest.testNumber());
        verifyNoInteractions(accounts, transactions);
    }

    @Test
    void mvcIdentityGuardUsesTheSameErrorIfTheFilterIsAbsent() throws Exception {
        var withoutFilter = MockMvcBuilders.standaloneSetup(new AccountController(accounts))
                .setControllerAdvice(new ApiExceptionHandler()).build();
        String body = withoutFilter.perform(get("/api/accounts").principal(() -> "9"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertError(body, 401, "AUTHENTICATION_REQUIRED");
        assertThat(json.readTree(body).path("message").asText()).isEqualTo("Authentication is required.");
        verifyNoInteractions(accounts, transactions);
    }

    @Test
    void unexpectedFailureHasGenericJsonAndLogsOnlySafeContext(CapturedOutput output) throws Exception {
        String details = SECRET + " " + RequestDtoTest.testNumber() + " " + RequestDtoTest.testCode()
                + " password-marker token-marker database-credential-marker sql-parameter-marker";
        var failure = new IllegalStateException(details, new RuntimeException(details));
        when(transactions.purchase(eq(9L), eq(7L), any())).thenThrow(failure);
        String body = mvc.perform(post("/api/accounts/7/purchases").principal(new AuthenticatedUser(9L))
                        .header("Authorization", "Bearer token-marker").param("debug", "password-marker")
                        .contentType(MediaType.APPLICATION_JSON).content(RequestDtoTest.validJson()))
                .andExpect(status().isInternalServerError()).andReturn().getResponse().getContentAsString();
        assertError(body, 500, "INTERNAL_ERROR");
        assertThat(json.readTree(body).path("message").asText())
                .isEqualTo("An unexpected error occurred. Please try again later.");
        assertThat(body).doesNotContain("IllegalStateException", "RuntimeException", "TransactionService", "stackTrace");
        assertThat(output.getAll()).contains("ERROR", "API failure: status=500", "method=POST",
                "route=/api/accounts/{accountId}/purchases", "user=9", "type=java.lang.IllegalStateException",
                "TransactionService.purchase(TransactionService.java:");
        for (String secret : new String[] {SECRET, RequestDtoTest.testNumber(), "password-marker", "token-marker",
                "database-credential-marker", "sql-parameter-marker"}) {
            assertThat(body).doesNotContain(secret);
            assertThat(output.getAll()).doesNotContain(secret);
        }
        assertThat(json.readTree(body).path("message").asText()).doesNotContain(RequestDtoTest.testCode());
    }

    @Test
    void frameworkErrorsKeepTheirStatusesAndUseSafeJson() throws Exception {
        String missing = mvc.perform(get("/api/unavailable").principal(new AuthenticatedUser(9L)))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        assertError(missing, 404, "RESOURCE_NOT_FOUND");
        String method = mvc.perform(delete("/api/accounts").principal(new AuthenticatedUser(9L)))
                .andExpect(status().isMethodNotAllowed()).andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("GET")))
                .andReturn().getResponse().getContentAsString();
        assertError(method, 405, "METHOD_NOT_ALLOWED");
        String media = mvc.perform(post("/api/accounts/7/purchases").principal(new AuthenticatedUser(9L))
                        .contentType(MediaType.TEXT_PLAIN).content(SECRET))
                .andExpect(status().isUnsupportedMediaType()).andReturn().getResponse().getContentAsString();
        assertError(media, 415, "UNSUPPORTED_MEDIA_TYPE");
        assertThat(media).doesNotContain(SECRET);
        verifyNoInteractions(accounts, transactions);
        when(accounts.getAccounts(9L)).thenReturn(List.of(ControllerFixtures.account()));
        String accept = mvc.perform(get("/api/accounts").principal(new AuthenticatedUser(9L)).accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable()).andReturn().getResponse().getContentAsString();
        assertError(accept, 406, "NOT_ACCEPTABLE");
    }

    @ParameterizedTest
    @MethodSource("serviceErrors")
    void serviceFailuresHaveSafeConsistentErrors(RuntimeException failure, int status, String code,
                                                 CapturedOutput output) throws Exception {
        when(transactions.purchase(eq(9L), eq(7L), any())).thenThrow(failure);
        String body = mvc.perform(post("/api/accounts/7/purchases").principal(new AuthenticatedUser(9L))
                        .contentType(MediaType.APPLICATION_JSON).content(RequestDtoTest.validJson()))
                .andExpect(status().is(status)).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        assertError(body, status, code);
        assertThat(body).doesNotContain(failure.getClass().getSimpleName(), SECRET,
                RequestDtoTest.testNumber(), "testCardNumber", "testSecurityCode", "stackTrace");
        // A three-digit code can coincidentally occur in timestamp fractions.
        assertThat(json.readTree(body).path("message").asText()).doesNotContain(RequestDtoTest.testCode());
        assertThat(output.getAll()).contains("status=" + status, "code=" + code,
                "route=/api/accounts/{accountId}/purchases", "user=9")
                .doesNotContain(SECRET, RequestDtoTest.testNumber(), "testSecurityCode");
    }

    private void assertError(String body, int status, String code) throws Exception {
        var error = json.readTree(body);
        assertThat(error.size()).isEqualTo(4);
        assertThat(error.path("status").asInt()).isEqualTo(status);
        assertThat(error.path("code").asText()).isEqualTo(code);
        assertThat(error.path("message").asText()).isNotBlank();
        assertThat(error.path("timestamp").asText()).endsWith("Z");
        assertThat(Instant.parse(error.path("timestamp").asText())).isBeforeOrEqualTo(Instant.now());
    }
}
