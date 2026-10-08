package com.marvens.capstone.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.marvens.capstone.dto.PageResponse;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api")
public class TransactionController {
    private final TransactionService transactions;

    public TransactionController(TransactionService transactions) {
        this.transactions = transactions;
    }

    @Operation(summary = "Save an approved or declined fictional purchase")
    @ApiResponse(responseCode = "201", description = "New saved outcome")
    @ApiResponse(responseCode = "200", description = "Identical retry; no new write")
    @PostMapping("/accounts/{accountId}/purchases")
    public ResponseEntity<TransactionResultResponse> purchase(@AuthenticationPrincipal Jwt principal,
            @PathVariable @Positive Long accountId, @Valid @RequestBody PurchaseRequest request) {
        Long userId = Long.valueOf(principal.getSubject());
        TransactionResultResponse result = transactions.purchase(userId, accountId, request);
        return ResponseEntity.status(result.replayed ? 200 : 201).body(result);
    }

    @Operation(summary = "Page owned history newest first")
    @GetMapping("/accounts/{accountId}/transactions")
    public PageResponse<TransactionResponse> history(@AuthenticationPrincipal Jwt principal,
            @PathVariable @Positive Long accountId, @RequestParam(defaultValue = "0") @Min(0) @Max(10000) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        Long userId = Long.valueOf(principal.getSubject());
        return transactions.getHistory(userId, accountId, page, size);
    }

    @Operation(summary = "Refund an owned approved purchase in full")
    @ApiResponse(responseCode = "201", description = "New saved outcome")
    @ApiResponse(responseCode = "200", description = "Identical retry; no new write")
    @PostMapping("/transactions/{purchaseId}/refund")
    public ResponseEntity<TransactionResultResponse> refund(@AuthenticationPrincipal Jwt principal,
            @PathVariable @Positive Long purchaseId, @RequestParam @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") String requestId) {
        Long userId = Long.valueOf(principal.getSubject());
        TransactionResultResponse result = transactions.refund(userId, purchaseId, requestId);
        return ResponseEntity.status(result.replayed ? 200 : 201).body(result);
    }
}
