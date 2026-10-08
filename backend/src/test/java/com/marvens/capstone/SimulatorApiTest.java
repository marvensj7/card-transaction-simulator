package com.marvens.capstone;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marvens.capstone.controller.AccountController;
import com.marvens.capstone.controller.AdminController;
import com.marvens.capstone.controller.TransactionController;
import com.marvens.capstone.dto.PurchaseRequest;
import com.marvens.capstone.entity.AppUser;
import com.marvens.capstone.entity.CardTransaction;
import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.entity.DemoCard;
import com.marvens.capstone.repository.AppUserRepository;
import com.marvens.capstone.repository.CardTransactionRepository;
import com.marvens.capstone.repository.CreditAccountRepository;
import com.marvens.capstone.repository.DemoCardRepository;
import com.marvens.capstone.service.AccountService;
import com.marvens.capstone.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real controllers and service decisions; repositories are replaced by controlled test data.
@WebMvcTest({AccountController.class, TransactionController.class, AdminController.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Import({AccountService.class, TransactionService.class})
@ExtendWith(OutputCaptureExtension.class)
class SimulatorApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean AppUserRepository users;
    @MockitoBean CreditAccountRepository accounts;
    @MockitoBean DemoCardRepository cards;
    @MockitoBean CardTransactionRepository transactions;
    CreditAccount account;
    DemoCard card;

    @BeforeEach
    void setUp() {
        AppUser customer = TestData.user(AppUser.Role.USER);
        AppUser admin = TestData.user(AppUser.Role.ADMIN);
        ReflectionTestUtils.setField(customer, "id", 1L);
        ReflectionTestUtils.setField(admin, "id", 2L);
        account = TestData.account(customer);
        ReflectionTestUtils.setField(account, "id", 7L);
        card = TestData.card(account);
        ReflectionTestUtils.setField(card, "id", 8L);
        when(users.findById(1L)).thenReturn(Optional.of(customer));
        when(users.findById(2L)).thenReturn(Optional.of(admin));
        when(users.findById(3L)).thenReturn(Optional.of(TestData.user(AppUser.Role.USER)));
        when(accounts.findByUser_Id(1L)).thenReturn(account);
        when(accounts.findByIdAndUser_Id(7L, 1L)).thenReturn(account);
        when(accounts.findOwnedForUpdate(7L, 1L)).thenReturn(account);
        when(accounts.findForUpdate(7L)).thenReturn(account);
        when(cards.findByAccount_Id(7L)).thenReturn(card);
        when(cards.findByIdAndAccount_Id(8L, 7L)).thenReturn(card);
        when(transactions.save(any())).thenAnswer(call -> {
            CardTransaction saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 42L);
            return saved;
        });
    }

    @Test
    void accountAndCardResponsesContainOnlySafeDisplayFields() throws Exception {
        String summary = mvc.perform(get("/api/accounts").sessionAttr("userId", 1L))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].ownerName").value("Demo Customer"))
                .andExpect(jsonPath("$[0].availableCredit").value(800))
                .andReturn().getResponse().getContentAsString();
        String masked = mvc.perform(get("/api/accounts/7/cards").sessionAttr("userId", 1L))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].maskedNumber").value("\u2022\u2022\u2022\u2022 4242"))
                .andReturn().getResponse().getContentAsString();
        assertThat(summary + masked).doesNotContain("passwordHash", "$2b$", TestData.testNumber(), "testSecurityCode");
    }

    @Test
    void approvalAddsTheAmountAndReturnsTheCurrentCredit() throws Exception {
        String result = submit(TestData.purchase(8L, "50.01"), 200);
        assertThat(json.readTree(result).path("transaction").path("status").asText()).isEqualTo("APPROVED");
        assertThat(json.readTree(result).path("account").path("outstandingBalance").decimalValue())
                .isEqualByComparingTo("250.01");
        assertThat(account.getOutstandingBalance()).isEqualByComparingTo("250.01");
        verify(accounts).save(account);
        verify(transactions).save(any());
        assertThat(result).doesNotContain(TestData.testNumber(), "testSecurityCode", "requestId", "passwordHash");
    }

    @Test
    void insufficientCreditCreatesADeclineWithoutChangingBalance() throws Exception {
        String result = submit(TestData.purchase(8L, "800.01"), 200);
        assertThat(json.readTree(result).path("transaction").path("reasonCode").asText()).isEqualTo("INSUFFICIENT_CREDIT");
        assertThat(account.getOutstandingBalance()).isEqualByComparingTo("200.00");
        verify(accounts, never()).save(any());
        verify(transactions).save(any());
    }

    @Test
    void frozenAccountDeclinesNewSpending() throws Exception {
        account.setStatus(CreditAccount.Status.FROZEN);
        String result = submit(TestData.purchase(8L, "1.00"), 200);
        assertThat(json.readTree(result).path("transaction").path("reasonCode").asText()).isEqualTo("ACCOUNT_FROZEN");
        assertThat(account.getOutstandingBalance()).isEqualByComparingTo("200.00");
        verify(accounts, never()).save(any());
    }

    @Test
    void matchingExpiredCardDeclinesButTheCurrentExpiryMonthIsValid() throws Exception {
        YearMonth month = YearMonth.now(ZoneOffset.UTC);
        card.setExpiryMonth((byte) month.getMonthValue());
        card.setExpiryYear((short) (month.getYear() - 1));
        PurchaseRequest request = TestData.purchase(8L, "1.00");
        request.expiryMonth = month.getMonthValue();
        request.expiryYear = month.getYear() - 1;
        assertThat(json.readTree(submit(request, 200)).path("transaction").path("reasonCode").asText())
                .isEqualTo("CARD_EXPIRED");
        card.setExpiryYear((short) month.getYear());
        request.expiryYear = month.getYear();
        request.requestId = UUID.randomUUID().toString();
        assertThat(json.readTree(submit(request, 200)).path("transaction").path("status").asText())
                .isEqualTo("APPROVED");
    }

    @Test
    void invalidAmountsDoNotSaveAnything() throws Exception {
        for (String amount : new String[] {"0", "-1", "1.001", "1000000000000"}) {
            submit(TestData.purchase(8L, amount), 400);
        }
        assertThat(account.getOutstandingBalance()).isEqualByComparingTo("200.00");
        verify(transactions, never()).save(any());
        verify(accounts, never()).save(any());
    }

    @Test
    void everyPurchaseFieldIsRequired() throws Exception {
        for (String field : new String[] {"cardId", "testCardNumber", "expiryMonth", "expiryYear",
                "testSecurityCode", "merchantName", "amount", "requestId"}) {
            ObjectNode body = TestData.purchaseJson(TestData.purchase(8L, "1.00"));
            body.remove(field);
            mvc.perform(post("/api/accounts/7/purchases").sessionAttr("userId", 1L)
                            .contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                    .andExpect(status().isBadRequest());
        }
        verify(transactions, never()).save(any());
    }

    @Test
    void wrongCardDetailsAndMalformedInputNeverBecomeHistory(CapturedOutput output) throws Exception {
        PurchaseRequest request = TestData.purchase(8L, "1.00");
        request.testCardNumber = "0".repeat(16);
        submit(request, 400);
        request.testCardNumber = TestData.testNumber();
        request.expiryMonth = 11;
        submit(request, 400);
        request.expiryMonth = 13;
        submit(request, 400);
        request.expiryMonth = 12;
        request.testSecurityCode = "fictional-secret-marker";
        String result = submit(request, 400);
        request.testSecurityCode = "9".repeat(3);
        request.merchantName = " ";
        submit(request, 400);
        request.merchantName = "Demo Bookstore";
        request.requestId = "1-1-1-1-1";
        submit(request, 400);
        assertThat(result + output.getAll()).doesNotContain("fictional-secret-marker", TestData.testNumber());
        verify(transactions, never()).save(any());
    }

    @Test
    void anotherCustomerAndTheWrongRoleCannotUseAnAccount() throws Exception {
        mvc.perform(get("/api/accounts/7/cards").sessionAttr("userId", 3L))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/accounts/7/transactions").sessionAttr("userId", 3L))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/accounts").sessionAttr("userId", 1L).header("X-Role", "ADMIN"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/accounts/7/purchases").sessionAttr("userId", 2L)
                        .contentType(MediaType.APPLICATION_JSON).content(TestData.purchaseJson(TestData.purchase(8L, "1.00")).toString()))
                .andExpect(status().isForbidden());
        verify(transactions, never()).save(any());
    }

    @Test
    void identicalRetryReturnsTheOriginalPurchaseWithoutAddingToTheBalance() throws Exception {
        PurchaseRequest request = TestData.purchase(8L, "50.00");
        request.requestId = request.requestId.toUpperCase();
        submit(request, 200);
        ArgumentCaptor<CardTransaction> capture = ArgumentCaptor.forClass(CardTransaction.class);
        verify(transactions).save(capture.capture());
        CardTransaction saved = capture.getValue();
        when(transactions.findByAccount_IdAndRequestId(7L, request.requestId.toLowerCase())).thenReturn(saved);
        account.setStatus(CreditAccount.Status.FROZEN);
        String result = submit(request, 200);
        assertThat(json.readTree(result).path("transaction").path("status").asText()).isEqualTo("APPROVED");
        assertThat(account.getOutstandingBalance()).isEqualByComparingTo("250.00");
        verify(transactions, times(1)).save(any());
        request.amount = new BigDecimal("51.00");
        submit(request, 409);
        request.amount = new BigDecimal("50.00");
        request.merchantName = "Other Merchant";
        submit(request, 409);
        verify(transactions, times(1)).save(any());
    }

    @Test
    void fullRefundCopiesThePurchaseAmountAndSupportsAnIdenticalRetry() throws Exception {
        submit(TestData.purchase(8L, "50.00"), 200);
        ArgumentCaptor<CardTransaction> capture = ArgumentCaptor.forClass(CardTransaction.class);
        verify(transactions).save(capture.capture());
        CardTransaction purchase = capture.getValue();
        when(transactions.findOwnedAccountId(42L, 1L)).thenReturn(7L);
        when(transactions.findByIdAndAccount_User_Id(42L, 1L)).thenReturn(purchase);
        account.setStatus(CreditAccount.Status.FROZEN);
        String requestId = UUID.randomUUID().toString();
        String result = mvc.perform(post("/api/transactions/42/refund").sessionAttr("userId", 1L)
                        .param("requestId", requestId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.originalPurchaseId").value(42))
                .andExpect(jsonPath("$.transaction.amount").value(50))
                .andReturn().getResponse().getContentAsString();
        assertThat(account.getOutstandingBalance()).isEqualByComparingTo("200.00");
        assertThat(json.readTree(result).path("transaction").path("type").asText()).isEqualTo("REFUND");
        verify(transactions, times(2)).save(capture.capture());
        CardTransaction refund = capture.getValue();
        when(transactions.findByAccount_IdAndRequestId(7L, requestId)).thenReturn(refund);
        mvc.perform(post("/api/transactions/42/refund").sessionAttr("userId", 1L)
                .param("requestId", requestId)).andExpect(status().isOk());
        verify(transactions, times(2)).save(any());
        when(transactions.findByOriginalPurchase_Id(42L)).thenReturn(refund);
        mvc.perform(post("/api/transactions/42/refund").sessionAttr("userId", 1L)
                .param("requestId", UUID.randomUUID().toString())).andExpect(status().isConflict());
    }

    @Test
    void historyAndAdminViewsUseSimpleArraysAndTheSameSummaries() throws Exception {
        submit(TestData.purchase(8L, "1.00"), 200);
        ArgumentCaptor<CardTransaction> capture = ArgumentCaptor.forClass(CardTransaction.class);
        verify(transactions).save(capture.capture());
        when(transactions.findByAccount_IdOrderByIdDesc(7L)).thenReturn(List.of(capture.getValue()));
        when(transactions.findAllByOrderByIdDesc()).thenReturn(List.of(capture.getValue()));
        when(accounts.findAllByOrderByIdAsc()).thenReturn(List.of(account));
        mvc.perform(get("/api/accounts/7/transactions").sessionAttr("userId", 1L))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(42));
        mvc.perform(get("/api/admin/transactions").sessionAttr("userId", 2L))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].accountId").value(7));
        mvc.perform(get("/api/admin/accounts").sessionAttr("userId", 2L))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].ownerName").value("Demo Customer"));
        mvc.perform(patch("/api/admin/accounts/7/status").sessionAttr("userId", 2L).param("status", "FROZEN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FROZEN"));
    }

    @Test
    void untrustedIdentityIsRejectedBeforeReadingEvenMalformedInput() throws Exception {
        mvc.perform(post("/api/accounts/7/purchases").header("X-User-Id", "1").header("X-Role", "ADMIN")
                        .header("Authorization", "Bearer fictional-unverified-token")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Sign in to continue."));
        verifyNoInteractions(users, accounts, cards, transactions);
    }

    @Test
    void invalidSessionValuesAreRejectedBeforeAnyRepositoryCall() throws Exception {
        mvc.perform(get("/api/accounts").sessionAttr("userId", "1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/accounts").sessionAttr("userId", 0L)).andExpect(status().isUnauthorized());
        verifyNoInteractions(users, accounts, cards, transactions);
    }

    @Test
    void invalidHttpRequestsUseSafeMessagesAndCorrectStatuses() throws Exception {
        mvc.perform(post("/api/accounts/7/purchases").sessionAttr("userId", 1L)
                .contentType(MediaType.APPLICATION_JSON).content("{")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/accounts/1.5/cards").sessionAttr("userId", 1L))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/transactions/42/refund").sessionAttr("userId", 1L))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/admin/accounts/7/status").sessionAttr("userId", 2L).param("status", "CLOSED"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/missing").sessionAttr("userId", 1L)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/accounts").sessionAttr("userId", 1L))
                .andExpect(status().isMethodNotAllowed()).andExpect(header().exists("Allow"));
        mvc.perform(post("/api/accounts/7/purchases").sessionAttr("userId", 1L)
                .contentType(MediaType.TEXT_PLAIN).content("fictional-marker")).andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void unexpectedErrorsDoNotPrintInputOrDatabaseDetails(CapturedOutput output) throws Exception {
        doThrow(new IllegalStateException("fictional-database-secret-marker")).when(transactions).save(any());
        String result = submit(TestData.purchase(8L, "1.00"), 500);
        assertThat(json.readTree(result).size()).isEqualTo(1);
        assertThat(json.readTree(result).path("message").asText()).isEqualTo("An unexpected error occurred.");
        assertThat(result + output.getAll()).doesNotContain("fictional-database-secret-marker", TestData.testNumber());
        assertThat(output.getAll()).contains("API failure type=java.lang.IllegalStateException");
    }

    private String submit(PurchaseRequest request, int expectedStatus) throws Exception {
        return mvc.perform(post("/api/accounts/7/purchases").sessionAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON).content(TestData.purchaseJson(request).toString()))
                .andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
    }

}
