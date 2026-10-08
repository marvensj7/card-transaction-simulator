package com.marvens.capstone.controller;

import jakarta.servlet.http.HttpSession;
import java.util.List;
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
    public List<AccountResponse> getAccounts(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        return accounts.getAccounts(userId);
    }

    @GetMapping("/{accountId}/cards")
    public List<CardResponse> getCards(HttpSession session, @PathVariable Long accountId) {
        Long userId = (Long) session.getAttribute("userId");
        return accounts.getCards(userId, accountId);
    }
}
