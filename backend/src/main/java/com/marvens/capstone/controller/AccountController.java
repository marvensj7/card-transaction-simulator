package com.marvens.capstone.controller;

import java.security.Principal;
import java.util.List;
import com.marvens.capstone.controller.dto.AccountResponse;
import com.marvens.capstone.controller.dto.CardResponse;
import com.marvens.capstone.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) { this.accounts = accounts; }

    @GetMapping
    @Operation(summary = "List the signed-in customer's credit account")
    @ApiResponse(responseCode = "200", description = "Customer account summaries")
    public List<AccountResponse> getAccounts(@Parameter(hidden = true) Principal principal) {
        return accounts.getAccounts(CurrentUser.id(principal)).stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{accountId}/cards")
    @Operation(summary = "List an owned account's masked demo card")
    @ApiResponse(responseCode = "200", description = "Masked demo cards")
    public List<CardResponse> getCards(@Parameter(hidden = true) Principal principal,
                                       @PathVariable @Positive Long accountId) {
        return accounts.getCards(CurrentUser.id(principal), accountId).stream().map(CardResponse::from).toList();
    }
}
