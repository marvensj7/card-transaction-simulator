package com.marvens.capstone.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import com.marvens.capstone.entity.CardTransaction;

public class TransactionResponse {
    public final Long id;
    public final Long accountId;
    public final Long cardId;
    public final CardTransaction.Type type;
    public final CardTransaction.Status status;
    public final BigDecimal amount;
    public final BigDecimal outstandingAfter;
    public final String merchantName;
    public final String reasonCode;
    public final Instant createdAt;
    public final Long originalPurchaseId;

    public TransactionResponse(CardTransaction transaction) {
        id = transaction.getId();
        accountId = transaction.getAccount().getId();
        cardId = transaction.getCard().getId();
        type = transaction.getType();
        status = transaction.getStatus();
        amount = transaction.getAmount();
        outstandingAfter = transaction.getOutstandingAfter();
        merchantName = transaction.getMerchantName();
        reasonCode = transaction.getReasonCode();
        createdAt = transaction.getCreatedAt().toInstant(ZoneOffset.UTC);
        if (transaction.getOriginalPurchase() == null) {
            originalPurchaseId = null;
        } else {
            originalPurchaseId = transaction.getOriginalPurchase().getId();
        }
    }
}
