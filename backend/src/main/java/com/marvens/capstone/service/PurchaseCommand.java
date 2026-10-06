package com.marvens.capstone.service;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;

// In-memory input only. Plain class avoids a generated toString containing card data.
public class PurchaseCommand {
    private final Long cardId;
    private final String testCardNumber;
    private final Integer expiryMonth;
    private final Integer expiryYear;
    private final String testSecurityCode;
    private final String merchantName;
    private final BigDecimal amount;
    private final String requestId;

    public PurchaseCommand(Long cardId, String testCardNumber, Integer expiryMonth, Integer expiryYear,
                           String testSecurityCode, String merchantName, BigDecimal amount, String requestId) {
        this.cardId = cardId;
        this.testCardNumber = testCardNumber;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.testSecurityCode = testSecurityCode;
        this.merchantName = merchantName;
        this.amount = amount;
        this.requestId = requestId;
    }

    public Long getCardId() { return cardId; }
    @JsonIgnore
    public String getTestCardNumber() { return testCardNumber; }
    public Integer getExpiryMonth() { return expiryMonth; }
    public Integer getExpiryYear() { return expiryYear; }
    @JsonIgnore
    public String getTestSecurityCode() { return testSecurityCode; }
    public String getMerchantName() { return merchantName; }
    public BigDecimal getAmount() { return amount; }
    public String getRequestId() { return requestId; }

    @Override
    public String toString() { return "PurchaseCommand"; }
}
