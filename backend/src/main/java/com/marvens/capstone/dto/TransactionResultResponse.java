package com.marvens.capstone.dto;

import com.marvens.capstone.entity.CardTransaction;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.marvens.capstone.entity.CreditAccount;

// One purchase/refund result plus the account's current balance.
public class TransactionResultResponse {
    public final TransactionResponse transaction;
    public final CreditAccount account;
    @JsonIgnore
    public final boolean replayed;

    public TransactionResultResponse(CardTransaction savedTransaction, CreditAccount currentAccount) {
        this(savedTransaction, currentAccount, false);
    }

    public TransactionResultResponse(CardTransaction savedTransaction, CreditAccount currentAccount, boolean replayed) {
        this.replayed = replayed;
        transaction = new TransactionResponse(savedTransaction);
        account = currentAccount;
    }
}
