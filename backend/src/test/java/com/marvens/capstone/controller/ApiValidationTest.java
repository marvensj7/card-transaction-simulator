package com.marvens.capstone.controller;

import java.time.Instant;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marvens.capstone.service.AccountService;
import com.marvens.capstone.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AccountController.class, TransactionController.class, AdminController.class})
@ExtendWith(OutputCaptureExtension.class)
class ApiValidationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockitoBean private TransactionService transactions;
    @MockitoBean private AccountService accounts;
    private static final String SECRET = "fictional-rejected-secret";

    static Stream<Arguments> invalidFields() {
        return Stream.of(Arguments.of("cardId", 0, "Resource IDs"),
                Arguments.of("testCardNumber", SECRET, "16-digit"),
                Arguments.of("testSecurityCode", SECRET, "three or four digits"),
                Arguments.of("expiryMonth", 13, "1 to 12"),
                Arguments.of("expiryYear", 1999, "2000 to 9999"),
                Arguments.of("merchantName", " ", "Merchant name"),
                Arguments.of("amount", "1.001", "positive decimal string"),
                Arguments.of("requestId", SECRET, "UUID"));
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void beanValidationUsesCorrectiveMessagesWithoutRejectedValues(String field, Object value,
            String hint, CapturedOutput output) throws Exception {
        ObjectNode body = (ObjectNode) json.readTree(RequestDtoTest.validJson());
        body.set(field, json.valueToTree(value));
        String response = badRequest(purchase().content(body.toString()), "VALIDATION_FAILED");
        assertThat(json.readTree(response).path("message").asText()).contains(hint);
        assertSafe(response, output);
    }

    @ParameterizedTest
    @ValueSource(strings = {"cardId", "testCardNumber", "testSecurityCode", "expiryMonth", "expiryYear",
            "merchantName", "amount", "requestId"})
    void missingAndNullPurchaseFieldsUseTheErrorShape(String field, CapturedOutput output) throws Exception {
        ObjectNode body = (ObjectNode) json.readTree(RequestDtoTest.validJson());
        body.remove(field);
        assertSafe(badRequest(purchase().content(body.toString()), "VALIDATION_FAILED"), output);
        body.putNull(field);
        assertSafe(badRequest(purchase().content(body.toString()), "VALIDATION_FAILED"), output);
    }

    @Test
    void malformedJsonWrongTypesAndMissingBodyUseSafeJson(CapturedOutput output) throws Exception {
        for (String body : new String[] {"{", "null", "[]",
                "{\"testCardNumber\":\"" + RequestDtoTest.testNumber() + "\",\"testSecurityCode\":\"" + SECRET + "\",",
                RequestDtoTest.validJson().replace("\"25.00\"", "25.00"),
                RequestDtoTest.validJson().replace("\"cardId\":7", "\"cardId\":7.5")}) {
            assertSafe(badRequest(purchase().content(body), "MALFORMED_JSON"), output);
        }
        assertSafe(badRequest(purchase(), "MALFORMED_JSON"), output);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/accounts/0/cards", "/api/accounts/-1/cards", "/api/accounts/1.5/cards",
            "/api/accounts/fictional-rejected-secret/cards", "/api/accounts/9223372036854775808/cards",
            "/api/accounts/7/transactions?page=-1", "/api/accounts/7/transactions?size=0",
            "/api/accounts/7/transactions?page=fictional-rejected-secret", "/api/accounts/7/transactions?size=1.5",
            "/api/admin/accounts?page=-1", "/api/admin/transactions?size=-1"})
    void invalidPathsAndQueriesNeverRepeatValues(String path, CapturedOutput output) throws Exception {
        assertSafe(badRequest(get(path), "INVALID_PARAMETER"), output);
    }

    @Test
    void refundAndAccountStatusValidationUseTheSameShape(CapturedOutput output) throws Exception {
        assertSafe(badRequest(post("/api/transactions/42/refund").contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestId\":\"" + SECRET + "\"}"), "VALIDATION_FAILED"), output);
        assertSafe(badRequest(post("/api/transactions/0/refund").contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestId\":\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\"}"), "INVALID_PARAMETER"), output);
        assertSafe(badRequest(patch("/api/admin/accounts/7/status").contentType(MediaType.APPLICATION_JSON)
                .content("{}"), "VALIDATION_FAILED"), output);
        for (String value : new String[] {"\"" + SECRET + "\"", "1"}) {
            assertSafe(badRequest(patch("/api/admin/accounts/7/status").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":" + value + "}"), "MALFORMED_JSON"), output);
        }
    }

    private MockHttpServletRequestBuilder purchase() {
        return post("/api/accounts/7/purchases").contentType(MediaType.APPLICATION_JSON);
    }

    private String badRequest(MockHttpServletRequestBuilder request, String code) throws Exception {
        String body = mvc.perform(request.principal(new AuthenticatedUser(9L)))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.code").value(code))
                .andReturn().getResponse().getContentAsString();
        var error = json.readTree(body);
        assertThat(error.size()).isEqualTo(4);
        assertThat(error.path("message").asText()).isNotBlank();
        assertThat(error.path("timestamp").asText()).endsWith("Z");
        assertThat(Instant.parse(error.path("timestamp").asText())).isBeforeOrEqualTo(Instant.now());
        verifyNoInteractions(accounts, transactions);
        return body;
    }

    private void assertSafe(String body, CapturedOutput output) {
        assertThat(body).doesNotContain(SECRET, RequestDtoTest.testNumber(), "testCardNumber", "testSecurityCode",
                "rejectedValue", "exception", "stackTrace");
        assertThat(output.getAll()).doesNotContain(SECRET, RequestDtoTest.testNumber(), "testSecurityCode");
    }
}
