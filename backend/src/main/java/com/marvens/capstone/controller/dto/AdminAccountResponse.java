package com.marvens.capstone.controller.dto;

import com.marvens.capstone.entity.CreditAccount;

public record AdminAccountResponse(Long id, String creditLimit, String outstandingBalance,
                                   String availableCredit, CreditAccount.Status status,
                                   Long ownerId, String ownerDisplayName, String ownerEmail) {
    public static AdminAccountResponse from(CreditAccount account) {
        AccountResponse summary = AccountResponse.from(account);
        return new AdminAccountResponse(summary.id(), summary.creditLimit(), summary.outstandingBalance(),
                summary.availableCredit(), summary.status(), account.getUser().getId(),
                account.getUser().getDisplayName(), account.getUser().getEmail());
    }
}
