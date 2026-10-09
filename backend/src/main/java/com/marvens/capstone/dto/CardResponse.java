package com.marvens.capstone.dto;

import com.marvens.capstone.entity.DemoCard;
import com.marvens.capstone.service.FictionalCardNumbers;
import io.swagger.v3.oas.annotations.media.Schema;

public class CardResponse {
    public final Long id;
    public final String label;
    public final String maskedNumber;
    @Schema(description = "Simulation-only entry instruction; the full fictional number is never returned")
    public final String numberEntryHint;
    public final int expiryMonth;
    public final int expiryYear;

    public CardResponse(DemoCard card) {
        id = card.getId();
        label = card.getLabel();
        maskedNumber = "•••• " + card.getLastFour();
        numberEntryHint = FictionalCardNumbers.entryHint(card);
        expiryMonth = card.getExpiryMonth();
        expiryYear = card.getExpiryYear();
    }
}
