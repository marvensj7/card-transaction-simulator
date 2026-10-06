package com.marvens.capstone.controller;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marvens.capstone.entity.CardTransaction;
import com.marvens.capstone.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@ExtendWith(OutputCaptureExtension.class)
class TransactionControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockitoBean private TransactionService transactions;

    @ParameterizedTest
    @CsvSource({"APPROVED,false,201", "DECLINED,false,201", "APPROVED,true,200", "DECLINED,true,200"})
    void purchaseStatusAndSavedResponseFollowTheServiceOutcome(CardTransaction.Status state,
                                                              boolean replayed, int status) throws Exception {
        var account = ControllerFixtures.account();
        var purchase = ControllerFixtures.purchase(account);
        purchase.setStatus(state);
        purchase.setReasonCode(state == CardTransaction.Status.DECLINED ? "ACCOUNT_FROZEN" : null);
        // The saved transaction balance is independent of the current account summary.
        account.setOutstandingBalance(new java.math.BigDecimal("75"));
        when(transactions.purchase(eq(9L), eq(7L), any())).thenReturn(new TransactionOutcome(purchase, account, replayed));
        var result = mvc.perform(post("/api/accounts/7/purchases").principal(new AuthenticatedUser(9L))
                        .header("X-User-Id", "1").param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RequestDtoTest.validJson().replace("\"cardId\":7", "\"userId\":1,\"role\":\"ADMIN\",\"cardId\":7")))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.transaction.id").value(42))
                .andExpect(jsonPath("$.transaction.status").value(state.name()))
                .andExpect(jsonPath("$.transaction.amount").value("25.00"))
                .andExpect(jsonPath("$.transaction.outstandingAfter").value("25.00"))
                .andExpect(jsonPath("$.transaction.createdAt").value("2026-10-06T15:30:00.123456Z"))
                .andExpect(jsonPath("$.account.outstandingBalance").value("75.00"))
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(RequestDtoTest.testNumber(),
                "testSecurityCode", "passwordHash", "fictional-secret-hash-marker", "accessToken", "replayed", "requestId");
        var command = ArgumentCaptor.forClass(PurchaseCommand.class);
        verify(transactions).purchase(eq(9L), eq(7L), command.capture());
        assertThat(command.getValue().getCardId()).isEqualTo(7L);
        assertThat(command.getValue().getAmount()).isEqualByComparingTo("25.00");
        assertThat(command.getValue().getTestCardNumber()).isEqualTo(RequestDtoTest.testNumber());
    }

    @ParameterizedTest
    @CsvSource({"false,201", "true,200"})
    void fullRefundIncludesTheOriginalPurchaseAndSupportsReplay(boolean replayed, int status) throws Exception {
        var account = ControllerFixtures.account();
        var purchase = ControllerFixtures.purchase(account);
        var refund = ControllerFixtures.purchase(account);
        refund.setType(CardTransaction.Type.REFUND);
        refund.setOriginalPurchase(purchase);
        refund.setOutstandingAfter(new java.math.BigDecimal("0"));
        account.setOutstandingBalance(new java.math.BigDecimal("0"));
        when(transactions.refund(9L, 42L, "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
                .thenReturn(new TransactionOutcome(refund, account, replayed));
        mvc.perform(post("/api/transactions/42/refund").principal(new AuthenticatedUser(9L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb\",\"userId\":1,\"amount\":\"99.00\"}"))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.transaction.type").value("REFUND"))
                .andExpect(jsonPath("$.transaction.originalPurchaseId").value(42))
                .andExpect(jsonPath("$.transaction.amount").value("25.00"))
                .andExpect(jsonPath("$.account.outstandingBalance").value("0.00"));
        verify(transactions).refund(9L, 42L, "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    }

    @Test
    void historyDefaultsCapsSizeAndPreservesTheServicePage() throws Exception {
        var item = ControllerFixtures.purchase(ControllerFixtures.account());
        when(transactions.getHistory(9L, 7L, 0, 20)).thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 21));
        mvc.perform(get("/api/accounts/7/transactions").principal(new AuthenticatedUser(9L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(42))
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(21)).andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content").doesNotExist()).andExpect(jsonPath("$.pageable").doesNotExist());
        when(transactions.getHistory(9L, 7L, 2, 50)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 50), 51));
        mvc.perform(get("/api/accounts/7/transactions").principal(new AuthenticatedUser(9L)).param("page", "2").param("size", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(2)).andExpect(jsonPath("$.size").value(50));
        verify(transactions).getHistory(9L, 7L, 0, 20);
        verify(transactions).getHistory(9L, 7L, 2, 50);
    }

    @Test
    void invalidPurchaseFieldsAreRejectedWithoutSensitiveLogs(CapturedOutput output) throws Exception {
        Object[][] invalid = {{"cardId", 0}, {"cardId", 7.5}, {"expiryMonth", 12.5}, {"expiryMonth", 0}, {"expiryMonth", 13},
                {"expiryYear", 1999}, {"expiryYear", 10000}, {"testCardNumber", "sensitive-number-marker"},
                {"testSecurityCode", "sensitive-code-marker"}, {"merchantName", " "}, {"merchantName", "x".repeat(101)},
                {"amount", "0"}, {"amount", "-1"}, {"amount", "1.001"}, {"amount", "1000000000000"},
                {"amount", "1e2"}, {"amount", " 25.00"}, {"amount", 25}, {"requestId", "invalid-id"}};
        for (Object[] entry : invalid) {
            ObjectNode body = (ObjectNode) json.readTree(RequestDtoTest.validJson());
            body.set((String) entry[0], json.valueToTree(entry[1]));
            var result = mvc.perform(post("/api/accounts/7/purchases").principal(new AuthenticatedUser(9L))
                            .contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                    .andExpect(status().isBadRequest()).andReturn();
            assertThat(result.getResponse().getContentAsString()).doesNotContain(RequestDtoTest.testNumber(),
                    "sensitive-number-marker", "sensitive-code-marker");
        }
        for (String field : new String[] {"cardId", "testCardNumber", "expiryMonth", "expiryYear",
                "testSecurityCode", "merchantName", "amount", "requestId"}) {
            ObjectNode body = (ObjectNode) json.readTree(RequestDtoTest.validJson());
            body.remove(field);
            mvc.perform(post("/api/accounts/7/purchases").principal(new AuthenticatedUser(9L))
                            .contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                    .andExpect(status().isBadRequest());
        }
        assertThat(output.getAll()).doesNotContain(RequestDtoTest.testNumber(), "sensitive-number-marker",
                "sensitive-code-marker", "testSecurityCode", "fictional-secret-hash-marker");
        verifyNoInteractions(transactions);
    }

    @Test
    void malformedBodiesRefundIdsPathsAndPaginationStopBeforeServiceAccess() throws Exception {
        for (String body : new String[] {"{}", "{\"requestId\":null}", "{\"requestId\":\"invalid\"}", "{"}) {
            mvc.perform(post("/api/transactions/42/refund").principal(new AuthenticatedUser(9L))
                            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/accounts/0/purchases").principal(new AuthenticatedUser(9L))
                        .contentType(MediaType.APPLICATION_JSON).content(RequestDtoTest.validJson())).andExpect(status().isBadRequest());
        for (String query : new String[] {"page=-1", "size=0", "size=-1", "page=bad", "size=bad"}) {
            mvc.perform(get("/api/accounts/7/transactions?" + query).principal(new AuthenticatedUser(9L)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/accounts/7/purchases").principal(new AuthenticatedUser(9L)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(transactions);
    }

    @Test
    void protectedTransactionRoutesRejectSpoofedIdentityEvenWithMalformedInput() throws Exception {
        mvc.perform(post("/api/accounts/7/purchases").header("X-User-Id", "9")
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/accounts/7/transactions").param("userId", "9"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/transactions/42/refund").header("Authorization", "Bearer unverified-test-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(transactions);
    }
}
