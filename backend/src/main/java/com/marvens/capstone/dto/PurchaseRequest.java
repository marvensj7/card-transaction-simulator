package com.marvens.capstone.dto;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;

// JSON input only. TransactionService checks these fields before saving anything.
public class PurchaseRequest {
    public Long cardId;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String testCardNumber;
    public Integer expiryMonth;
    public Integer expiryYear;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String testSecurityCode;
    public String merchantName;
    public BigDecimal amount;
    public String requestId;

    @Override
    public String toString() {
        return "PurchaseRequest";
    }
}
