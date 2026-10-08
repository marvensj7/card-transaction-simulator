package com.marvens.capstone.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;
import com.marvens.capstone.dto.AccountResponse;
import com.marvens.capstone.dto.TransactionResponse;
import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.service.AccountService;
import com.marvens.capstone.service.TransactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AccountService accounts;
    private final TransactionService transactions;

    public AdminController(AccountService accounts, TransactionService transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    @GetMapping("/accounts")
    public List<AccountResponse> accounts(@AuthenticationPrincipal Jwt principal) {
        Long adminId = Long.valueOf(principal.getSubject());
        return accounts.getAdminAccounts(adminId);
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> transactions(@AuthenticationPrincipal Jwt principal) {
        Long adminId = Long.valueOf(principal.getSubject());
        return transactions.getAdminTransactions(adminId);
    }

    @PatchMapping("/accounts/{accountId}/status")
    public AccountResponse changeStatus(@AuthenticationPrincipal Jwt principal,
            @PathVariable Long accountId, @RequestParam CreditAccount.Status status) {
        Long adminId = Long.valueOf(principal.getSubject());
        return accounts.changeStatus(adminId, accountId, status);
    }
}
