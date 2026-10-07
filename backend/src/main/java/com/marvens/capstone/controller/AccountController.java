package com.marvens.capstone.controller;

import java.security.Principal;
import java.util.List;
import com.marvens.capstone.dto.AccountResponse;
import com.marvens.capstone.dto.CardResponse;
import com.marvens.capstone.security.AuthenticatedUser;
import com.marvens.capstone.service.AccountService;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) { this.accounts = accounts; }

    @GetMapping
    public List<AccountResponse> getAccounts(Principal principal) {
        return accounts.getAccounts(AuthenticatedUser.id(principal)).stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{accountId}/cards")
    public List<CardResponse> getCards(Principal principal,
            @PathVariable @Positive Long accountId) {
        return accounts.getCards(AuthenticatedUser.id(principal), accountId).stream().map(CardResponse::from).toList();
    }
}
