package com.marvens.capstone.controller.dto;

import com.marvens.capstone.entity.CreditAccount;
import jakarta.validation.constraints.NotNull;

public record AccountStatusRequest(@NotNull CreditAccount.Status status) {
}
