package com.marvens.capstone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "demo_cards", uniqueConstraints =
        @UniqueConstraint(name = "uq_demo_cards_account", columnNames = "account_id"))
public class DemoCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @JsonIgnore
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_demo_cards_account"))
    private CreditAccount account;

    @NotBlank
    @Size(max = 20)
    @Column(name = "test_profile", nullable = false, length = 20)
    private String testProfile;

    @NotBlank
    @Size(max = 50)
    @Column(name = "label", nullable = false, length = 50)
    private String label;

    @NotNull
    @Pattern(regexp = "[0-9]{4}")
    @Column(name = "last_four", nullable = false, length = 4, columnDefinition = "char(4)")
    private String lastFour;

    @NotNull
    @Min(1)
    @Max(12)
    @Column(name = "expiry_month", nullable = false)
    private Byte expiryMonth;

    @NotNull
    @Min(2000)
    @Max(9999)
    @Column(name = "expiry_year", nullable = false)
    private Short expiryYear;

    public DemoCard() {
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
