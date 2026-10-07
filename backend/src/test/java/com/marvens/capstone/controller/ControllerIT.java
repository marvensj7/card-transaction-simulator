package com.marvens.capstone.controller;

import com.marvens.capstone.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marvens.capstone.entity.*;
import com.marvens.capstone.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real services and MySQL; no test transaction keeps lazy entities open for mapping. */
@SpringBootTest
@AutoConfigureMockMvc
class ControllerIT {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private AppUserRepository users;
    @Autowired private CreditAccountRepository accounts;
    @Autowired private DemoCardRepository cards;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager manager;
    private Long ownerId;
    private Long otherId;
    private Long adminId;
    private Long accountId;
    private Long cardId;
    private String ownerEmail;

    @BeforeEach
    void createOnlyFictionalFixtures() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            AppUser owner = user(AppUser.Role.USER);
            ownerId = owner.getId();
            ownerEmail = owner.getEmail();
            otherId = user(AppUser.Role.USER).getId();
            adminId = user(AppUser.Role.ADMIN).getId();
            var account = new CreditAccount();
            account.setUser(owner);
            account.setCreditLimit(new BigDecimal("1000.00"));
            account.setStatus(CreditAccount.Status.ACTIVE);
            accountId = accounts.saveAndFlush(account).getId();
            var card = new DemoCard();
            card.setAccount(account);
            card.setLabel("Controller Test Card");
            card.setLastFour("4242");
            card.setTestProfile("DEMO_4242");
            card.setExpiryMonth((byte) 12);
            card.setExpiryYear((short) 2030);
            cardId = cards.saveAndFlush(card).getId();
        });
    }

    @AfterEach
    void deleteOnlyTheseFixtureRows() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            if (accountId != null) {
                jdbc.update("DELETE FROM card_transactions WHERE account_id = ? AND type = 'REFUND'", accountId);
                jdbc.update("DELETE FROM card_transactions WHERE account_id = ?", accountId);
                jdbc.update("DELETE FROM demo_cards WHERE account_id = ?", accountId);
                jdbc.update("DELETE FROM credit_accounts WHERE id = ?", accountId);
            }
            for (Long id : new Long[] {ownerId, otherId, adminId}) {
                if (id != null) jdbc.update("DELETE FROM app_users WHERE id = ?", id);
            }
        });
    }

    @Test
    void customerRequestsRoundTripThroughControllersServicesAndMySql() throws Exception {
        String requestId = id();
        JsonNode first = response(purchase(ownerId, requestId, "25.00"), 201);
        Long purchaseId = first.path("transaction").path("id").asLong();
        response(purchase(ownerId, id(), "10.00"), 201);
        JsonNode replay = response(purchase(ownerId, requestId.toUpperCase(), "25"), 200);
        assertThat(replay.path("transaction")).isEqualTo(first.path("transaction"));
        assertThat(replay.path("account").path("outstandingBalance").asText()).isEqualTo("35.00");
        assertThat(first.path("transaction").path("createdAt").asText()).endsWith("Z");
        String refundId = id();
        JsonNode refund = response(refund(ownerId, purchaseId, refundId), 201);
        assertThat(refund.path("transaction").path("originalPurchaseId").asLong()).isEqualTo(purchaseId);
        assertThat(refund.path("transaction").path("type").asText()).isEqualTo("REFUND");
        assertThat(refund.path("account").path("outstandingBalance").asText()).isEqualTo("10.00");
        assertThat(response(refund(ownerId, purchaseId, refundId), 200)).isEqualTo(refund);
        JsonNode history = response(get("/api/accounts/" + accountId + "/transactions")
                .principal(new AuthenticatedUser(ownerId)).param("size", "1"), 200);
        assertThat(history.path("items").get(0)).isEqualTo(refund.path("transaction"));
        assertThat(history.path("totalItems").asInt()).isEqualTo(3);
        assertThat(history.path("totalPages").asInt()).isEqualTo(3);
        JsonNode summary = response(get("/api/accounts").principal(new AuthenticatedUser(ownerId)), 200);
        assertThat(summary.get(0).path("availableCredit").asText()).isEqualTo("990.00");
        JsonNode masked = response(get("/api/accounts/" + accountId + "/cards")
                .principal(new AuthenticatedUser(ownerId)), 200);
        assertThat(masked.get(0).path("maskedNumber").asText()).isEqualTo("\u2022\u2022\u2022\u2022 4242");
    }

    @Test
    void adminResponsesLoadOwnersAfterTheServiceTransactionCloses() throws Exception {
        JsonNode frozen = response(statusChange("FROZEN"), 200);
        assertThat(frozen.path("ownerId").asLong()).isEqualTo(ownerId);
        assertThat(frozen.path("ownerEmail").asText()).isEqualTo(ownerEmail);
        assertThat(frozen.path("status").asText()).isEqualTo("FROZEN");
        JsonNode declined = response(purchase(ownerId, id(), "1.00"), 201);
        assertThat(declined.path("transaction").path("reasonCode").asText()).isEqualTo("ACCOUNT_FROZEN");
        JsonNode accountPage = response(get("/api/admin/accounts").principal(new AuthenticatedUser(adminId)), 200);
        var accountIds = new ArrayList<Long>();
        boolean foundOwner = false;
        for (JsonNode row : accountPage.path("items")) {
            accountIds.add(row.path("id").asLong());
            if (row.path("id").asLong() == accountId) {
                foundOwner = true;
                assertThat(row.path("ownerEmail").asText()).isEqualTo(ownerEmail);
            }
        }
        assertThat(foundOwner).isTrue();
        assertThat(accountIds).isSorted();
        JsonNode activity = response(get("/api/admin/transactions").principal(new AuthenticatedUser(adminId)), 200);
        assertThat(activity.path("items").get(0).path("ownerEmail").asText()).isEqualTo(ownerEmail);
        assertThat(activity.path("items").get(0).path("id")).isEqualTo(declined.path("transaction").path("id"));
        assertThat(response(statusChange("ACTIVE"), 200).path("status").asText()).isEqualTo("ACTIVE");
    }

    @Test
    void storedOwnershipAndRolesCannotBeOverriddenByCallerIds() throws Exception {
        Long purchaseId = response(purchase(ownerId, id(), "1.00"), 201).path("transaction").path("id").asLong();
        for (MockHttpServletRequestBuilder request : new MockHttpServletRequestBuilder[] {
                get("/api/accounts/" + accountId + "/cards").principal(new AuthenticatedUser(otherId)),
                get("/api/accounts/" + accountId + "/transactions").principal(new AuthenticatedUser(otherId)),
                purchase(otherId, id(), "1.00"), refund(otherId, purchaseId, id())}) {
            assertThat(response(request.header("X-User-Id", ownerId), 404).path("code").asText())
                    .isEqualTo("RESOURCE_NOT_FOUND");
        }
        for (MockHttpServletRequestBuilder request : new MockHttpServletRequestBuilder[] {
                get("/api/admin/accounts"), get("/api/admin/transactions"),
                patch("/api/admin/accounts/" + accountId + "/status")
                        .param("status", "FROZEN")}) {
            assertThat(response(request.principal(new AuthenticatedUser(ownerId))
                    .header("X-Role", "ADMIN").header("X-User-Id", adminId), 403).path("code").asText())
                    .isEqualTo("ACCESS_DENIED");
        }
        assertThat(response(purchase(adminId, id(), "1.00"), 403).path("code").asText()).isEqualTo("ACCESS_DENIED");
        mvc.perform(get("/api/admin/accounts").header("Authorization", "Bearer unverified-test-token")
                        .header("X-User-Id", adminId).header("X-Role", "ADMIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectedPurchasesRetriesAndRefundsDoNotChangeBalancesOrHistory() throws Exception {
        ObjectNode invalid = (ObjectNode) json.readTree(RequestDtoTest.validJson());
        invalid.put("cardId", cardId);
        invalid.put("testCardNumber", "4111".repeat(4));
        assertThat(response(post("/api/accounts/" + accountId + "/purchases")
                .principal(new AuthenticatedUser(ownerId)).contentType(MediaType.APPLICATION_JSON)
                .content(invalid.toString()), 400).path("code").asText()).isEqualTo("INVALID_PURCHASE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM card_transactions WHERE account_id = ?", Long.class, accountId))
                .isZero();
        assertThat(balance()).isEqualByComparingTo("0.00");

        String requestId = id();
        Long purchaseId = response(purchase(ownerId, requestId, "25.00"), 201).path("transaction").path("id").asLong();
        assertThat(response(purchase(ownerId, requestId, "26.00"), 409).path("code").asText()).isEqualTo("REQUEST_CONFLICT");
        assertThat(balance()).isEqualByComparingTo("25.00");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM card_transactions WHERE account_id = ?", Long.class, accountId))
                .isEqualTo(1);

        response(refund(ownerId, purchaseId, id()), 201);
        assertThat(response(refund(ownerId, purchaseId, id()), 409).path("code").asText()).isEqualTo("REFUND_NOT_ELIGIBLE");
        Long declinedId = response(purchase(ownerId, id(), "1001.00"), 201).path("transaction").path("id").asLong();
        assertThat(response(refund(ownerId, declinedId, id()), 409).path("code").asText()).isEqualTo("REFUND_NOT_ELIGIBLE");
        assertThat(balance()).isEqualByComparingTo("0.00");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM card_transactions WHERE account_id = ?", Long.class, accountId))
                .isEqualTo(3);
    }

    private BigDecimal balance() {
        return jdbc.queryForObject("SELECT outstanding_balance FROM credit_accounts WHERE id = ?", BigDecimal.class, accountId);
    }

    private AppUser user(AppUser.Role role) {
        var user = new AppUser();
        user.setDisplayName("Controller Test User");
        user.setEmail("controller-" + id() + "@example.test");
        user.setRole(role);
        user.setPasswordHash("$2b$12$MuK8D982Lf/BpY8IlIUbeuV5.ZoE0.qphVqR58DtC5byiysllvA7C");
        return users.saveAndFlush(user);
    }

    private MockHttpServletRequestBuilder purchase(Long userId, String requestId, String amount) throws Exception {
        ObjectNode body = (ObjectNode) json.readTree(RequestDtoTest.validJson());
        body.put("cardId", cardId);
        body.put("requestId", requestId);
        body.put("amount", amount);
        body.put("userId", ownerId);
        return post("/api/accounts/" + accountId + "/purchases").principal(new AuthenticatedUser(userId))
                .contentType(MediaType.APPLICATION_JSON).content(body.toString());
    }

    private MockHttpServletRequestBuilder refund(Long userId, Long purchaseId, String requestId) {
        return post("/api/transactions/" + purchaseId + "/refund").principal(new AuthenticatedUser(userId))
                .param("requestId", requestId);
    }

    private MockHttpServletRequestBuilder statusChange(String state) {
        return patch("/api/admin/accounts/" + accountId + "/status").principal(new AuthenticatedUser(adminId))
                .param("status", state);
    }

    private JsonNode response(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        String body = mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain(RequestDtoTest.testNumber(), "testSecurityCode", "testCardNumber",
                "passwordHash", "$2b$", "accessToken", "signingKey");
        JsonNode result = json.readTree(body);
        if (expectedStatus >= 400) {
            assertThat(result.size()).isEqualTo(4);
            assertThat(result.path("status").asInt()).isEqualTo(expectedStatus);
            assertThat(result.path("code").asText()).isNotBlank();
            assertThat(result.path("message").asText()).isNotBlank();
            assertThat(result.path("timestamp").asText()).endsWith("Z");
            assertThat(Instant.parse(result.path("timestamp").asText())).isBeforeOrEqualTo(Instant.now());
            assertThat(body).doesNotContain("exception", "stackTrace");
        }
        return result;
    }

    private String id() { return UUID.randomUUID().toString(); }
}
