package com.marvens.capstone.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

final class ApiFormats {
    private ApiFormats() { }

    static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    static String utc(LocalDateTime value) {
        return value.toInstant(ZoneOffset.UTC).toString();
    }
}
