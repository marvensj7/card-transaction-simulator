package com.marvens.capstone.dto;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

/** In-memory fictional input. Sensitive fields are write-only and never printed. */
public class PurchaseRequest {
    @NotNull @Positive
    private final Long cardId;
    @NotNull @Pattern(regexp = "[0-9]{16}")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private final String testCardNumber;
    @NotNull @Min(1) @Max(12)
    private final Integer expiryMonth;
    @NotNull @Min(2000) @Max(9999)
    private final Integer expiryYear;
    @NotNull @Pattern(regexp = "[0-9]{3,4}")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private final String testSecurityCode;
    @NotBlank @Size(max = 100)
    private final String merchantName;
    @NotNull @Digits(integer = 12, fraction = 2)
    @DecimalMin(value = "0", inclusive = false)
    private final BigDecimal amount;
    @NotNull @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private final String requestId;

    @JsonCreator
    public PurchaseRequest(@JsonProperty("cardId") Long cardId,
                           @JsonProperty("testCardNumber") String testCardNumber,
                           @JsonProperty("expiryMonth") Integer expiryMonth,
                           @JsonProperty("expiryYear") Integer expiryYear,
                           @JsonProperty("testSecurityCode") String testSecurityCode,
                           @JsonProperty("merchantName") String merchantName,
                           @JsonProperty("amount") BigDecimal amount,
                           @JsonProperty("requestId") String requestId) {
        this.cardId = cardId;
        this.testCardNumber = testCardNumber;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.testSecurityCode = testSecurityCode;
        this.merchantName = merchantName;
        this.amount = amount;
        this.requestId = requestId;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getTestCardNumber() { return testCardNumber; }
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getTestSecurityCode() { return testSecurityCode; }
    public Long getCardId() { return cardId; }
    public Integer getExpiryMonth() { return expiryMonth; }
    public Integer getExpiryYear() { return expiryYear; }
    public String getMerchantName() { return merchantName; }
    public BigDecimal getAmount() { return amount; }
    public String getRequestId() { return requestId; }

    @Override
    public String toString() { return "PurchaseRequest"; }
}
