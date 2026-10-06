package com.marvens.capstone.controller.dto;

import com.marvens.capstone.entity.CardTransaction;

public record TransactionResponse(Long id, Long accountId, Long cardId, CardTransaction.Type type,
                                  CardTransaction.Status status, String amount, String outstandingAfter,
                                  String merchantName, String reasonCode, String createdAt,
                                  Long originalPurchaseId) {
    public static TransactionResponse from(CardTransaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getAccount().getId(),
                transaction.getCard().getId(), transaction.getType(), transaction.getStatus(),
                ApiFormats.money(transaction.getAmount()), ApiFormats.money(transaction.getOutstandingAfter()),
                transaction.getMerchantName(), transaction.getReasonCode(), ApiFormats.utc(transaction.getCreatedAt()),
                transaction.getOriginalPurchase() == null ? null : transaction.getOriginalPurchase().getId());
    }
}
