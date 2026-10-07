package com.marvens.capstone.service;

import com.marvens.capstone.dto.PurchaseRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marvens.capstone.entity.AppUser;
import com.marvens.capstone.entity.CardTransaction;
import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.entity.DemoCard;
import com.marvens.capstone.exception.*;
import com.marvens.capstone.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionServiceTest {
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final CreditAccountRepository accounts = mock(CreditAccountRepository.class);
    private final DemoCardRepository cards = mock(DemoCardRepository.class);
    private final CardTransactionRepository transactions = mock(CardTransactionRepository.class);
    private final AccountService accountService = new AccountService(users, accounts, cards);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-06T15:30:00.123456789Z"), ZoneOffset.ofHours(-4));
    private final TransactionService service = new TransactionService(accountService, accounts, cards, transactions, clock);
    private CreditAccount account;
    private DemoCard card;
    private String requestId;

    @BeforeEach
    void setUp() {
        account = new CreditAccount();
        ReflectionTestUtils.setField(account, "id", 7L);
        account.setCreditLimit(new BigDecimal("1000.00"));
        account.setOutstandingBalance(new BigDecimal("200.00"));
        account.setStatus(CreditAccount.Status.ACTIVE);
        card = new DemoCard();
        ReflectionTestUtils.setField(card, "id", 8L);
        card.setAccount(account);
        card.setTestProfile("DEMO_4242");
        card.setLastFour("4242");
        card.setExpiryMonth((byte) 12);
        card.setExpiryYear((short) 2030);
        requestId = UUID.randomUUID().toString();
        when(users.findRoleById(1L)).thenReturn(Optional.of(AppUser.Role.USER));
        when(accounts.findOwnedForUpdate(7L, 1L)).thenReturn(Optional.of(account));
        when(cards.findByIdAndAccount_Id(8L, 7L)).thenReturn(Optional.of(card));
        when(transactions.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void approvalUsesExactMoneyAndUtcMicroseconds() {
        var result = service.purchase(1L, 7L, command("50.01"));
        assertThat(result.transaction().getStatus()).isEqualTo(CardTransaction.Status.APPROVED);
        assertThat(account.getOutstandingBalance()).isEqualByComparingTo("250.01");
        assertThat(result.transaction().getOutstandingAfter()).isEqualByComparingTo("250.01");
        assertThat(result.transaction().getCreatedAt().toString()).isEqualTo("2026-10-06T15:30:00.123456");
        assertThat(result.replayed()).isFalse();
        verify(accounts).saveAndFlush(account);
    }

    @Test
    void insufficientCreditRecordsDeclineWithoutChangingBalance() {
        var result = service.purchase(1L, 7L, command("800.01"));
        assertDecline(result, "INSUFFICIENT_CREDIT");
    }

    @Test
    void frozenAccountRecordsDeclineEvenWithAvailableCredit() {
        account.setStatus(CreditAccount.Status.FROZEN);
        assertDecline(service.purchase(1L, 7L, command("1.00")), "ACCOUNT_FROZEN");
    }

    @Test
    void declinedRetryKeepsItsSavedOutcomeAfterReactivation() {
        account.setStatus(CreditAccount.Status.FROZEN);
        var declined = service.purchase(1L, 7L, command("1.00"));
        when(transactions.findByAccount_IdAndRequestId(7L, requestId)).thenReturn(Optional.of(declined.transaction()));
        account.setStatus(CreditAccount.Status.ACTIVE);
        var retry = service.purchase(1L, 7L, command("1.00"));
        assertThat(retry.replayed()).isTrue();
        assertThat(retry.transaction().getReasonCode()).isEqualTo("ACCOUNT_FROZEN");
        assertThat(account.getOutstandingBalance()).isEqualByComparingTo("200.00");
        verify(transactions, times(1)).saveAndFlush(any());
    }

    @Test
    void inputAndOutcomeSerializationExcludeSensitiveCardFields() throws Exception {
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var command = command("50.00");
        String input = mapper.writeValueAsString(command);
        String output = mapper.writeValueAsString(service.purchase(1L, 7L, command));
        for (String text : new String[] {input, output, command.toString()}) {
            assertThat(text.contains(testNumber())).isFalse();
            assertThat(text.contains(testCode())).isFalse();
            assertThat(text).doesNotContain("testCardNumber", "testSecurityCode");
        }
    }

    @Test
    void expiryUsesUtcMonthEvenWhenInjectedClockHasAnotherZone() {
        var boundaryClock = Clock.fixed(Instant.parse("2026-11-01T00:30:00Z"), ZoneOffset.ofHours(-4));
        var boundaryService = new TransactionService(accountService, accounts, cards, transactions, boundaryClock);
        card.setExpiryMonth((byte) 10);
        card.setExpiryYear((short) 2026);
        var command = new PurchaseRequest(8L, testNumber(), 10, 2026, testCode(),
                "Demo Shop", BigDecimal.ONE, requestId);
        assertDecline(boundaryService.purchase(1L, 7L, command), "CARD_EXPIRED");
    }

    @Test
    void matchingExpiredCardRecordsDecline() {
        card.setExpiryYear((short) 2025);
        var command = new PurchaseRequest(8L, testNumber(), 12, 2025, testCode(),
                "Demo Shop", new BigDecimal("1.00"), requestId);
        assertDecline(service.purchase(1L, 7L, command), "CARD_EXPIRED");
    }

    @Test
    void cardIsValidThroughTheLastDayOfItsExpiryMonth() {
        card.setExpiryMonth((byte) 10);
        card.setExpiryYear((short) 2026);
        var command = new PurchaseRequest(8L, testNumber(), 10, 2026, testCode(),
                "Demo Shop", new BigDecimal("1.00"), requestId);
        assertThat(service.purchase(1L, 7L, command).transaction().getStatus())
                .isEqualTo(CardTransaction.Status.APPROVED);
    }

    @Test
    void unownedAccountOrCardCreatesNoHistory() {
        assertThatThrownBy(() -> service.purchase(1L, 9L, command("1.00")))
                .isInstanceOf(ResourceNotFoundException.class);
        when(cards.findByIdAndAccount_Id(8L, 7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.purchase(1L, 7L, command("1.00")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(transactions, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-1.00", "1.001", "1000000000000.00"})
    void invalidAmountsCreateNoHistory(String amount) {
        assertThatThrownBy(() -> service.purchase(1L, 7L, command(amount)))
                .isInstanceOf(InvalidPurchaseException.class);
        verify(transactions, never()).saveAndFlush(any());
    }

    @Test
    void nullInputAndMalformedRequestIdFailClearly() {
        assertThatThrownBy(() -> service.purchase(1L, 7L, null)).isInstanceOf(InvalidPurchaseException.class);
        requestId = "bad-id";
        assertThatThrownBy(() -> service.purchase(1L, 7L, command("1.00")))
                .isInstanceOf(InvalidRequestException.class);
        verify(transactions, never()).saveAndFlush(any());
    }

    @Test
    void unrecognizedNumberWrongExpiryAndMalformedCodeCreateNoHistory() {
        for (PurchaseRequest command : new PurchaseRequest[] {
                new PurchaseRequest(8L, "0".repeat(16), 12, 2030, testCode(), "Demo Shop", BigDecimal.ONE, requestId),
                new PurchaseRequest(8L, testNumber(), 11, 2030, testCode(), "Demo Shop", BigDecimal.ONE, requestId),
                new PurchaseRequest(8L, testNumber(), 12, 2030, "x", "Demo Shop", BigDecimal.ONE, requestId)}) {
            assertThatThrownBy(() -> service.purchase(1L, 7L, command)).isInstanceOf(InvalidPurchaseException.class);
        }
        verify(transactions, never()).saveAndFlush(any());
    }

    @Test
    void identicalRetryReturnsSavedOutcomeEvenAfterAccountIsFrozen() {
        var first = service.purchase(1L, 7L, command("50.00"));
        when(transactions.findByAccount_IdAndRequestId(7L, requestId)).thenReturn(Optional.of(first.transaction()));
        account.setStatus(CreditAccount.Status.FROZEN);
        var retry = service.purchase(1L, 7L, command("50"));
        assertThat(retry.transaction()).isSameAs(first.transaction());
        assertThat(retry.replayed()).isTrue();
        assertThat(account.getOutstandingBalance()).isEqualByComparingTo("250.00");
        verify(transactions, times(1)).saveAndFlush(any());
    }

    @Test
    void changedAmountMerchantCardOrTransactionTypeConflicts() {
        var first = service.purchase(1L, 7L, command("50.00")).transaction();
        when(transactions.findByAccount_IdAndRequestId(7L, requestId)).thenReturn(Optional.of(first));
        for (PurchaseRequest changed : new PurchaseRequest[] {
                command("51.00"),
                new PurchaseRequest(8L, testNumber(), 12, 2030, testCode(), "Other Shop", new BigDecimal("50.00"), requestId),
                new PurchaseRequest(9L, testNumber(), 12, 2030, testCode(), "Demo Shop", new BigDecimal("50.00"), requestId)}) {
            assertThatThrownBy(() -> service.purchase(1L, 7L, changed)).isInstanceOf(RequestConflictException.class);
        }
        first.setType(CardTransaction.Type.REFUND);
        assertThatThrownBy(() -> service.purchase(1L, 7L, command("50.00"))).isInstanceOf(RequestConflictException.class);
        verify(transactions, times(1)).saveAndFlush(any());
    }

    @Test
    void fullRefundOnFrozenAccountCopiesOriginalDetailsAndCanBeRetried() {
        CardTransaction purchase = ownedPurchase();
        account.setStatus(CreditAccount.Status.FROZEN);
        var result = service.refund(1L, 42L, requestId);
        assertThat(result.transaction().getOriginalPurchase()).isSameAs(purchase);
        assertThat(result.transaction().getAmount()).isEqualByComparingTo("50.00");
        assertThat(result.transaction().getMerchantName()).isEqualTo("Demo Shop");
        assertThat(result.transaction().getCard()).isSameAs(card);
        assertThat(account.getOutstandingBalance()).isEqualByComparingTo("150.00");
        when(transactions.findByAccount_IdAndRequestId(7L, requestId)).thenReturn(Optional.of(result.transaction()));
        assertThat(service.refund(1L, 42L, requestId).replayed()).isTrue();
        verify(transactions, times(1)).saveAndFlush(any());
    }

    @Test
    void declinedPurchaseRefundRowAlreadyRefundedAndLowBalanceAreIneligible() {
        CardTransaction purchase = ownedPurchase();
        purchase.setStatus(CardTransaction.Status.DECLINED);
        assertRefundIneligible();
        purchase.setStatus(CardTransaction.Status.APPROVED);
        purchase.setType(CardTransaction.Type.REFUND);
        assertRefundIneligible();
        purchase.setType(CardTransaction.Type.PURCHASE);
        when(transactions.findByOriginalPurchase_Id(42L)).thenReturn(Optional.of(new CardTransaction()));
        assertRefundIneligible();
        when(transactions.findByOriginalPurchase_Id(42L)).thenReturn(Optional.empty());
        account.setOutstandingBalance(BigDecimal.ZERO);
        assertRefundIneligible();
        verify(transactions, never()).saveAndFlush(any());
    }

    @Test
    void unownedRefundAndReuseForDifferentPurchaseFail() {
        assertThatThrownBy(() -> service.refund(1L, 99L, requestId)).isInstanceOf(ResourceNotFoundException.class);
        CardTransaction purchase = ownedPurchase();
        CardTransaction saved = new CardTransaction();
        saved.setType(CardTransaction.Type.REFUND);
        CardTransaction other = new CardTransaction();
        ReflectionTestUtils.setField(other, "id", 43L);
        saved.setOriginalPurchase(other);
        when(transactions.findByAccount_IdAndRequestId(7L, requestId)).thenReturn(Optional.of(saved));
        assertThatThrownBy(() -> service.refund(1L, purchase.getId(), requestId)).isInstanceOf(RequestConflictException.class);
        saved.setType(CardTransaction.Type.PURCHASE);
        assertThatThrownBy(() -> service.refund(1L, purchase.getId(), requestId)).isInstanceOf(RequestConflictException.class);
        verify(transactions, never()).saveAndFlush(any());
    }

    private CardTransaction ownedPurchase() {
        CardTransaction purchase = new CardTransaction();
        ReflectionTestUtils.setField(purchase, "id", 42L);
        purchase.setAccount(account);
        purchase.setCard(card);
        purchase.setType(CardTransaction.Type.PURCHASE);
        purchase.setStatus(CardTransaction.Status.APPROVED);
        purchase.setAmount(new BigDecimal("50.00"));
        purchase.setMerchantName("Demo Shop");
        when(transactions.findOwnedAccountId(42L, 1L)).thenReturn(Optional.of(7L));
        when(transactions.findOwnedById(42L, 1L)).thenReturn(Optional.of(purchase));
        return purchase;
    }

    private void assertRefundIneligible() {
        assertThatThrownBy(() -> service.refund(1L, 42L, requestId)).isInstanceOf(RefundNotEligibleException.class);
    }

    private void assertDecline(TransactionOutcome result, String reason) {
        assertThat(result.transaction().getStatus()).isEqualTo(CardTransaction.Status.DECLINED);
        assertThat(result.transaction().getReasonCode()).isEqualTo(reason);
        assertThat(result.transaction().getOutstandingAfter()).isEqualByComparingTo("200.00");
        assertThat(account.getOutstandingBalance()).isEqualByComparingTo("200.00");
        verify(accounts, never()).saveAndFlush(any());
    }

    private PurchaseRequest command(String amount) {
        return new PurchaseRequest(8L, testNumber(), 12, 2030, testCode(), "Demo Shop", new BigDecimal(amount), requestId);
    }

    // Test input is kept out of test names, parameter labels, logs, and results.
    static String testNumber() { return "4242".repeat(4); }
    static String testCode() { return "1".repeat(3); }
}
