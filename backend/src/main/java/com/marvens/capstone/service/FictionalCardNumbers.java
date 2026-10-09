package com.marvens.capstone.service;

import java.util.Locale;
import com.marvens.capstone.entity.DemoCard;

// Simulation only: this predictable number is neither a payment credential nor a secret.
public final class FictionalCardNumbers {
    public static final String ACCOUNT_PROFILE = "ACCOUNT_V1";
    private static final long MAX_ACCOUNT_ID = 999_999_999_999L;

    private FictionalCardNumbers() {
    }

    public static void assignTo(DemoCard assignedCard) {
        String fictionalNumber = numberForAccount(assignedCard.getAccount().getId());
        assignedCard.setTestProfile(ACCOUNT_PROFILE);
        assignedCard.setLastFour(fictionalNumber.substring(12));
    }

    public static boolean matchesAssignedNumber(DemoCard assignedCard, String submittedNumber) {
        String expectedNumber;
        if (ACCOUNT_PROFILE.equals(assignedCard.getTestProfile())) {
            expectedNumber = numberForAccount(assignedCard.getAccount().getId());
        } else if ("DEMO_4242".equals(assignedCard.getTestProfile())) {
            // Keep previously assigned cards and their transaction history usable.
            expectedNumber = "4242".repeat(4);
        } else {
            return false;
        }

        return expectedNumber.equals(submittedNumber)
                && expectedNumber.substring(12).equals(assignedCard.getLastFour());
    }

    public static String entryHint(DemoCard assignedCard) {
        if (ACCOUNT_PROFILE.equals(assignedCard.getTestProfile())) {
            return "For this simulation, enter 0000 followed by account ID "
                    + assignedCard.getAccount().getId() + " padded to 12 digits with leading zeros.";
        } else if ("DEMO_4242".equals(assignedCard.getTestProfile())) {
            return "For this legacy classroom card, enter 4242 repeated four times.";
        } else {
            return "This fictional card profile is unsupported. Ask the demo administrator for help.";
        }
    }

    private static String numberForAccount(Long accountId) {
        if (accountId == null || accountId < 1 || accountId > MAX_ACCOUNT_ID) {
            throw new IllegalArgumentException("Fictional cards require an account ID of 1 to 12 digits.");
        }

        return "0000" + String.format(Locale.ROOT, "%012d", accountId);
    }
}
