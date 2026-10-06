package com.marvens.capstone.controller;

import java.security.Principal;
import com.marvens.capstone.controller.dto.*;
import com.marvens.capstone.service.TransactionOutcome;
import com.marvens.capstone.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    @Operation(summary = "Submit a fictional purchase for an owned account")
    @ApiResponse(responseCode = "201", description = "New approved or declined purchase")
    @ApiResponse(responseCode = "200", description = "Saved result of an identical retry")
    public ResponseEntity<TransactionResultResponse> purchase(@Parameter(hidden = true) Principal principal,
            @PathVariable @Positive Long accountId, @Valid @RequestBody PurchaseRequest request) {
        return result(transactions.purchase(CurrentUser.id(principal), accountId, request.toCommand()));
    }

    @GetMapping("/accounts/{accountId}/transactions")
    @Operation(summary = "Read an owned account's transaction history, newest first")
    @ApiResponse(responseCode = "200", description = "Transaction page with total counts")
    public PageResponse<TransactionResponse> history(@Parameter(hidden = true) Principal principal,
            @PathVariable @Positive Long accountId,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) Integer size) {
        return PageResponse.from(transactions.getHistory(CurrentUser.id(principal), accountId, page, Math.min(size, 50)),
                TransactionResponse::from);
    }

    @PostMapping("/transactions/{purchaseId}/refund")
    @Operation(summary = "Fully refund an owned approved purchase")
    @ApiResponse(responseCode = "201", description = "New full refund")
    @ApiResponse(responseCode = "200", description = "Saved result of an identical retry")
    public ResponseEntity<TransactionResultResponse> refund(@Parameter(hidden = true) Principal principal,
            @PathVariable @Positive Long purchaseId, @Valid @RequestBody RefundRequest request) {
        return result(transactions.refund(CurrentUser.id(principal), purchaseId, request.requestId()));
    }

    private ResponseEntity<TransactionResultResponse> result(TransactionOutcome outcome) {
        return ResponseEntity.status(outcome.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(TransactionResultResponse.from(outcome));
    }
}
