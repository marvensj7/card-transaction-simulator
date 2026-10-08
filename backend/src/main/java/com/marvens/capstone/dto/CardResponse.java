package com.marvens.capstone.dto;

import com.marvens.capstone.entity.DemoCard;

public class CardResponse {
    public final Long id;
    public final String label;
    public final String maskedNumber;
    public final int expiryMonth;
    public final int expiryYear;

    public CardResponse(DemoCard card) {
        id = card.getId();
        label = card.getLabel();
        maskedNumber = "•••• " + card.getLastFour();
        expiryMonth = card.getExpiryMonth();
        expiryYear = card.getExpiryYear();
    }
}
