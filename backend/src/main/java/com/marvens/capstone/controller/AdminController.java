package com.marvens.capstone.controller;

import java.security.Principal;
import com.marvens.capstone.controller.dto.*;
import com.marvens.capstone.service.AccountService;
import com.marvens.capstone.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
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
    @Operation(summary = "Review credit accounts and their owners, by ascending account ID")
    @ApiResponse(responseCode = "200", description = "Admin account page")
    public PageResponse<AdminAccountResponse> accounts(@Parameter(hidden = true) Principal principal,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) Integer size) {
        return PageResponse.from(accounts.getAdminAccounts(CurrentUser.id(principal), page, Math.min(size, 50)),
                AdminAccountResponse::from);
    }

    @GetMapping("/transactions")
    @Operation(summary = "Review transaction activity and owner emails, newest first")
    @ApiResponse(responseCode = "200", description = "Admin transaction page")
    public PageResponse<AdminTransactionResponse> transactions(@Parameter(hidden = true) Principal principal,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) Integer size) {
        return PageResponse.from(transactions.getAdminTransactions(CurrentUser.id(principal), page, Math.min(size, 50)),
                AdminTransactionResponse::from);
    }

    @PatchMapping("/accounts/{accountId}/status")
    @Operation(summary = "Freeze or reactivate a credit account")
    @ApiResponse(responseCode = "200", description = "Updated admin account summary")
    public AdminAccountResponse changeStatus(@Parameter(hidden = true) Principal principal,
            @PathVariable @Positive Long accountId, @Valid @RequestBody AccountStatusRequest request) {
        return AdminAccountResponse.from(accounts.changeStatus(CurrentUser.id(principal), accountId, request.status()));
    }
}
