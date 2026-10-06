package com.marvens.capstone.controller;

import java.time.Instant;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvens.capstone.exception.*;
import com.marvens.capstone.service.AccountService;
import com.marvens.capstone.service.TransactionService;
import org.junit.jupiter.api.extension.ExtendWith;
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
                Arguments.of(new InvalidPurchaseException(SECRET), 400, "INVALID_PURCHASE"),
                Arguments.of(new InvalidRequestException(SECRET), 400, "INVALID_REQUEST"),
                Arguments.of(new AccessDeniedException(), 403, "ACCESS_DENIED"),
                Arguments.of(new ResourceNotFoundException(SECRET), 404, "RESOURCE_NOT_FOUND"),
                Arguments.of(new RequestConflictException(), 409, "REQUEST_CONFLICT"),
                Arguments.of(new RefundNotEligibleException(SECRET), 409, "REFUND_NOT_ELIGIBLE"));
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
