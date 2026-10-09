package com.marvens.capstone.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "credit_accounts")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CreditAccount {
    public enum Status { ACTIVE, FROZEN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private AppUser user;

    @Column(name = "credit_limit", nullable = false, precision = 14, scale = 2)
    @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2)
    private BigDecimal creditLimit;

    @Column(name = "outstanding_balance", nullable = false, precision = 14, scale = 2)
    @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2)
    private BigDecimal outstandingBalance = new BigDecimal("0.00");

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "varchar(20)")
    @NotNull
    private Status status;

    public CreditAccount() {
        // JPA needs a no-argument constructor to load existing rows.
    }

    public Long getId() {
        return id;
    }

    @JsonIgnore
    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public String getOwnerName() {
        return user.getDisplayName();
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public BigDecimal getAvailableCredit() {
        return creditLimit.subtract(outstandingBalance);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
