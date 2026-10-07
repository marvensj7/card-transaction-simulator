package com.marvens.capstone.controller;

import java.security.Principal;

import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.dto.AdminAccountResponse;
import com.marvens.capstone.dto.AdminTransactionResponse;
import com.marvens.capstone.dto.PageResponse;
import com.marvens.capstone.security.AuthenticatedUser;
import com.marvens.capstone.service.AccountService;
import com.marvens.capstone.service.TransactionService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

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
    public PageResponse<AdminAccountResponse> accounts(Principal principal,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) Integer size) {
        return PageResponse.from(accounts.getAdminAccounts(AuthenticatedUser.id(principal), page, Math.min(size, 50)),
                AdminAccountResponse::from);
    }

    @GetMapping("/transactions")
    public PageResponse<AdminTransactionResponse> transactions(Principal principal,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) Integer size) {
        return PageResponse.from(transactions.getAdminTransactions(AuthenticatedUser.id(principal), page, Math.min(size, 50)),
                AdminTransactionResponse::from);
    }

    @PatchMapping("/accounts/{accountId}/status")
    public AdminAccountResponse changeStatus(Principal principal,
            @PathVariable @Positive Long accountId, @RequestParam CreditAccount.Status status) {
        return AdminAccountResponse.from(accounts.changeStatus(AuthenticatedUser.id(principal), accountId, status));
    }
}
