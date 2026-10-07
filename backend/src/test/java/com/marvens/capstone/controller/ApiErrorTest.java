package com.marvens.capstone.controller;

import java.time.Instant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marvens.capstone.dto.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.assertj.core.api.Assertions.*;

class ApiErrorTest {
    @Test
    void errorHasExactlyTheFourDocumentedFieldsAndAUtcTimestamp() throws Exception {
        Instant before = Instant.now();
        var error = ApiError.of(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Check the request fields.");
        var body = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(error));
        assertThat(body.size()).isEqualTo(4);
        assertThat(body.path("status").asInt()).isEqualTo(400);
        assertThat(body.path("code").asText()).isEqualTo("INVALID_REQUEST");
        assertThat(body.path("message").asText()).isEqualTo("Check the request fields.");
        assertThat(body.path("timestamp").asText()).endsWith("Z");
        assertThat(Instant.parse(body.path("timestamp").asText())).isBetween(before, Instant.now());
    }
}
