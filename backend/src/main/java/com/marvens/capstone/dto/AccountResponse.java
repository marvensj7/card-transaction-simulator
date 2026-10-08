package com.marvens.capstone.dto;

import java.math.BigDecimal;
import com.marvens.capstone.entity.CreditAccount;

// Both the customer and admin screens use this same safe summary.
public class AccountResponse {
    public final Long id;
    public final String ownerName;
    public final BigDecimal creditLimit;
    public final BigDecimal outstandingBalance;
    public final BigDecimal availableCredit;
    public final CreditAccount.Status status;

    public AccountResponse(CreditAccount account) {
        id = account.getId();
        ownerName = account.getUser().getDisplayName();
        creditLimit = account.getCreditLimit();
        outstandingBalance = account.getOutstandingBalance();
        availableCredit = creditLimit.subtract(outstandingBalance);
        status = account.getStatus();
    }
}
