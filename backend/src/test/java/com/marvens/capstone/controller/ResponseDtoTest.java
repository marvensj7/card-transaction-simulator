package com.marvens.capstone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvens.capstone.dto.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ResponseDtoTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void responsesUseDecimalTextUtcAndOnlyTheDocumentedFields() throws Exception {
        var account = ControllerFixtures.account();
        var transaction = ControllerFixtures.purchase(account);
        var accountJson = json.readTree(json.writeValueAsString(AccountResponse.from(account)));
        assertThat(accountJson.get("creditLimit").isTextual()).isTrue();
        assertThat(accountJson.get("outstandingBalance").asText()).isEqualTo("25.00");
        assertThat(accountJson.get("availableCredit").asText()).isEqualTo("975.00");
        assertThat(accountJson.size()).isEqualTo(5);
        var cardJson = json.readTree(json.writeValueAsString(CardResponse.from(transaction.getCard())));
        assertThat(cardJson.get("maskedNumber").asText()).isEqualTo("\u2022\u2022\u2022\u2022 4242");
        assertThat(cardJson.size()).isEqualTo(6);
        var transactionJson = json.readTree(json.writeValueAsString(TransactionResponse.from(transaction)));
        assertThat(transactionJson.get("amount").asText()).isEqualTo("25.00");
        assertThat(transactionJson.get("createdAt").asText()).isEqualTo("2026-10-06T15:30:00.123456Z");
        assertThat(transactionJson.get("reasonCode").isNull()).isTrue();
        assertThat(transactionJson.get("originalPurchaseId").isNull()).isTrue();
        assertThat(transactionJson.size()).isEqualTo(11);
        String adminJson = json.writeValueAsString(AdminAccountResponse.from(account))
                + json.writeValueAsString(AdminTransactionResponse.from(transaction));
        assertThat(adminJson).contains("owner@example.test").doesNotContain("passwordHash",
                "fictional-secret-hash-marker", "testCardNumber", "testSecurityCode", "requestId",
                RequestDtoTest.testNumber(), "accessToken", "signingKey");
    }
}
