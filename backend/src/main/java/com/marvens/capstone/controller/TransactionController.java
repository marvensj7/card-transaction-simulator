package com.marvens.capstone.controller;

import java.security.Principal;
import jakarta.validation.constraints.Pattern;

import com.marvens.capstone.dto.*;
import com.marvens.capstone.security.AuthenticatedUser;
import com.marvens.capstone.service.TransactionOutcome;
import com.marvens.capstone.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TransactionController {
    private final TransactionService transactions;

    public TransactionController(TransactionService transactions) { this.transactions = transactions; }

    @PostMapping("/accounts/{accountId}/purchases")
    public ResponseEntity<TransactionResultResponse> purchase(Principal principal,
            @PathVariable @Positive Long accountId, @Valid @RequestBody PurchaseRequest request) {
        return result(transactions.purchase(AuthenticatedUser.id(principal), accountId, request));
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public PageResponse<TransactionResponse> history(Principal principal,
            @PathVariable @Positive Long accountId,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) Integer size) {
        return PageResponse.from(transactions.getHistory(AuthenticatedUser.id(principal), accountId, page, Math.min(size, 50)),
                TransactionResponse::from);
    }

    @PostMapping("/transactions/{purchaseId}/refund")
    public ResponseEntity<TransactionResultResponse> refund(Principal principal,
            @PathVariable @Positive Long purchaseId,
            @RequestParam @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
            String requestId) {
        return result(transactions.refund(AuthenticatedUser.id(principal), purchaseId, requestId));
    }

    private ResponseEntity<TransactionResultResponse> result(TransactionOutcome outcome) {
        return ResponseEntity.status(outcome.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(TransactionResultResponse.from(outcome));
    }
}
