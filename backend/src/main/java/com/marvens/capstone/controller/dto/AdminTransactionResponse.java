package com.marvens.capstone.controller.dto;

import com.marvens.capstone.entity.CardTransaction;

public record AdminTransactionResponse(Long id, Long accountId, Long cardId, CardTransaction.Type type,
                                       CardTransaction.Status status, String amount, String outstandingAfter,
                                       String merchantName, String reasonCode, String createdAt,
                                       Long originalPurchaseId, String ownerEmail) {
    public static AdminTransactionResponse from(CardTransaction transaction) {
        TransactionResponse summary = TransactionResponse.from(transaction);
        return new AdminTransactionResponse(summary.id(), summary.accountId(), summary.cardId(), summary.type(),
                summary.status(), summary.amount(), summary.outstandingAfter(), summary.merchantName(),
                summary.reasonCode(), summary.createdAt(), summary.originalPurchaseId(),
                transaction.getAccount().getUser().getEmail());
    }
}
