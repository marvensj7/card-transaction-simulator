package com.marvens.capstone.dto;

import com.marvens.capstone.entity.DemoCard;

public record CardResponse(Long id, String label, String maskedNumber, int expiryMonth,
                           int expiryYear, String testProfile) {
    public static CardResponse from(DemoCard card) {
        return new CardResponse(card.getId(), card.getLabel(), "•••• " + card.getLastFour(),
                card.getExpiryMonth(), card.getExpiryYear(), card.getTestProfile());
    }
}
