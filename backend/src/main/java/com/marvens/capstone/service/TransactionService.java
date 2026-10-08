package com.marvens.capstone.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.marvens.capstone.dto.PageResponse;
import java.util.UUID;
import com.marvens.capstone.dto.PurchaseRequest;
import com.marvens.capstone.dto.TransactionResponse;
import com.marvens.capstone.dto.TransactionResultResponse;
import com.marvens.capstone.entity.AppUser;
import com.marvens.capstone.entity.CardTransaction;
import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.entity.DemoCard;
import com.marvens.capstone.repository.CardTransactionRepository;
import com.marvens.capstone.repository.CreditAccountRepository;
import com.marvens.capstone.repository.DemoCardRepository;
import com.marvens.capstone.exception.ConflictException;
import com.marvens.capstone.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class TransactionService {
    private final AccountService accountService;
    private final CreditAccountRepository accounts;
    private final DemoCardRepository cards;
    private final CardTransactionRepository transactions;

    public TransactionService(AccountService accountService, CreditAccountRepository accounts,
            DemoCardRepository cards, CardTransactionRepository transactions) {
        this.accountService = accountService;
        this.accounts = accounts;
        this.cards = cards;
        this.transactions = transactions;
    }

    // Commit balance + history together. After waiting for a lock, read the committed result.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResultResponse purchase(Long userId, Long accountId, PurchaseRequest request) {
        accountService.requireRole(userId, AppUser.Role.USER);
        String requestId = checkRequestId(request.requestId);
        CreditAccount account = lockOwnedAccount(userId, accountId);
        DemoCard card = cards.findByIdAndAccount_Id(request.cardId, accountId);
        if (card == null) {
            throw new ResourceNotFoundException("Card is unavailable.");
        }
        validateAssignedCard(request, card);

        // Reusing the same request ID must not add another purchase.
        CardTransaction saved = transactions.findByAccount_IdAndRequestId(accountId, requestId);
        if (saved != null) {
            if (saved.getType() != CardTransaction.Type.PURCHASE
                    || !saved.getCard().getId().equals(request.cardId)
                    || !saved.getMerchantName().equals(request.merchantName)
                    || saved.getAmount().compareTo(request.amount) != 0) {
                throw new ConflictException("Request ID is already used for different details.");
            }
            return new TransactionResultResponse(saved, account);
        }

        String reason = null;
        YearMonth expiry = YearMonth.of(card.getExpiryYear(), card.getExpiryMonth());
        BigDecimal availableCredit = account.getCreditLimit().subtract(account.getOutstandingBalance());
        if (expiry.isBefore(YearMonth.now(ZoneOffset.UTC))) {
            reason = "CARD_EXPIRED";
        } else if (account.getStatus() == CreditAccount.Status.FROZEN) {
            reason = "ACCOUNT_FROZEN";
        } else if (request.amount.compareTo(availableCredit) > 0) {
            reason = "INSUFFICIENT_CREDIT";
        }

        CardTransaction purchase = new CardTransaction();
        purchase.setType(CardTransaction.Type.PURCHASE);
        if (reason == null) {
            account.setOutstandingBalance(account.getOutstandingBalance().add(request.amount));
            accounts.save(account);
            purchase.setStatus(CardTransaction.Status.APPROVED);
        } else {
            purchase.setStatus(CardTransaction.Status.DECLINED);
        }
        purchase.setReasonCode(reason);
        fillHistory(purchase, account, card, request.amount, request.merchantName, requestId);
        transactions.save(purchase);
        return new TransactionResultResponse(purchase, account);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResultResponse refund(Long userId, Long purchaseId, String rawRequestId) {
        accountService.requireRole(userId, AppUser.Role.USER);
        String requestId = checkRequestId(rawRequestId);
        Long accountId = transactions.findOwnedAccountId(purchaseId, userId);
        if (accountId == null) {
            throw new ResourceNotFoundException("Purchase is unavailable.");
        }
        CreditAccount account = lockOwnedAccount(userId, accountId);
        CardTransaction purchase = transactions.findByIdAndAccount_User_Id(purchaseId, userId);
        if (purchase == null) {
            throw new ResourceNotFoundException("Purchase is unavailable.");
        }

        CardTransaction saved = transactions.findByAccount_IdAndRequestId(accountId, requestId);
        if (saved != null) {
            if (saved.getType() != CardTransaction.Type.REFUND
                    || !saved.getOriginalPurchase().getId().equals(purchaseId)) {
                throw new ConflictException("Request ID is already used for different details.");
            }
            return new TransactionResultResponse(saved, account);
        }
        if (purchase.getType() != CardTransaction.Type.PURCHASE
                || purchase.getStatus() != CardTransaction.Status.APPROVED
                || transactions.findByOriginalPurchase_Id(purchaseId) != null) {
            throw new ConflictException("Only an approved purchase without a refund can be refunded.");
        }
        if (!purchase.getCard().getAccount().getId().equals(accountId)
                || account.getOutstandingBalance().compareTo(purchase.getAmount()) < 0) {
            throw new ConflictException("This purchase cannot be refunded against this account.");
        }

        // A full refund copies the purchase amount, including when the account is frozen.
        account.setOutstandingBalance(account.getOutstandingBalance().subtract(purchase.getAmount()));
        accounts.save(account);
        CardTransaction refund = new CardTransaction();
        refund.setType(CardTransaction.Type.REFUND);
        refund.setStatus(CardTransaction.Status.APPROVED);
        refund.setOriginalPurchase(purchase);
        fillHistory(refund, account, purchase.getCard(), purchase.getAmount(), purchase.getMerchantName(), requestId);
        transactions.save(refund);
        return new TransactionResultResponse(refund, account);
    }

    public PageResponse<TransactionResponse> getHistory(Long userId, Long accountId, int page, int size) {
        accountService.getOwnedAccount(userId, accountId);
        List<TransactionResponse> result = new ArrayList<>();
        Page<CardTransaction> rows = transactions.findByAccount_IdOrderByIdDesc(accountId, PageRequest.of(page, size));
        for (CardTransaction transaction : rows) {
            result.add(new TransactionResponse(transaction));
        }
        return new PageResponse<>(result, rows);
    }

    public PageResponse<TransactionResponse> getAdminTransactions(Long adminId, int page, int size) {
        accountService.requireRole(adminId, AppUser.Role.ADMIN);
        List<TransactionResponse> result = new ArrayList<>();
        Page<CardTransaction> rows = transactions.findAllByOrderByIdDesc(PageRequest.of(page, size));
        for (CardTransaction transaction : rows) {
            result.add(new TransactionResponse(transaction));
        }
        return new PageResponse<>(result, rows);
    }

    private CreditAccount lockOwnedAccount(Long userId, Long accountId) {
        CreditAccount account = accounts.findLockedByIdAndUser_Id(accountId, userId);
        if (account == null) {
            throw new ResourceNotFoundException("Account is unavailable.");
        }
        return account;
    }

    private void validateAssignedCard(PurchaseRequest request, DemoCard card) {
        if (!"DEMO_4242".equals(card.getTestProfile()) || !"4242".equals(card.getLastFour())
                || !"4242424242424242".equals(request.testCardNumber)
                || request.expiryMonth.intValue() != card.getExpiryMonth().intValue()
                || request.expiryYear.intValue() != card.getExpiryYear().intValue()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use the assigned fictional card and expiry.");
        }
    }

    private String checkRequestId(String value) {
        if (value != null) {
            try {
                String normalized = UUID.fromString(value).toString();
                if (normalized.equalsIgnoreCase(value)) {
                    return normalized;
                }
            } catch (IllegalArgumentException ignored) {
                // The fixed error below explains the rule without printing the input.
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request ID must be a UUID.");
    }

    private void fillHistory(CardTransaction transaction, CreditAccount account, DemoCard card,
            BigDecimal amount, String merchant, String requestId) {
        transaction.setAccount(account);
        transaction.setCard(card);
        transaction.setAmount(amount.setScale(2));
        transaction.setOutstandingAfter(account.getOutstandingBalance());
        transaction.setMerchantName(merchant);
        transaction.setRequestId(requestId);
        transaction.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).withNano(0));
    }
}
