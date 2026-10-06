package com.marvens.capstone.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.marvens.capstone.entity.*;
import org.springframework.test.util.ReflectionTestUtils;

final class ControllerFixtures {
    private ControllerFixtures() { }

    static CreditAccount account() {
        var owner = new AppUser();
        ReflectionTestUtils.setField(owner, "id", 9L);
        owner.setDisplayName("Demo Owner");
        owner.setEmail("owner@example.test");
        owner.setRole(AppUser.Role.USER);
        owner.setPasswordHash("fictional-secret-hash-marker");
        var account = new CreditAccount();
        ReflectionTestUtils.setField(account, "id", 7L);
        account.setUser(owner);
        account.setCreditLimit(new BigDecimal("1000"));
        account.setOutstandingBalance(new BigDecimal("25"));
        account.setStatus(CreditAccount.Status.ACTIVE);
        return account;
    }

    static DemoCard card(CreditAccount account) {
        var card = new DemoCard();
        ReflectionTestUtils.setField(card, "id", 7L);
        card.setAccount(account);
        card.setLabel("Demo Card");
        card.setLastFour("4242");
        card.setExpiryMonth((byte) 12);
        card.setExpiryYear((short) 2030);
        card.setTestProfile("DEMO_4242");
        return card;
    }

    static CardTransaction purchase(CreditAccount account) {
        var transaction = new CardTransaction();
        ReflectionTestUtils.setField(transaction, "id", 42L);
        transaction.setAccount(account);
        transaction.setCard(card(account));
        transaction.setType(CardTransaction.Type.PURCHASE);
        transaction.setStatus(CardTransaction.Status.APPROVED);
        transaction.setAmount(new BigDecimal("25"));
        transaction.setOutstandingAfter(new BigDecimal("25"));
        transaction.setMerchantName("Demo Shop");
        transaction.setCreatedAt(LocalDateTime.of(2026, 10, 6, 15, 30, 0, 123456000));
        transaction.setRequestId("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        return transaction;
    }
}
