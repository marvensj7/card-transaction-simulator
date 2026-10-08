package com.marvens.capstone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "demo_cards")
public class DemoCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    @NotNull
    private CreditAccount account;

    @Column(name = "test_profile", nullable = false, length = 20)
    @NotBlank @Size(max = 20)
    private String testProfile;

    @Column(name = "label", nullable = false, length = 50)
    @NotBlank @Size(max = 50)
    private String label;

    @Column(name = "last_four", nullable = false, length = 4, columnDefinition = "char(4)")
    @NotBlank @Pattern(regexp = "\\d{4}")
    private String lastFour;

    @Column(name = "expiry_month", nullable = false)
    @NotNull @Min(1) @Max(12)
    private Byte expiryMonth;

    @Column(name = "expiry_year", nullable = false)
    @NotNull @Min(2000) @Max(9999)
    private Short expiryYear;

    public DemoCard() {
        // JPA needs a no-argument constructor to load existing rows.
    }

    public Long getId() { return id; }
    public CreditAccount getAccount() { return account; }
    public void setAccount(CreditAccount account) { this.account = account; }
    public String getTestProfile() { return testProfile; }
    public void setTestProfile(String testProfile) { this.testProfile = testProfile; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getLastFour() { return lastFour; }
    public void setLastFour(String lastFour) { this.lastFour = lastFour; }
    public Byte getExpiryMonth() { return expiryMonth; }
    public void setExpiryMonth(Byte expiryMonth) { this.expiryMonth = expiryMonth; }
    public Short getExpiryYear() { return expiryYear; }
    public void setExpiryYear(Short expiryYear) { this.expiryYear = expiryYear; }
}
