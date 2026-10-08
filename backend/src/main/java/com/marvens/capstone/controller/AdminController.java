package com.marvens.capstone.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.marvens.capstone.dto.PageResponse;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
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

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AccountService accounts;
    private final TransactionService transactions;

    public AdminController(AccountService accounts, TransactionService transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    @Operation(summary = "Page accounts by ascending ID")
    @GetMapping("/accounts")
    public PageResponse<AccountResponse> accounts(@AuthenticationPrincipal Jwt principal, @RequestParam(defaultValue = "0") @Min(0) @Max(10000) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        Long adminId = Long.valueOf(principal.getSubject());
        return accounts.getAdminAccounts(adminId, page, size);
    }

    @Operation(summary = "Page all activity newest first")
    @GetMapping("/transactions")
    public PageResponse<TransactionResponse> transactions(@AuthenticationPrincipal Jwt principal, @RequestParam(defaultValue = "0") @Min(0) @Max(10000) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        Long adminId = Long.valueOf(principal.getSubject());
        return transactions.getAdminTransactions(adminId, page, size);
    }

    @Operation(summary = "Freeze or reactivate an account")
    @PatchMapping("/accounts/{accountId}/status")
    public AccountResponse changeStatus(@AuthenticationPrincipal Jwt principal,
            @PathVariable @Positive Long accountId, @RequestParam CreditAccount.Status status) {
        Long adminId = Long.valueOf(principal.getSubject());
        return accounts.changeStatus(adminId, accountId, status);
    }
}
