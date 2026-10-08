package com.marvens.capstone.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;
import jakarta.validation.constraints.Positive;
import com.marvens.capstone.dto.AccountResponse;
import com.marvens.capstone.dto.CardResponse;
import com.marvens.capstone.service.AccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public List<AccountResponse> getAccounts(@AuthenticationPrincipal Jwt principal) {
        Long userId = Long.valueOf(principal.getSubject());
        return accounts.getAccounts(userId);
    }

    @GetMapping("/{accountId}/cards")
    public List<CardResponse> getCards(@AuthenticationPrincipal Jwt principal, @PathVariable @Positive Long accountId) {
        Long userId = Long.valueOf(principal.getSubject());
        return accounts.getCards(userId, accountId);
    }
}
