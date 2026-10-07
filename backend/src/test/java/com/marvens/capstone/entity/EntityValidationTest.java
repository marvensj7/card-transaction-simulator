package com.marvens.capstone.entity;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityValidationTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeValidator() {
        FACTORY.close();
    }

    @Test
    void validFictionalObjectsAllowOptionalRelationshipsAndZeroBalances() {
        AppUser user = EntityFixtures.user();
        CreditAccount account = EntityFixtures.account(user);
        DemoCard card = EntityFixtures.card(account);
        CardTransaction purchase = EntityFixtures.purchase(account, card);
        purchase.setOutstandingAfter(new BigDecimal("0.00"));

        assertThat(invalidFields(user)).isEmpty();
        assertThat(invalidFields(account)).isEmpty();
        assertThat(invalidFields(card)).isEmpty();
        assertThat(invalidFields(purchase)).isEmpty();
    }

    @Test
    void userRejectsBlankNamesInvalidEmailAndOversizedHash() {
        AppUser user = EntityFixtures.user();
        user.setDisplayName(" ");
        user.setEmail("not-an-email");
        user.setPasswordHash("x".repeat(101));
        user.setRole(null);

        assertThat(invalidFields(user)).containsExactlyInAnyOrder("displayName", "email", "passwordHash", "role");
    }

    @Test
    void moneyRequiresPositiveAmountsAndDecimalFourteenTwo() {
        CreditAccount account = EntityFixtures.account(EntityFixtures.user());
        account.setCreditLimit(BigDecimal.ZERO);
        account.setOutstandingBalance(new BigDecimal("-0.01"));
        assertThat(invalidFields(account)).containsExactlyInAnyOrder("creditLimit", "outstandingBalance");

        CardTransaction purchase = EntityFixtures.purchase(account, EntityFixtures.card(account));
        purchase.setAmount(new BigDecimal("0.001"));
        purchase.setOutstandingAfter(new BigDecimal("1000000000000.00"));
        assertThat(invalidFields(purchase)).containsExactlyInAnyOrder("amount", "outstandingAfter");
        purchase.setAmount(BigDecimal.ZERO);
        assertThat(invalidFields(purchase)).contains("amount");
    }

    @Test
    void cardRejectsBadMaskedDigitsExpiryAndMissingOwner() {
        DemoCard card = EntityFixtures.card(null);
        card.setLastFour("42a2");
        card.setExpiryMonth((byte) 13);
        card.setExpiryYear((short) 1999);
        card.setTestProfile(" ");
        card.setLabel("x".repeat(51));

        assertThat(invalidFields(card)).containsExactlyInAnyOrder(
                "account", "lastFour", "expiryMonth", "expiryYear", "testProfile", "label");
    }

    @Test
    void transactionRejectsMissingReferencesTimestampAndMalformedRequestId() {
        CardTransaction purchase = EntityFixtures.purchase(null, null);
        purchase.setCreatedAt(null);
        purchase.setRequestId("not-a-uuid");
        purchase.setMerchantName(" ");
        purchase.setReasonCode("x".repeat(41));

        assertThat(invalidFields(purchase)).containsExactlyInAnyOrder(
                "account", "card", "createdAt", "requestId", "merchantName", "reasonCode");
    }

    @Test
    void jsonOmitsPasswordHashAndDoesNotTraverseRelationships() throws Exception {
        AppUser user = EntityFixtures.user();
        CreditAccount account = EntityFixtures.account(user);

        var json = new ObjectMapper().valueToTree(user);
        assertThat(json.has("passwordHash")).isFalse();
        assertThat(json.has("creditAccount")).isFalse();
        assertThat(json.get("displayName").asText()).isEqualTo("Mapping Test User");
        assertThat(new ObjectMapper().valueToTree(account).has("user")).isFalse();
    }

    private static Set<String> invalidFields(Object entity) {
        // Compare property names only so assertion failures never print sensitive values.
        return VALIDATOR.validate(entity).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
