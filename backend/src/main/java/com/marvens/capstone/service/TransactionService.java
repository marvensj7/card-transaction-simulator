package com.marvens.capstone.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import com.marvens.capstone.entity.CardTransaction;
import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.entity.DemoCard;
import com.marvens.capstone.exception.InvalidPurchaseException;
import com.marvens.capstone.exception.RefundNotEligibleException;
import com.marvens.capstone.exception.RequestConflictException;
import com.marvens.capstone.exception.ResourceNotFoundException;
import com.marvens.capstone.repository.CardTransactionRepository;
import com.marvens.capstone.repository.CreditAccountRepository;
import com.marvens.capstone.repository.DemoCardRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

@Service
@Transactional(readOnly = true)
public class TransactionService {
    private final AccountService accountService;
    private final CreditAccountRepository accounts;
    private final DemoCardRepository cards;
    private final CardTransactionRepository transactions;
    private final Clock clock;

    public TransactionService(AccountService accountService, CreditAccountRepository accounts,
                              DemoCardRepository cards, CardTransactionRepository transactions, Clock clock) {
        this.accountService = accountService;
        this.accounts = accounts;
        this.cards = cards;
        this.transactions = transactions;
        this.clock = clock;
    }

    // Read committed lets a waiting retry see history committed by the lock holder.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionOutcome purchase(Long userId, Long accountId, PurchaseCommand command) {
        accountService.requireCustomer(userId);
        CreditAccount account = lockOwnedAccount(userId, accountId);
        validatePurchaseFields(command);
        String requestId = RequestChecks.requestId(command.getRequestId());
        // Lock first: a simultaneous retry waits until the first result commits.
        CardTransaction saved = transactions.findByAccount_IdAndRequestId(accountId, requestId).orElse(null);
        if (saved != null) {
            if (saved.getType() != CardTransaction.Type.PURCHASE
                    || !saved.getCard().getId().equals(command.getCardId())
                    || !saved.getMerchantName().equals(command.getMerchantName())
                    || saved.getAmount().compareTo(command.getAmount()) != 0) {
                throw new RequestConflictException();
            }
        }

