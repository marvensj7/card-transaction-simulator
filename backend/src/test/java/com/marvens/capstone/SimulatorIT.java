package com.marvens.capstone;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvens.capstone.dto.PurchaseRequest;
import com.marvens.capstone.dto.TransactionResultResponse;
import com.marvens.capstone.entity.AppUser;
import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.entity.DemoCard;
import com.marvens.capstone.repository.AppUserRepository;
import com.marvens.capstone.repository.CardTransactionRepository;
import com.marvens.capstone.repository.CreditAccountRepository;
import com.marvens.capstone.repository.DemoCardRepository;
import com.marvens.capstone.service.TransactionService;
import com.marvens.capstone.service.FictionalCardNumbers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Opt in with -Pmysql-verification. Uses the existing database and only removes its own fixtures.
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class SimulatorIT extends SecurityTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AppUserRepository users;
    @Autowired CreditAccountRepository accounts;
    @Autowired DemoCardRepository cards;
    @MockitoSpyBean CardTransactionRepository transactions;
    @Autowired TransactionService service;
    @Autowired JdbcTemplate jdbc;
    Long ownerId;
    Long otherId;
    Long adminId;
    Long accountId;
    Long cardId;

    @BeforeEach
    void createFictionalRows() {
        AppUser owner = users.saveAndFlush(TestData.user(AppUser.Role.USER));
        ownerId = owner.getId();
        otherId = users.saveAndFlush(TestData.user(AppUser.Role.USER)).getId();
        adminId = users.saveAndFlush(TestData.user(AppUser.Role.ADMIN)).getId();
        CreditAccount account = accounts.saveAndFlush(TestData.account(owner));
        accountId = account.getId();
        DemoCard card = TestData.card(account);
        FictionalCardNumbers.assignTo(card);
        cards.saveAndFlush(card);
        cardId = card.getId();
    }

    @AfterEach
    void removeOnlyTheseRows() {
        reset(transactions);
        // Delete linked refunds before their original purchases.
        jdbc.update("DELETE FROM card_transactions WHERE account_id = ? AND type = 'REFUND'", accountId);
        jdbc.update("DELETE FROM card_transactions WHERE account_id = ?", accountId);
        jdbc.update("DELETE FROM demo_cards WHERE account_id = ?", accountId);
        jdbc.update("DELETE FROM credit_accounts WHERE id = ?", accountId);
        jdbc.update("DELETE FROM app_users WHERE id IN (?, ?, ?)", ownerId, otherId, adminId);
    }

    @Test
    void purchaseRetryDeclineAndRefundKeepRealBalancesAndHistoryCorrect() throws Exception {
        PurchaseRequest purchase = purchaseRequest("50.00");
        JsonNode approved = response(purchase(ownerId, purchase), 201);
        Long purchaseId = approved.path("transaction").path("id").asLong();
        assertThat(approved.path("transaction").path("status").asText()).isEqualTo("APPROVED");
        assertThat(balance()).isEqualByComparingTo("250.00");
        assertThat(response(purchase(ownerId, purchase), 200).path("transaction").path("id").asLong()).isEqualTo(purchaseId);
        assertThat(historyCount()).isEqualTo(1);
        purchase.amount = new BigDecimal("51.00");
        response(purchase(ownerId, purchase), 409);
        assertThat(balance()).isEqualByComparingTo("250.00");

        JsonNode declined = response(purchase(ownerId, purchaseRequest("900.00")), 201);
        assertThat(declined.path("transaction").path("reasonCode").asText()).isEqualTo("INSUFFICIENT_CREDIT");
        assertThat(balance()).isEqualByComparingTo("250.00");
        response(refund(ownerId, declined.path("transaction").path("id").asLong(), id()), 409);

        String refundId = id();
        JsonNode refunded = response(refund(ownerId, purchaseId, refundId), 201);
        assertThat(refunded.path("transaction").path("originalPurchaseId").asLong()).isEqualTo(purchaseId);
        assertThat(refunded.path("transaction").path("amount").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(balance()).isEqualByComparingTo("200.00");
        response(refund(ownerId, purchaseId, refundId), 200);
        response(refund(ownerId, purchaseId, id()), 409);
        assertThat(historyCount()).isEqualTo(3);
        JsonNode history = response(get("/api/accounts/" + accountId + "/transactions").with(identity(ownerId, "USER")), 200);
        assertThat(history.path("items").get(2).path("refunded").asBoolean()).isTrue();
    }

    @Test
    void ownershipAndRolesAreEnforcedByRealRepositoryQueries() throws Exception {
        Long purchaseId = response(purchase(ownerId, purchaseRequest("1.00")), 201)
                .path("transaction").path("id").asLong();
        response(get("/api/accounts/" + accountId + "/cards").with(identity(otherId, "USER")), 404);
        response(get("/api/accounts/" + accountId + "/transactions").with(identity(otherId, "USER")), 404);
        response(purchase(otherId, purchaseRequest("1.00")), 404);
        response(refund(otherId, purchaseId, id()), 404);
        response(get("/api/admin/accounts").with(identity(ownerId, "USER")).header("X-Role", "ADMIN"), 403);
        response(purchase(adminId, purchaseRequest("1.00")), 403);
        assertThat(balance()).isEqualByComparingTo("201.00");
        assertThat(historyCount()).isEqualTo(1);
    }

    @Test
    void adminFreezeBlocksPurchasesButAllowsAnExistingPurchaseToBeRefunded() throws Exception {
        Long purchaseId = response(purchase(ownerId, purchaseRequest("50.00")), 201)
                .path("transaction").path("id").asLong();
        response(patch("/api/admin/accounts/" + accountId + "/status")
                .with(identity(adminId, "ADMIN")).param("status", "FROZEN"), 200);
        JsonNode declined = response(purchase(ownerId, purchaseRequest("1.00")), 201);
        assertThat(declined.path("transaction").path("reasonCode").asText()).isEqualTo("ACCOUNT_FROZEN");
        response(refund(ownerId, purchaseId, id()), 201);
        assertThat(balance()).isEqualByComparingTo("200.00");
        response(patch("/api/admin/accounts/" + accountId + "/status")
                .with(identity(adminId, "ADMIN")).param("status", "ACTIVE"), 200);
        JsonNode accountList = response(get("/api/admin/accounts").with(identity(adminId, "ADMIN")), 200);
        assertThat(accountList.path("items").isArray()).isTrue();
        JsonNode activity = response(get("/api/admin/transactions").with(identity(adminId, "ADMIN")), 200);
        assertThat(activity.path("items").isArray()).isTrue();
    }

    @Test
    void invalidInputLeavesTheExistingDatabaseAlone() throws Exception {
        PurchaseRequest request = purchaseRequest("1.00");
        request.testCardNumber = "0".repeat(16);
        response(purchase(ownerId, request), 400);
        request.testCardNumber = "0000" + String.format("%012d", accountId);
        request.amount = new BigDecimal("1.001");
        response(purchase(ownerId, request), 400);
        assertThat(balance()).isEqualByComparingTo("200.00");
        assertThat(historyCount()).isZero();
    }

    @Test
    void historyPaginatesNewestFirstAndReportsTheRealTotal() throws Exception {
        for (int i = 0; i < 51; i++) service.purchase(ownerId, accountId, purchaseRequest("1.00"));
        JsonNode first = response(get("/api/accounts/" + accountId + "/transactions?size=50")
                .with(identity(ownerId, "USER")), 200);
        JsonNode last = response(get("/api/accounts/" + accountId + "/transactions?size=50&page=1")
                .with(identity(ownerId, "USER")), 200);
        assertThat(first.path("items").size()).isEqualTo(50);
        assertThat(last.path("items").size()).isEqualTo(1);
        assertThat(first.path("totalElements").asInt()).isEqualTo(51);
        assertThat(first.path("totalPages").asInt()).isEqualTo(2);
        assertThat(first.path("items").get(0).path("id").asLong()).isGreaterThan(last.path("items").get(0).path("id").asLong());
        assertThat(first.path("items").get(0).path("createdAt").asText()).endsWith("Z");
        assertThat(balance()).isEqualByComparingTo("251.00");
    }

    @Test
    void simultaneousPurchasesCannotSpendTheSameAvailableCredit() throws Exception {
        var workers = Executors.newFixedThreadPool(2);
        try {
            var first = workers.submit(() -> service.purchase(ownerId, accountId, purchaseRequest("500.00")));
            var second = workers.submit(() -> service.purchase(ownerId, accountId, purchaseRequest("500.00")));
            List<TransactionResultResponse> results = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertThat(results).extracting(result -> result.transaction.status.name())
                    .containsExactlyInAnyOrder("APPROVED", "DECLINED");
            assertThat(balance()).isEqualByComparingTo("700.00");
            assertThat(historyCount()).isEqualTo(2);
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void simultaneousIdenticalRetriesCreateOnlyOnePurchase() throws Exception {
        PurchaseRequest request = purchaseRequest("50.00");
        var workers = Executors.newFixedThreadPool(2);
        try {
            var first = workers.submit(() -> service.purchase(ownerId, accountId, request));
            var second = workers.submit(() -> service.purchase(ownerId, accountId, request));
            Long firstId = first.get(15, TimeUnit.SECONDS).transaction.id;
            Long secondId = second.get(15, TimeUnit.SECONDS).transaction.id;
            assertThat(firstId).isEqualTo(secondId);
            assertThat(balance()).isEqualByComparingTo("250.00");
            assertThat(historyCount()).isEqualTo(1);
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void aFailedHistorySaveRollsBackEvenAnAlreadyFlushedBalanceChange() throws Exception {
        doAnswer(call -> {
            accounts.flush();
            throw new IllegalStateException("fictional-write-failure-marker");
        }).when(transactions).save(any());
        response(purchase(ownerId, purchaseRequest("50.00")), 500);
        assertThat(balance()).isEqualByComparingTo("200.00");
        assertThat(historyCount()).isZero();
    }

    private PurchaseRequest purchaseRequest(String amount) {
        PurchaseRequest request = TestData.purchase(cardId, amount);
        request.testCardNumber = "0000" + String.format("%012d", accountId);
        return request;
    }

    private MockHttpServletRequestBuilder purchase(Long userId, PurchaseRequest request) {
        return post("/api/accounts/" + accountId + "/purchases").with(identity(userId, userId.equals(adminId) ? "ADMIN" : "USER"))
                .contentType(MediaType.APPLICATION_JSON).content(TestData.purchaseJson(request).toString());
    }

    private MockHttpServletRequestBuilder refund(Long userId, Long purchaseId, String requestId) {
        return post("/api/transactions/" + purchaseId + "/refund").with(identity(userId, userId.equals(adminId) ? "ADMIN" : "USER"))
                .param("requestId", requestId);
    }

    private JsonNode response(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        String body = mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("0000" + String.format("%012d", accountId), TestData.testNumber(), "testSecurityCode", "passwordHash", "$2b$", "accessToken");
        return json.readTree(body);
    }

    private BigDecimal balance() {
        return jdbc.queryForObject("SELECT outstanding_balance FROM credit_accounts WHERE id = ?", BigDecimal.class, accountId);
    }

    private int historyCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM card_transactions WHERE account_id = ?", Integer.class, accountId);
    }

    private String id() {
        return UUID.randomUUID().toString();
    }
}
