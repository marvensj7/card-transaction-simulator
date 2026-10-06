package com.marvens.capstone.service;

import java.util.Locale;
import com.marvens.capstone.exception.InvalidRequestException;
import org.springframework.data.domain.PageRequest;

final class RequestChecks {
    private RequestChecks() { }

    static String requestId(String value) {
        if (value == null || !value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new InvalidRequestException("Request ID must be a UUID.");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    static PageRequest page(Integer page, Integer size) {
        int number = page == null ? 0 : page;
        int count = size == null ? 20 : size;
        if (number < 0 || count < 1) {
            throw new InvalidRequestException("Page must be nonnegative and size must be positive.");
        }
        return PageRequest.of(number, Math.min(count, 50));
    }
}
