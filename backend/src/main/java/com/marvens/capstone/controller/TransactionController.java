package com.marvens.capstone.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import com.marvens.capstone.dto.PurchaseRequest;
import com.marvens.capstone.dto.TransactionResponse;
import com.marvens.capstone.dto.TransactionResultResponse;
import com.marvens.capstone.service.TransactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TransactionController {
    private final TransactionService transactions;

    public TransactionController(TransactionService transactions) {
        this.transactions = transactions;
    }

    @PostMapping("/accounts/{accountId}/purchases")
    public TransactionResultResponse purchase(@AuthenticationPrincipal Jwt principal,
            @PathVariable @Positive Long accountId, @Valid @RequestBody PurchaseRequest request) {
        Long userId = Long.valueOf(principal.getSubject());
        return transactions.purchase(userId, accountId, request);
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public List<TransactionResponse> history(@AuthenticationPrincipal Jwt principal, @PathVariable @Positive Long accountId) {
        Long userId = Long.valueOf(principal.getSubject());
        return transactions.getHistory(userId, accountId);
    }

    @PostMapping("/transactions/{purchaseId}/refund")
    public TransactionResultResponse refund(@AuthenticationPrincipal Jwt principal,
            @PathVariable @Positive Long purchaseId, @RequestParam @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") String requestId) {
        Long userId = Long.valueOf(principal.getSubject());
        return transactions.refund(userId, purchaseId, requestId);
    }
}
