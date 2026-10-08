package com.marvens.capstone.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;

// Request format is checked by @Valid; account/card/financial rules stay in the service.
public class PurchaseRequest {
    @NotNull @Positive
    public Long cardId;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank @Pattern(regexp = "[0-9]{16}", message = "must contain 16 fictional digits")
    public String testCardNumber;
    @NotNull @Min(1) @Max(12)
    public Integer expiryMonth;
    @NotNull @Min(2000) @Max(9999)
    public Integer expiryYear;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank @Pattern(regexp = "[0-9]{3,4}", message = "must contain 3 or 4 fictional digits")
    public String testSecurityCode;
    @NotBlank @Size(max = 100)
    public String merchantName;
    @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2)
    public BigDecimal amount;
    @NotBlank @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", message = "must be a UUID")
    public String requestId;

    @Override
    public String toString() {
        return "PurchaseRequest";
    }
}
