package com.marvens.capstone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvens.capstone.dto.PurchaseRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RequestDtoTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void fictionalInputCanBeReadButCannotBePrintedOrSerialized() throws Exception {
        var request = json.readValue(validJson(), PurchaseRequest.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(request)).isEmpty();
        }
        assertThat(request.getTestCardNumber()).isEqualTo(testNumber());
        assertThat(request.getTestSecurityCode()).isEqualTo(testCode());
        assertThat(request.getAmount()).isEqualByComparingTo("25.00");
        assertThat(request.toString()).isEqualTo("PurchaseRequest");
        assertThat(json.writeValueAsString(request)).doesNotContain(testNumber(), testCode(),
                "testCardNumber", "testSecurityCode");
    }

    @Test
    void standardDecimalBindingAcceptsJsonTextAndNumbers() throws Exception {
        var text = json.readValue(validJson(), PurchaseRequest.class);
        var number = json.readValue(validJson().replace("\"25.00\"", "25.00"), PurchaseRequest.class);
        assertThat(text.getAmount()).isEqualByComparingTo("25.00");
        assertThat(number.getAmount()).isEqualByComparingTo(text.getAmount());
    }

    static String testNumber() { return "4242".repeat(4); }
    static String testCode() { return "9".repeat(3); }
    static String validJson() {
        return """
                {"cardId":7,"testCardNumber":"%s","expiryMonth":12,"expiryYear":2030,
                 "testSecurityCode":"%s","merchantName":"Demo Shop","amount":"25.00",
                 "requestId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"}
                """.formatted(testNumber(), testCode());
    }
}
