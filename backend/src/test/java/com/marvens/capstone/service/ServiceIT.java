package com.marvens.capstone.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.marvens.capstone.entity.AppUser;
import com.marvens.capstone.entity.CardTransaction;
import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.entity.DemoCard;
import com.marvens.capstone.exception.*;
import com.marvens.capstone.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Real Spring transaction boundaries and existing MySQL tables. Only these fictional rows are cleaned up.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ServiceIT {
    @Autowired private AccountService accountService;
    @Autowired private TransactionService service;
    @Autowired private AppUserRepository users;
    @Autowired private CreditAccountRepository accounts;
    @Autowired private DemoCardRepository cards;
    @MockitoSpyBean private CardTransactionRepository transactions;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    private Long ownerId;
    private Long otherOwnerId;
    private Long adminId;
    private Long accountId;
    private Long cardId;

    @BeforeEach
    void createCommittedFictionalFixture() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            AppUser owner = saveUser(AppUser.Role.USER);
            ownerId = owner.getId();
            otherOwnerId = saveUser(AppUser.Role.USER).getId();
            adminId = saveUser(AppUser.Role.ADMIN).getId();
            CreditAccount account = new CreditAccount();
            account.setUser(owner);
            account.setCreditLimit(new BigDecimal("1000.00"));
            account.setOutstandingBalance(new BigDecimal("0.00"));
            account.setStatus(CreditAccount.Status.ACTIVE);
            accounts.saveAndFlush(account);
            accountId = account.getId();
            DemoCard card = new DemoCard();
            card.setAccount(account);
            card.setTestProfile("DEMO_4242");
            card.setLabel("Service Test Card");
            card.setLastFour("4242");
            card.setExpiryMonth((byte) 12);
            card.setExpiryYear((short) 2030);
            cards.saveAndFlush(card);
            cardId = card.getId();
        });
    }

    @AfterEach
    void removeOnlyFixtureRows() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            if (accountId != null) {
                jdbc.update("DELETE FROM card_transactions WHERE account_id = ? AND type = 'REFUND'", accountId);
                jdbc.update("DELETE FROM card_transactions WHERE account_id = ?", accountId);
                jdbc.update("DELETE FROM demo_cards WHERE account_id = ?", accountId);
                jdbc.update("DELETE FROM credit_accounts WHERE id = ?", accountId);
            }
            for (Long userId : new Long[] {ownerId, otherOwnerId, adminId}) {
                if (userId != null) jdbc.update("DELETE FROM app_users WHERE id = ?", userId);
            }
        });
    }

    @Test
    void purchaseDeclinesHistoryRefundAndRetryRoundTripThroughMySql() {
        String requestId = id();
        var approved = service.purchase(ownerId, accountId, command("50.01", requestId));
        assertThat(balance()).isEqualByComparingTo("50.01");
        assertThat(approved.transaction().getStatus()).isEqualTo(CardTransaction.Status.APPROVED);
        var insufficient = service.purchase(ownerId, accountId, command("950.00", id()));
        assertThat(insufficient.transaction().getReasonCode()).isEqualTo("INSUFFICIENT_CREDIT");
        accountService.changeStatus(adminId, accountId, CreditAccount.Status.FROZEN);
        var frozen = service.purchase(ownerId, accountId, command("1.00", id()));
        assertThat(frozen.transaction().getReasonCode()).isEqualTo("ACCOUNT_FROZEN");
        assertThat(balance()).isEqualByComparingTo("50.01");
        var retry = service.purchase(ownerId, accountId, command("50.01", requestId.toUpperCase()));
        assertThat(retry.replayed()).isTrue();
        assertThat(retry.transaction().getId()).isEqualTo(approved.transaction().getId());
        assertThatThrownBy(() -> service.purchase(ownerId, accountId, command("51.00", requestId)))
                .isInstanceOf(RequestConflictException.class);

        String refundId = id();
        var refund = service.refund(ownerId, approved.transaction().getId(), refundId);
        assertThat(refund.transaction().getOriginalPurchase().getId()).isEqualTo(approved.transaction().getId());
        assertThat(refund.transaction().getOutstandingAfter()).isEqualByComparingTo("0.00");
        assertThat(balance()).isEqualByComparingTo("0.00");
        assertThat(service.refund(ownerId, approved.transaction().getId(), refundId).replayed()).isTrue();
        assertThatThrownBy(() -> service.refund(ownerId, approved.transaction().getId(), id()))
                .isInstanceOf(RefundNotEligibleException.class);
        var history = service.getHistory(ownerId, accountId, 0, 2);
        assertThat(history.getTotalElements()).isEqualTo(4);
        assertThat(history.getContent()).extracting(CardTransaction::getId)
                .containsExactly(refund.transaction().getId(), frozen.transaction().getId());
        accountService.changeStatus(adminId, accountId, CreditAccount.Status.ACTIVE);
        assertThat(service.purchase(ownerId, accountId, command("1.00", id())).transaction().getStatus())
                .isEqualTo(CardTransaction.Status.APPROVED);
    }

    @Test
    void ownershipRolesAndRefundEligibilityAreEnforcedAgainstStoredRows() {
        var purchase = service.purchase(ownerId, accountId, command("5.00", id()));
        assertThat(accountService.getAccounts(ownerId)).extracting(CreditAccount::getId).containsExactly(accountId);
        assertThat(accountService.getCards(ownerId, accountId)).extracting(DemoCard::getId).containsExactly(cardId);
        assertThatThrownBy(() -> service.purchase(otherOwnerId, accountId, command("1.00", id())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> accountService.getCards(otherOwnerId, accountId)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.getHistory(otherOwnerId, accountId, null, null)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.refund(otherOwnerId, purchase.transaction().getId(), id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> accountService.getAdminAccounts(ownerId, null, null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.getAdminTransactions(ownerId, null, null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.purchase(adminId, accountId, command("1.00", id())))
                .isInstanceOf(AccessDeniedException.class);
        accountService.changeStatus(adminId, accountId, CreditAccount.Status.FROZEN);
        var declined = service.purchase(ownerId, accountId, command("1.00", id()));
        assertThatThrownBy(() -> service.refund(ownerId, declined.transaction().getId(), id()))
                .isInstanceOf(RefundNotEligibleException.class);
        var refund = service.refund(ownerId, purchase.transaction().getId(), id());
        assertThatThrownBy(() -> service.refund(ownerId, refund.transaction().getId(), id()))
                .isInstanceOf(RefundNotEligibleException.class);
        assertThat(accountService.getAdminAccounts(adminId, null, 100).getSize()).isEqualTo(50);
        assertThat(service.getAdminTransactions(adminId, null, null).getContent())
                .extracting(CardTransaction::getId).contains(refund.transaction().getId());
    }

    @Test
    void failureAfterBalanceFlushRollsBackPurchaseBalanceAndHistory() {
        doThrow(new IllegalStateException("Simulated history write failure")).when(transactions).saveAndFlush(any());
        assertThatThrownBy(() -> service.purchase(ownerId, accountId, command("50.00", id())))
                .isInstanceOf(IllegalStateException.class);
        assertThat(balance()).isEqualByComparingTo("0.00");
        assertThat(historyCount()).isZero();
    }

    @Test
    void failureAfterRefundHistoryFlushRollsBackBothWritesAndAllowsRetry() {
        var purchase = service.purchase(ownerId, accountId, command("50.00", id()));
        String requestId = id();
        doAnswer(invocation -> {
            transactions.save(invocation.getArgument(0));
            transactions.flush();
            throw new IllegalStateException("Simulated failure after history flush");
        }).when(transactions).saveAndFlush(any());
        assertThatThrownBy(() -> service.refund(ownerId, purchase.transaction().getId(), requestId))
                .isInstanceOf(IllegalStateException.class);
        assertThat(balance()).isEqualByComparingTo("50.00");
        assertThat(historyCount()).isEqualTo(1);
        reset(transactions);
        assertThat(service.refund(ownerId, purchase.transaction().getId(), requestId).replayed()).isFalse();
        assertThat(balance()).isEqualByComparingTo("0.00");
    }

    @Test
    void concurrentPurchasesCannotSpendTheSameAvailableCredit() throws Exception {
        var results = together(() -> service.purchase(ownerId, accountId, command("600.00", id())),
                () -> service.purchase(ownerId, accountId, command("600.00", id())));
        assertThat(results).extracting(result -> result.transaction().getStatus())
                .containsExactlyInAnyOrder(CardTransaction.Status.APPROVED, CardTransaction.Status.DECLINED);
        assertThat(balance()).isEqualByComparingTo("600.00");
        assertThat(historyCount()).isEqualTo(2);
    }

    @Test
    void concurrentIdenticalRetriesSaveOnePurchase() throws Exception {
        String requestId = id();
        var results = together(() -> service.purchase(ownerId, accountId, command("25.00", requestId)),
                () -> service.purchase(ownerId, accountId, command("25.00", requestId)));
        assertThat(results.get(0).transaction().getId()).isEqualTo(results.get(1).transaction().getId());
        assertThat(results).extracting(TransactionOutcome::replayed).containsExactlyInAnyOrder(false, true);
        assertThat(balance()).isEqualByComparingTo("25.00");
        assertThat(historyCount()).isEqualTo(1);
    }

    @Test
    void purchaseWaitsForAnExistingAccountWriteLock() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        var started = new CountDownLatch(1);
        try {
            var future = new TransactionTemplate(transactionManager).execute(status -> {
                accounts.findOwnedForUpdate(accountId, ownerId).orElseThrow();
                var pending = executor.submit(() -> {
                    started.countDown();
                    return service.purchase(ownerId, accountId, command("25.00", id()));
                });
                try {
                    assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                    assertThatThrownBy(() -> pending.get(300, TimeUnit.MILLISECONDS))
                            .isInstanceOf(TimeoutException.class);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
                return pending;
            });
            assertThat(future.get(10, TimeUnit.SECONDS).transaction().getStatus())
                    .isEqualTo(CardTransaction.Status.APPROVED);
            assertThat(balance()).isEqualByComparingTo("25.00");
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void concurrentRefundsCanReverseAPurchaseOnlyOnce() throws Exception {
        var purchase = service.purchase(ownerId, accountId, command("25.00", id()));
        Callable<Boolean> refund = () -> {
            try {
                service.refund(ownerId, purchase.transaction().getId(), id());
                return true;
            } catch (RefundNotEligibleException expected) {
                return false;
            }
        };
        assertThat(together(refund, refund)).containsExactlyInAnyOrder(true, false);
        assertThat(balance()).isEqualByComparingTo("0.00");
        assertThat(historyCount()).isEqualTo(2);
    }

    private <T> List<T> together(Callable<T> first, Callable<T> second) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var firstResult = executor.submit(() -> { ready.countDown(); start.await(); return first.call(); });
            var secondResult = executor.submit(() -> { ready.countDown(); start.await(); return second.call(); });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(firstResult.get(20, TimeUnit.SECONDS), secondResult.get(20, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private AppUser saveUser(AppUser.Role role) {
        AppUser user = new AppUser();
        user.setDisplayName("Service Test User");
        user.setEmail("service-" + id() + "@example.test");
        user.setPasswordHash("$2b$12$MuK8D982Lf/BpY8IlIUbeuV5.ZoE0.qphVqR58DtC5byiysllvA7C");
        user.setRole(role);
        return users.saveAndFlush(user);
    }

    private BigDecimal balance() {
        return jdbc.queryForObject("SELECT outstanding_balance FROM credit_accounts WHERE id = ?", BigDecimal.class, accountId);
    }

    private long historyCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM card_transactions WHERE account_id = ?", Long.class, accountId);
    }

    private PurchaseCommand command(String amount, String requestId) {
        return new PurchaseCommand(cardId, TransactionServiceTest.testNumber(), 12, 2030,
                TransactionServiceTest.testCode(), "Demo Shop", new BigDecimal(amount), requestId);
    }

    private String id() { return UUID.randomUUID().toString(); }
}
