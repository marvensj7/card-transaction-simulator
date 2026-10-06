package com.marvens.capstone.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Fictional values only. These helpers create entity objects, not service behavior.
final class EntityFixtures {
    private EntityFixtures() {
    }

    static AppUser user() {
        AppUser user = new AppUser();
        user.setDisplayName("Mapping Test User");
        user.setEmail("mapping-" + UUID.randomUUID() + "@example.test");
        // Same BCrypt hash format as the SQL seed; no plaintext password is needed.
        user.setPasswordHash("$2b$12$MuK8D982Lf/BpY8IlIUbeuV5.ZoE0.qphVqR58DtC5byiysllvA7C");
        user.setRole(AppUser.Role.USER);
        return user;
    }

    static CreditAccount account(AppUser user) {
        CreditAccount account = new CreditAccount();
        account.setUser(user);
        account.setCreditLimit(new BigDecimal("1000.00"));
        account.setStatus(CreditAccount.Status.ACTIVE);
        return account;
    }

    static DemoCard card(CreditAccount account) {
        DemoCard card = new DemoCard();
        card.setAccount(account);
        card.setTestProfile("DEMO_4242");
        card.setLabel("Mapping Test Card");
        card.setLastFour("4242");
        card.setExpiryMonth((byte) 12);
        card.setExpiryYear((short) 2030);
        return card;
    }

    static CardTransaction purchase(CreditAccount account, DemoCard card) {
        CardTransaction transaction = new CardTransaction();
        transaction.setAccount(account);
        transaction.setCard(card);
        transaction.setType(CardTransaction.Type.PURCHASE);
        transaction.setStatus(CardTransaction.Status.APPROVED);
        transaction.setAmount(new BigDecimal("25.10"));
        transaction.setOutstandingAfter(new BigDecimal("25.10"));
        transaction.setMerchantName("Fictional Bookstore");
        transaction.setCreatedAt(LocalDateTime.of(2026, 10, 6, 15, 30, 0, 123456000));
        transaction.setRequestId(UUID.randomUUID().toString());
        return transaction;
    }
}
