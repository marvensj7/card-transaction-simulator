package com.marvens.capstone.service;

import com.marvens.capstone.entity.CardTransaction;
import com.marvens.capstone.entity.CreditAccount;

// Internal service result. Controllers map this to API response DTOs.
public record TransactionOutcome(CardTransaction transaction, CreditAccount account, boolean replayed) {
}
