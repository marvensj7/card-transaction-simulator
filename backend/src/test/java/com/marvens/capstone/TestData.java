package com.marvens.capstone;

import java.math.BigDecimal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import com.marvens.capstone.dto.PurchaseRequest;
import com.marvens.capstone.entity.AppUser;
import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.entity.DemoCard;

// Shared fictional fixtures. Nothing here is a production login shortcut.
class TestData {
    static AppUser user(AppUser.Role role) {
        AppUser user = new AppUser();
        user.setDisplayName("Demo Customer");
        user.setEmail("check-" + UUID.randomUUID() + "@example.test");
        user.setPasswordHash("$2b$12$MuK8D982Lf/BpY8IlIUbeuV5.ZoE0.qphVqR58DtC5byiysllvA7C");
        user.setRole(role);
        return user;
    }

    static CreditAccount account(AppUser user) {
        CreditAccount account = new CreditAccount();
        account.setUser(user);
        account.setCreditLimit(new BigDecimal("1000.00"));
        account.setOutstandingBalance(new BigDecimal("200.00"));
        account.setStatus(CreditAccount.Status.ACTIVE);
        return account;
    }

    static DemoCard card(CreditAccount account) {
        DemoCard card = new DemoCard();
        card.setAccount(account);
        card.setLabel("Demo Card");
        card.setTestProfile("DEMO_4242");
        card.setLastFour("4242");
        card.setExpiryMonth((byte) 12);
        card.setExpiryYear((short) 2035);
        return card;
    }

    static PurchaseRequest purchase(Long cardId, String amount) {
        PurchaseRequest request = new PurchaseRequest();
        request.cardId = cardId;
        request.testCardNumber = testNumber();
        request.expiryMonth = 12;
        request.expiryYear = 2035;
        request.testSecurityCode = "9".repeat(3);
        request.merchantName = "Demo Bookstore";
        request.amount = new BigDecimal(amount);
        request.requestId = UUID.randomUUID().toString();
        return request;
    }

    static String testNumber() {
        return "4242".repeat(4);
    }

    static ObjectNode purchaseJson(PurchaseRequest request) {
        ObjectMapper json = new ObjectMapper();
        ObjectNode body = json.valueToTree(request);
        // These fields are write-only, so normal response serialization deliberately omits them.
        body.put("testCardNumber", request.testCardNumber);
        body.put("testSecurityCode", request.testSecurityCode);
        return body;
    }
}
