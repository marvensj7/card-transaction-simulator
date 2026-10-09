package com.marvens.capstone;

import com.marvens.capstone.dto.CardResponse;
import com.marvens.capstone.entity.AppUser;
import com.marvens.capstone.entity.CreditAccount;
import com.marvens.capstone.entity.DemoCard;
import com.marvens.capstone.service.FictionalCardNumbers;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FictionalCardNumbersTest {
    @Test
    void differentAccountsWithTheSameLastFourRequireTheirCompleteAssignedNumbers() {
        DemoCard firstCard = assignedCard(17L);
        DemoCard secondCard = assignedCard(10017L);
        String firstNumber = "0000" + String.format("%012d", 17L);
        String secondNumber = "0000" + String.format("%012d", 10017L);

        assertThat(firstCard.getLastFour()).isEqualTo(secondCard.getLastFour());
        assertThat(FictionalCardNumbers.matchesAssignedNumber(firstCard, firstNumber)).isTrue();
        assertThat(FictionalCardNumbers.matchesAssignedNumber(secondCard, secondNumber)).isTrue();
        assertThat(FictionalCardNumbers.matchesAssignedNumber(firstCard, secondNumber)).isFalse();
        assertThat(FictionalCardNumbers.matchesAssignedNumber(secondCard, firstNumber)).isFalse();
        assertThat(FictionalCardNumbers.matchesAssignedNumber(firstCard, null)).isFalse();
        assertThat(FictionalCardNumbers.matchesAssignedNumber(firstCard, "0017")).isFalse();
        assertThat(FictionalCardNumbers.matchesAssignedNumber(firstCard, TestData.testNumber())).isFalse();
        firstCard.setLastFour("9999");
        assertThat(FictionalCardNumbers.matchesAssignedNumber(firstCard, firstNumber)).isFalse();
    }

    @Test
    void recreatedCardObjectsUseOnlyPersistedDetailsAndReturnMaskedInstructions() {
        DemoCard assignedCard = assignedCard(10017L);
        DemoCard reloadedCard = assignedCard(10017L);
        CardResponse response = new CardResponse(reloadedCard);
        assertThat(response.maskedNumber).isEqualTo("•••• 0017");
        assertThat(response.numberEntryHint).contains("account ID 10017", "12 digits");
        assertThat(response.numberEntryHint).doesNotContain("0000" + String.format("%012d", 10017L));
        assertThat(reloadedCard.getTestProfile()).isEqualTo(assignedCard.getTestProfile());
        assertThat(FictionalCardNumbers.matchesAssignedNumber(reloadedCard,
                "0000" + String.format("%012d", 10017L))).isTrue();
    }

    @Test
    void legacyCardsRemainUsableAndUnsupportedProfilesFailClosed() {
        DemoCard legacyCard = TestData.card(TestData.account(TestData.user(AppUser.Role.USER)));
        assertThat(FictionalCardNumbers.matchesAssignedNumber(legacyCard, TestData.testNumber())).isTrue();
        assertThat(FictionalCardNumbers.entryHint(legacyCard)).contains("legacy", "4242 repeated");
        legacyCard.setLastFour("0001");
        assertThat(FictionalCardNumbers.matchesAssignedNumber(legacyCard, TestData.testNumber())).isFalse();
        legacyCard.setTestProfile("UNSUPPORTED");
        assertThat(FictionalCardNumbers.matchesAssignedNumber(legacyCard, TestData.testNumber())).isFalse();
        assertThat(FictionalCardNumbers.entryHint(legacyCard)).contains("unsupported");
    }

    @Test
    void unsupportedAccountIdsCannotSilentlyTruncateOrReuseAnotherNumber() {
        for (Long accountId : new Long[] {null, 0L, -1L, 1_000_000_000_000L}) {
            assertThatThrownBy(() -> assignedCard(accountId)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Fictional cards require an account ID of 1 to 12 digits.");
        }
        DemoCard largestCard = assignedCard(999_999_999_999L);
        assertThat(FictionalCardNumbers.matchesAssignedNumber(largestCard, "0000" + "9".repeat(12))).isTrue();
    }

    private DemoCard assignedCard(Long accountId) {
        CreditAccount customerAccount = TestData.account(TestData.user(AppUser.Role.USER));
        ReflectionTestUtils.setField(customerAccount, "id", accountId);
        DemoCard assignedCard = TestData.card(customerAccount);
        FictionalCardNumbers.assignTo(assignedCard);
        return assignedCard;
    }
}