        DemoCard card = cards.findByIdAndAccount_Id(command.getCardId(), accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Card is unavailable."));
        validateAssignedCard(command, card);
        if (saved != null) {
            return new TransactionOutcome(saved, account, true);
        }

        String reason = null;
        if (YearMonth.of(card.getExpiryYear(), card.getExpiryMonth())
                .isBefore(YearMonth.now(clock.withZone(ZoneOffset.UTC)))) {
            reason = "CARD_EXPIRED";
        } else if (account.getStatus() == CreditAccount.Status.FROZEN) {
            reason = "ACCOUNT_FROZEN";
        } else if (command.getAmount().compareTo(
                account.getCreditLimit().subtract(account.getOutstandingBalance())) > 0) {
            reason = "INSUFFICIENT_CREDIT";
        }

        if (reason == null) {
            account.setOutstandingBalance(account.getOutstandingBalance().add(command.getAmount()));
            accounts.saveAndFlush(account);
        }
        CardTransaction purchase = history(account, card, command.getAmount(), command.getMerchantName(), requestId);
        purchase.setType(CardTransaction.Type.PURCHASE);
        purchase.setStatus(reason == null ? CardTransaction.Status.APPROVED : CardTransaction.Status.DECLINED);
        purchase.setReasonCode(reason);
        return new TransactionOutcome(transactions.saveAndFlush(purchase), account, false);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionOutcome refund(Long userId, Long purchaseId, String rawRequestId) {
        accountService.requireCustomer(userId);
        String requestId = RequestChecks.requestId(rawRequestId);
        Long accountId = transactions.findOwnedAccountId(purchaseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase is unavailable."));
        CreditAccount account = lockOwnedAccount(userId, accountId);
        CardTransaction purchase = transactions.findOwnedById(purchaseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase is unavailable."));

        CardTransaction saved = transactions.findByAccount_IdAndRequestId(accountId, requestId).orElse(null);
        if (saved != null) {
            if (saved.getType() != CardTransaction.Type.REFUND
                    || !saved.getOriginalPurchase().getId().equals(purchaseId)) {
                throw new RequestConflictException();
            }
            return new TransactionOutcome(saved, account, true);
        }
        if (purchase.getType() != CardTransaction.Type.PURCHASE
                || purchase.getStatus() != CardTransaction.Status.APPROVED) {
            throw new RefundNotEligibleException("Only an approved purchase can be refunded.");
        }
        if (transactions.findByOriginalPurchase_Id(purchaseId).isPresent()) {
            throw new RefundNotEligibleException("This purchase already has a full refund.");
        }
        if (!purchase.getCard().getAccount().getId().equals(accountId)
                || account.getOutstandingBalance().compareTo(purchase.getAmount()) < 0) {
            throw new RefundNotEligibleException("The purchase cannot be refunded against this account balance.");
        }

        // Frozen accounts can receive refunds. Amount and merchant come from the purchase.
        account.setOutstandingBalance(account.getOutstandingBalance().subtract(purchase.getAmount()));
        accounts.saveAndFlush(account);
        CardTransaction refund = history(account, purchase.getCard(), purchase.getAmount(),
                purchase.getMerchantName(), requestId);
        refund.setType(CardTransaction.Type.REFUND);
        refund.setStatus(CardTransaction.Status.APPROVED);
        refund.setOriginalPurchase(purchase);
        return new TransactionOutcome(transactions.saveAndFlush(refund), account, false);
    }

    public Page<CardTransaction> getHistory(Long userId, Long accountId, Integer page, Integer size) {
        accountService.getAccount(userId, accountId);
        return transactions.findByAccount_IdAndAccount_User_IdOrderByIdDesc(
                accountId, userId, RequestChecks.page(page, size));
    }

    public Page<CardTransaction> getAdminTransactions(Long adminId, Integer page, Integer size) {
        accountService.requireAdmin(adminId);
        return transactions.findAllByOrderByIdDesc(RequestChecks.page(page, size));
    }

    private CreditAccount lockOwnedAccount(Long userId, Long accountId) {
        return accounts.findOwnedForUpdate(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account is unavailable."));
    }

    private void validatePurchaseFields(PurchaseCommand command) {
        if (command == null || command.getCardId() == null || command.getCardId() < 1
                || command.getTestCardNumber() == null || !command.getTestCardNumber().matches("[0-9]{16}")
                || command.getTestSecurityCode() == null || !command.getTestSecurityCode().matches("[0-9]{3,4}")
                || command.getExpiryMonth() == null || command.getExpiryMonth() < 1 || command.getExpiryMonth() > 12
                || command.getExpiryYear() == null || command.getExpiryYear() < 2000 || command.getExpiryYear() > 9999) {
            throw new InvalidPurchaseException("Enter valid fictional card fields.");
        }
        if (command.getMerchantName() == null || command.getMerchantName().isBlank()
                || command.getMerchantName().length() > 100) {
            throw new InvalidPurchaseException("Merchant name is required and must be at most 100 characters.");
        }
        BigDecimal amount = command.getAmount();
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2
                || amount.compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw new InvalidPurchaseException("Amount must be positive, fit DECIMAL(14,2), and have at most two decimal places.");
        }
    }

    private void validateAssignedCard(PurchaseCommand command, DemoCard card) {
        // This is the only fictional profile in the schema's seed data.
        if (!"DEMO_4242".equals(card.getTestProfile())
                || !"4242424242424242".equals(command.getTestCardNumber())
                || !"4242".equals(card.getLastFour())
                || command.getExpiryMonth().intValue() != card.getExpiryMonth().intValue()
                || command.getExpiryYear().intValue() != card.getExpiryYear().intValue()) {
            throw new InvalidPurchaseException("Card details must match the assigned fictional test card.");
        }
    }

    private CardTransaction history(CreditAccount account, DemoCard card, BigDecimal amount,
                                    String merchant, String requestId) {
        CardTransaction transaction = new CardTransaction();
        transaction.setAccount(account);
        transaction.setCard(card);
        transaction.setAmount(amount.setScale(2, RoundingMode.UNNECESSARY));
        transaction.setOutstandingAfter(account.getOutstandingBalance());
        transaction.setMerchantName(merchant);
        transaction.setRequestId(requestId);
        transaction.setCreatedAt(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS));
        return transaction;
    }
}
