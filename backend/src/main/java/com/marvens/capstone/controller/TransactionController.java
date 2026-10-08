package com.marvens.capstone.controller;

import jakarta.servlet.http.HttpSession;
import java.util.List;
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
    public TransactionResultResponse purchase(HttpSession session,
            @PathVariable Long accountId, @RequestBody PurchaseRequest request) {
        Long userId = (Long) session.getAttribute("userId");
        return transactions.purchase(userId, accountId, request);
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public List<TransactionResponse> history(HttpSession session, @PathVariable Long accountId) {
        Long userId = (Long) session.getAttribute("userId");
        return transactions.getHistory(userId, accountId);
    }

    @PostMapping("/transactions/{purchaseId}/refund")
    public TransactionResultResponse refund(HttpSession session,
            @PathVariable Long purchaseId, @RequestParam String requestId) {
        Long userId = (Long) session.getAttribute("userId");
        return transactions.refund(userId, purchaseId, requestId);
    }
}
