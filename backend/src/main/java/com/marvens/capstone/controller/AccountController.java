package com.marvens.capstone.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;
import jakarta.validation.constraints.Positive;
import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.entity.DemoCard;
import com.marvens.capstone.service.AccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @Operation(summary = "List the current customer account")
    @GetMapping
    public List<CreditAccount> getAccounts(@AuthenticationPrincipal Jwt principal) {
        Long userId = Long.valueOf(principal.getSubject());
        return accounts.getAccounts(userId);
    }

    @Operation(summary = "Read masked cards for an owned account")
    @GetMapping("/{accountId}/cards")
    public List<DemoCard> getCards(@AuthenticationPrincipal Jwt principal, @PathVariable @Positive Long accountId) {
        Long userId = Long.valueOf(principal.getSubject());
        return accounts.getCards(userId, accountId);
    }
}
