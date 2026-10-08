package com.marvens.capstone.dto;

import com.marvens.capstone.entity.CardTransaction;
import com.marvens.capstone.entity.CreditAccount;

// One purchase/refund result plus the account's current balance.
public class TransactionResultResponse {
    public final TransactionResponse transaction;
    public final AccountResponse account;

    public TransactionResultResponse(CardTransaction savedTransaction, CreditAccount currentAccount) {
        transaction = new TransactionResponse(savedTransaction);
        account = new AccountResponse(currentAccount);
    }
}
