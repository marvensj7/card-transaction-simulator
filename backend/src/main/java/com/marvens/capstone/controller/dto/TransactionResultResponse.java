package com.marvens.capstone.controller.dto;

import com.marvens.capstone.service.TransactionOutcome;

public record TransactionResultResponse(TransactionResponse transaction, AccountResponse account) {
    public static TransactionResultResponse from(TransactionOutcome outcome) {
        return new TransactionResultResponse(TransactionResponse.from(outcome.transaction()),
                AccountResponse.from(outcome.account()));
    }
}
