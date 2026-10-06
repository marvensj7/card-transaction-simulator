package com.marvens.capstone.controller.dto;

import com.marvens.capstone.entity.CreditAccount;

public record AccountResponse(Long id, String creditLimit, String outstandingBalance,
                              String availableCredit, CreditAccount.Status status) {
    public static AccountResponse from(CreditAccount account) {
        return new AccountResponse(account.getId(), ApiFormats.money(account.getCreditLimit()),
                ApiFormats.money(account.getOutstandingBalance()),
                ApiFormats.money(account.getCreditLimit().subtract(account.getOutstandingBalance())),
                account.getStatus());
    }
}
