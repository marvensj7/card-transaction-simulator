package com.marvens.capstone.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import com.marvens.capstone.entity.AppUser;
import com.marvens.capstone.entity.CardTransaction;
import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.entity.DemoCard;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

// Uses the existing MySQL schema with -Pmysql-verification; each test rolls back.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class RepositoryIT {
    @Autowired
    private AppUserRepository users;
    @Autowired
    private CreditAccountRepository accounts;
    @Autowired
    private DemoCardRepository cards;
    @Autowired
    private CardTransactionRepository transactions;
    @Autowired
    private JdbcTemplate jdbc;
    @PersistenceContext
    private EntityManager entityManager;

    private AppUser owner;
    private AppUser otherOwner;
    private CreditAccount account;
    private CreditAccount otherAccount;
    private DemoCard card;
    private DemoCard otherCard;
    private long existingAccountCount;
    private long existingTransactionCount;

    @BeforeEach
    void saveTwoFictionalCustomers() {
        existingAccountCount = jdbc.queryForObject("SELECT COUNT(*) FROM credit_accounts", Long.class);
        existingTransactionCount = jdbc.queryForObject("SELECT COUNT(*) FROM card_transactions", Long.class);
        owner = saveUser(AppUser.Role.USER);
        otherOwner = saveUser(AppUser.Role.USER);
        account = saveAccount(owner);
        otherAccount = saveAccount(otherOwner);
        card = saveCard(account);
        otherCard = saveCard(otherAccount);
        reloadFromDatabase();
    }

    @Test
    void emailLookupUsesMySqlCollationAndReturnsEmptyForMissingEmail() {
        assertThat(users.findByEmail(owner.getEmail()).map(AppUser::getId)).contains(owner.getId());
        assertThat(users.findByEmail(owner.getEmail().toUpperCase(Locale.ROOT)).map(AppUser::getId))
                .contains(owner.getId());
        assertThat(users.findByEmail("missing-" + UUID.randomUUID() + "@example.test")).isEmpty();
    }

    @Test
    void accountLookupsFilterByOwnerAndAllowAnAdminWithoutAnAccount() {
        AppUser admin = saveUser(AppUser.Role.ADMIN);
        reloadFromDatabase();

        assertThat(accounts.findByUser_Id(owner.getId()).map(CreditAccount::getId)).contains(account.getId());
        assertThat(accounts.findByUser_Id(otherOwner.getId()).map(CreditAccount::getId))
                .contains(otherAccount.getId());
        assertThat(accounts.findByUser_Id(admin.getId())).isEmpty();
        assertThat(accounts.findByIdAndUser_Id(account.getId(), owner.getId()).map(CreditAccount::getId))
                .contains(account.getId());
        assertThat(accounts.findByIdAndUser_Id(account.getId(), otherOwner.getId())).isEmpty();
        assertThat(accounts.findByIdAndUser_Id(0L, owner.getId())).isEmpty();
    }

    @Test
    void cardLookupsReturnOnlyTheCardAssignedToTheAccount() {
        assertThat(cards.findByAccount_Id(account.getId()).map(DemoCard::getId)).contains(card.getId());
        assertThat(cards.findByAccount_Id(otherAccount.getId()).map(DemoCard::getId)).contains(otherCard.getId());
        assertThat(cards.findByAccount_Id(0L)).isEmpty();
        assertThat(cards.findByIdAndAccount_Id(card.getId(), account.getId()).map(DemoCard::getId))
                .contains(card.getId());
        assertThat(cards.findByIdAndAccount_Id(otherCard.getId(), account.getId())).isEmpty();
        assertThat(cards.findByIdAndAccount_Id(0L, account.getId())).isEmpty();
    }

    @Test
    void requestIdLookupIsScopedToItsAccount() {
        String requestId = UUID.randomUUID().toString();
        CardTransaction purchase = purchase(account, card);
        purchase.setRequestId(requestId);
        transactions.save(purchase);
        CardTransaction otherPurchase = purchase(otherAccount, otherCard);
        otherPurchase.setRequestId(requestId);
        transactions.save(otherPurchase);
        reloadFromDatabase();

        assertThat(transactions.findByAccount_IdAndRequestId(account.getId(), requestId)
                .map(CardTransaction::getId)).contains(purchase.getId());
        assertThat(transactions.findByAccount_IdAndRequestId(otherAccount.getId(), requestId)
                .map(CardTransaction::getId)).contains(otherPurchase.getId());
        assertThat(transactions.findByAccount_IdAndRequestId(0L, requestId)).isEmpty();
        assertThat(transactions.findByAccount_IdAndRequestId(account.getId(), UUID.randomUUID().toString()))
                .isEmpty();
    }

    @Test
    void ownedTransactionQueryFollowsTheAccountToItsUser() {
        CardTransaction purchase = transactions.save(purchase(account, card));
        CardTransaction otherPurchase = transactions.save(purchase(otherAccount, otherCard));
        reloadFromDatabase();

        assertThat(transactions.findOwnedById(purchase.getId(), owner.getId()).map(CardTransaction::getId))
                .contains(purchase.getId());
        assertThat(transactions.findOwnedById(purchase.getId(), otherOwner.getId())).isEmpty();
        assertThat(transactions.findOwnedById(otherPurchase.getId(), owner.getId())).isEmpty();
        assertThat(transactions.findOwnedById(0L, owner.getId())).isEmpty();
    }

    @Test
    void refundLookupReturnsTheLinkedRefundInsteadOfThePurchase() {
        CardTransaction purchase = transactions.save(purchase(account, card));
        CardTransaction unrefundedPurchase = transactions.save(purchase(account, card));
        CardTransaction refund = transactions.save(refund(purchase));
        reloadFromDatabase();

        assertThat(transactions.findByOriginalPurchase_Id(purchase.getId()).map(CardTransaction::getId))
                .contains(refund.getId());
        assertThat(transactions.findByOriginalPurchase_Id(unrefundedPurchase.getId())).isEmpty();
        assertThat(transactions.findByOriginalPurchase_Id(refund.getId())).isEmpty();
        assertThat(transactions.findByOriginalPurchase_Id(0L)).isEmpty();
    }

    @Test
    void customerHistoryIncludesAllOutcomesInNewestIdOrderAcrossPages() {
        CardTransaction first = transactions.save(purchase(account, card));
        CardTransaction declined = purchase(account, card);
        declined.setStatus(CardTransaction.Status.DECLINED);
        declined.setReasonCode("ACCOUNT_FROZEN");
        declined.setOutstandingAfter(new BigDecimal("0.00"));
        transactions.save(declined);
        transactions.save(purchase(otherAccount, otherCard));
        CardTransaction refund = transactions.save(refund(first));
        // All timestamps match: the append-only ID determines a stable page order.
        reloadFromDatabase();

        var firstPage = transactions.findByAccount_IdAndAccount_User_IdOrderByIdDesc(
                account.getId(), owner.getId(), PageRequest.of(0, 2));
        var secondPage = transactions.findByAccount_IdAndAccount_User_IdOrderByIdDesc(
                account.getId(), owner.getId(), PageRequest.of(1, 2));
        var pastEnd = transactions.findByAccount_IdAndAccount_User_IdOrderByIdDesc(
                account.getId(), owner.getId(), PageRequest.of(2, 2));

        assertThat(firstPage.getContent()).extracting(CardTransaction::getId)
                .containsExactly(refund.getId(), declined.getId());
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getNumber()).isZero();
        assertThat(firstPage.getSize()).isEqualTo(2);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.getContent()).extracting(CardTransaction::getId).containsExactly(first.getId());
        assertThat(secondPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.getNumber()).isEqualTo(1);
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(pastEnd.getContent()).isEmpty();
        assertThat(pastEnd.getTotalElements()).isEqualTo(3);

        var unowned = transactions.findByAccount_IdAndAccount_User_IdOrderByIdDesc(
                account.getId(), otherOwner.getId(), PageRequest.of(0, 20));
        var missing = transactions.findByAccount_IdAndAccount_User_IdOrderByIdDesc(
                0L, owner.getId(), PageRequest.of(0, 20));
        assertThat(unowned.getContent()).isEmpty();
        assertThat(unowned.getTotalElements()).isZero();
        assertThat(missing.getContent()).isEmpty();
        assertThat(missing.getTotalElements()).isZero();
    }

    @Test
    void adminAccountListPagesAcrossOwnersInIdOrder() {
        // Existing seed rows can be present; our new accounts are the last two IDs.
        int firstNewAccountPage = Math.toIntExact(existingAccountCount);
        var firstPage = accounts.findAllByOrderByIdAsc(PageRequest.of(firstNewAccountPage, 1));
        var secondPage = accounts.findAllByOrderByIdAsc(PageRequest.of(firstNewAccountPage + 1, 1));
        var pastEnd = accounts.findAllByOrderByIdAsc(PageRequest.of(firstNewAccountPage + 2, 1));

        assertThat(firstPage.getContent()).extracting(CreditAccount::getId).containsExactly(account.getId());
        assertThat(firstPage.getTotalElements()).isEqualTo(existingAccountCount + 2);
        assertThat(firstPage.getTotalPages()).isEqualTo(firstNewAccountPage + 2);
        assertThat(firstPage.getNumber()).isEqualTo(firstNewAccountPage);
        assertThat(firstPage.getSize()).isEqualTo(1);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.getContent()).extracting(CreditAccount::getId).containsExactly(otherAccount.getId());
        assertThat(secondPage.getTotalElements()).isEqualTo(existingAccountCount + 2);
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(pastEnd.getContent()).isEmpty();
        assertThat(pastEnd.getTotalElements()).isEqualTo(existingAccountCount + 2);
    }

    @Test
    void adminTransactionListPagesAcrossOwnersWithNewestIdsFirst() {
        CardTransaction first = transactions.save(purchase(account, card));
        CardTransaction second = transactions.save(purchase(otherAccount, otherCard));
        CardTransaction third = transactions.save(refund(first));
        reloadFromDatabase();

        var firstPage = transactions.findAllByOrderByIdDesc(PageRequest.of(0, 2));
        var secondPage = transactions.findAllByOrderByIdDesc(PageRequest.of(1, 2));
        int pageAfterEnd = Math.toIntExact((existingTransactionCount + 3 + 1) / 2);
        var pastEnd = transactions.findAllByOrderByIdDesc(PageRequest.of(pageAfterEnd, 2));

        assertThat(firstPage.getContent()).extracting(CardTransaction::getId)
                .containsExactly(third.getId(), second.getId());
        assertThat(firstPage.getTotalElements()).isEqualTo(existingTransactionCount + 3);
        assertThat(firstPage.getTotalPages()).isEqualTo(pageAfterEnd);
        assertThat(firstPage.getSize()).isEqualTo(2);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.getContent().get(0).getId()).isEqualTo(first.getId());
        assertThat(secondPage.getTotalElements()).isEqualTo(existingTransactionCount + 3);
        assertThat(secondPage.getNumber()).isEqualTo(1);
        assertThat(pastEnd.getContent()).isEmpty();
        assertThat(pastEnd.getTotalElements()).isEqualTo(existingTransactionCount + 3);
    }

    private AppUser saveUser(AppUser.Role role) {
        AppUser user = new AppUser();
        user.setDisplayName("Repository Test User");
        user.setEmail("repository-" + UUID.randomUUID() + "@example.test");
        user.setPasswordHash("$2b$12$MuK8D982Lf/BpY8IlIUbeuV5.ZoE0.qphVqR58DtC5byiysllvA7C");
        user.setRole(role);
        return users.save(user);
    }

    private CreditAccount saveAccount(AppUser user) {
        CreditAccount account = new CreditAccount();
        account.setUser(user);
        account.setCreditLimit(new BigDecimal("1000.00"));
        account.setStatus(CreditAccount.Status.ACTIVE);
        user.setCreditAccount(account);
        return accounts.save(account);
    }

    private DemoCard saveCard(CreditAccount account) {
        DemoCard card = new DemoCard();
        card.setAccount(account);
        card.setTestProfile("DEMO_4242");
        card.setLabel("Repository Test Card");
        card.setLastFour("4242");
        card.setExpiryMonth((byte) 12);
        card.setExpiryYear((short) 2030);
        account.setDemoCard(card);
        return cards.save(card);
    }

    private CardTransaction purchase(CreditAccount account, DemoCard card) {
        CardTransaction purchase = new CardTransaction();
        purchase.setAccount(account);
        purchase.setCard(card);
        purchase.setType(CardTransaction.Type.PURCHASE);
        purchase.setStatus(CardTransaction.Status.APPROVED);
        purchase.setAmount(new BigDecimal("25.00"));
        purchase.setOutstandingAfter(new BigDecimal("25.00"));
        purchase.setMerchantName("Fictional Bookstore");
        purchase.setCreatedAt(LocalDateTime.of(2026, 10, 6, 15, 30, 0, 123456000));
        purchase.setRequestId(UUID.randomUUID().toString());
        return purchase;
    }

    private CardTransaction refund(CardTransaction purchase) {
        CardTransaction refund = purchase(purchase.getAccount(), purchase.getCard());
        refund.setType(CardTransaction.Type.REFUND);
        refund.setOriginalPurchase(purchase);
        refund.setAmount(purchase.getAmount());
        refund.setMerchantName(purchase.getMerchantName());
        refund.setOutstandingAfter(new BigDecimal("0.00"));
        return refund;
    }

    private void reloadFromDatabase() {
        entityManager.flush();
        entityManager.clear();
    }
}
