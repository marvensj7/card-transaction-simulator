package com.marvens.capstone.service;

import com.marvens.capstone.dto.PageResponse;
import com.marvens.capstone.dto.PurchaseRequest;
import com.marvens.capstone.dto.TransactionResponse;
import com.marvens.capstone.dto.TransactionResultResponse;
import com.marvens.capstone.entity.AppUser;
import com.marvens.capstone.entity.CardTransaction;
import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.entity.DemoCard;
import com.marvens.capstone.exception.ConflictException;
import com.marvens.capstone.exception.ResourceNotFoundException;
import com.marvens.capstone.repository.CardTransactionRepository;
import com.marvens.capstone.repository.CreditAccountRepository;
import com.marvens.capstone.repository.DemoCardRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class TransactionService {
    private final AccountService accountService;
    private final CreditAccountRepository accountRepository;
    private final DemoCardRepository cardRepository;
    private final CardTransactionRepository transactionRepository;

    public TransactionService(AccountService accountService, CreditAccountRepository accountRepository,
            DemoCardRepository cardRepository, CardTransactionRepository transactionRepository) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
    }

    // Commit balance + history together. After waiting for a lock, read the committed result.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResultResponse purchase(Long userId, Long accountId, PurchaseRequest request) {
        accountService.requireRole(userId, AppUser.Role.USER);
        String requestId = checkRequestId(request.requestId);
        CreditAccount customerAccount = lockOwnedAccount(userId, accountId);
        DemoCard assignedCard = cardRepository.findByIdAndAccount_Id(request.cardId, accountId);
        if (assignedCard == null) {
            throw new ResourceNotFoundException("Card is unavailable.");
        }
        validateAssignedCard(request, assignedCard);

        // Reusing the same request ID must not add another purchase.
        CardTransaction existingTransaction = transactionRepository.findByAccount_IdAndRequestId(accountId, requestId);
        if (existingTransaction != null) {
            if (existingTransaction.getType() != CardTransaction.Type.PURCHASE
                    || !existingTransaction.getCard().getId().equals(request.cardId)
                    || !existingTransaction.getMerchantName().equals(request.merchantName)
                    || existingTransaction.getAmount().compareTo(request.amount) != 0) {
                throw new ConflictException("Request ID is already used for different details.");
            }
            return new TransactionResultResponse(existingTransaction, customerAccount, true);
        }

        String declineReason = null;
        YearMonth cardExpiry = YearMonth.of(assignedCard.getExpiryYear(), assignedCard.getExpiryMonth());
        BigDecimal availableCredit = customerAccount.getCreditLimit().subtract(customerAccount.getOutstandingBalance());
        if (cardExpiry.isBefore(YearMonth.now(ZoneOffset.UTC))) {
            declineReason = "CARD_EXPIRED";
        } else if (customerAccount.getStatus() == CreditAccount.Status.FROZEN) {
            declineReason = "ACCOUNT_FROZEN";
        } else if (request.amount.compareTo(availableCredit) > 0) {
            declineReason = "INSUFFICIENT_CREDIT";
        }

        CardTransaction purchase = new CardTransaction();
        purchase.setType(CardTransaction.Type.PURCHASE);
        if (declineReason == null) {
            customerAccount.setOutstandingBalance(customerAccount.getOutstandingBalance().add(request.amount));
            accountRepository.save(customerAccount);
            purchase.setStatus(CardTransaction.Status.APPROVED);
        } else {
            purchase.setStatus(CardTransaction.Status.DECLINED);
        }
        purchase.setReasonCode(declineReason);
        fillTransactionHistory(purchase, customerAccount, assignedCard, request.amount, request.merchantName, requestId);
        transactionRepository.save(purchase);
        return new TransactionResultResponse(purchase, customerAccount);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResultResponse refund(Long userId, Long purchaseId, String rawRequestId) {
        accountService.requireRole(userId, AppUser.Role.USER);
        String requestId = checkRequestId(rawRequestId);
        Long accountId = transactionRepository.findOwnedAccountId(purchaseId, userId);
        if (accountId == null) {
            throw new ResourceNotFoundException("Purchase is unavailable.");
        }
        CreditAccount customerAccount = lockOwnedAccount(userId, accountId);
        CardTransaction purchase = transactionRepository.findByIdAndAccount_User_Id(purchaseId, userId);
        if (purchase == null) {
            throw new ResourceNotFoundException("Purchase is unavailable.");
        }

        CardTransaction existingTransaction = transactionRepository.findByAccount_IdAndRequestId(accountId, requestId);
        if (existingTransaction != null) {
            if (existingTransaction.getType() != CardTransaction.Type.REFUND
                    || !existingTransaction.getOriginalPurchase().getId().equals(purchaseId)) {
                throw new ConflictException("Request ID is already used for different details.");
            }
            return new TransactionResultResponse(existingTransaction, customerAccount, true);
        }
        if (purchase.getType() != CardTransaction.Type.PURCHASE
                || purchase.getStatus() != CardTransaction.Status.APPROVED
                || transactionRepository.findByOriginalPurchase_Id(purchaseId) != null) {
            throw new ConflictException("Only an approved purchase without a refund can be refunded.");
        }
        if (!purchase.getCard().getAccount().getId().equals(accountId)
                || customerAccount.getOutstandingBalance().compareTo(purchase.getAmount()) < 0) {
            throw new ConflictException("This purchase cannot be refunded against this account.");
        }

        // A full refund copies the purchase amount, including when the account is frozen.
        customerAccount.setOutstandingBalance(customerAccount.getOutstandingBalance().subtract(purchase.getAmount()));
        accountRepository.save(customerAccount);
        CardTransaction refund = new CardTransaction();
        refund.setType(CardTransaction.Type.REFUND);
        refund.setStatus(CardTransaction.Status.APPROVED);
        refund.setOriginalPurchase(purchase);
        fillTransactionHistory(refund, customerAccount, purchase.getCard(), purchase.getAmount(), purchase.getMerchantName(), requestId);
        transactionRepository.save(refund);
        return new TransactionResultResponse(refund, customerAccount);
    }

    public PageResponse<TransactionResponse> getHistory(Long userId, Long accountId, int page, int size) {
        accountService.getOwnedAccount(userId, accountId);
        return historyResponse(transactionRepository.findByAccount_IdOrderByIdDesc(accountId, PageRequest.of(page, size)));
    }

    public PageResponse<TransactionResponse> getAdminTransactions(Long adminId, int page, int size) {
        accountService.requireRole(adminId, AppUser.Role.ADMIN);
        return historyResponse(transactionRepository.findAllByOrderByIdDesc(PageRequest.of(page, size)));
    }

    private PageResponse<TransactionResponse> historyResponse(Page<CardTransaction> transactionRecords) {
        List<Long> transactionIds = new ArrayList<>();
        for (CardTransaction transactionRecord : transactionRecords) {
            transactionIds.add(transactionRecord.getId());
        }
        List<Long> refundedPurchaseIds = new ArrayList<>();
        if (!transactionIds.isEmpty()) {
            refundedPurchaseIds = transactionRepository.findRefundedPurchaseIds(transactionIds);
        }
        List<TransactionResponse> transactionResponses = new ArrayList<>();
        for (CardTransaction transactionRecord : transactionRecords) {
            transactionResponses.add(new TransactionResponse(transactionRecord, refundedPurchaseIds.contains(transactionRecord.getId())));
        }
        return new PageResponse<>(transactionResponses, transactionRecords);
    }

    private CreditAccount lockOwnedAccount(Long userId, Long accountId) {
        CreditAccount customerAccount = accountRepository.findLockedByIdAndUser_Id(accountId, userId);
        if (customerAccount == null) {
            throw new ResourceNotFoundException("Account is unavailable.");
        }
        return customerAccount;
    }

    private void validateAssignedCard(PurchaseRequest request, DemoCard assignedCard) {
        if (!FictionalCardNumbers.matchesAssignedNumber(assignedCard, request.testCardNumber)
                || request.expiryMonth.intValue() != assignedCard.getExpiryMonth().intValue()
                || request.expiryYear.intValue() != assignedCard.getExpiryYear().intValue()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use the assigned fictional card and expiry.");
        }
    }

    private String checkRequestId(String rawRequestId) {
        if (rawRequestId != null) {
            try {
                String normalizedRequestId = UUID.fromString(rawRequestId).toString();
                if (normalizedRequestId.equalsIgnoreCase(rawRequestId)) {
                    return normalizedRequestId;
                }
            } catch (IllegalArgumentException ignored) {
                // The fixed error below explains the rule without printing the input.
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request ID must be a UUID.");
    }

    private void fillTransactionHistory(CardTransaction transaction, CreditAccount customerAccount, DemoCard assignedCard,
            BigDecimal amount, String merchantName, String requestId) {
        transaction.setAccount(customerAccount);
        transaction.setCard(assignedCard);
        transaction.setAmount(amount.setScale(2));
        transaction.setOutstandingAfter(customerAccount.getOutstandingBalance());
        transaction.setMerchantName(merchantName);
        transaction.setRequestId(requestId);
        transaction.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).withNano(0));
    }
}
