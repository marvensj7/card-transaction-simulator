package com.marvens.capstone.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "card_transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_card_transactions_request", columnNames = {"account_id", "request_id"}),
        @UniqueConstraint(name = "uq_card_transactions_original_purchase", columnNames = "original_purchase_id")
}, indexes = @Index(name = "idx_card_transactions_account_history", columnList = "account_id, id"))
public class CardTransaction {
    public enum Type { PURCHASE, REFUND }
    public enum Status { APPROVED, DECLINED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @JsonIgnore
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_card_transactions_account"))
    private CreditAccount account;

    @JsonIgnore
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_card_transactions_card"))
    private DemoCard card;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20, columnDefinition = "varchar(20)")
    private Type type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "varchar(20)")
    private Status status;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @Digits(integer = 12, fraction = 2)
    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @NotNull
    @DecimalMin("0")
    @Digits(integer = 12, fraction = 2)
    @Column(name = "outstanding_after", nullable = false, precision = 14, scale = 2)
    private BigDecimal outstandingAfter;

    @NotBlank
    @Size(max = 100)
    @Column(name = "merchant_name", nullable = false, length = 100)
    private String merchantName;

    @Size(max = 40)
    @Column(name = "reason_code", length = 40)
    private String reasonCode;

    // MySQL DATETIME has no zone. The service must supply UTC with microsecond precision.
    @NotNull
    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(6)")
    private LocalDateTime createdAt;

    @NotNull
    @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    @Column(name = "request_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String requestId;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "original_purchase_id",
            foreignKey = @ForeignKey(name = "fk_card_transactions_purchase"))
    private CardTransaction originalPurchase;

    public CardTransaction() {
    }

    public Long getId() { return id; }
    public CreditAccount getAccount() { return account; }
    public void setAccount(CreditAccount account) { this.account = account; }
    public DemoCard getCard() { return card; }
    public void setCard(DemoCard card) { this.card = card; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getOutstandingAfter() { return outstandingAfter; }
    public void setOutstandingAfter(BigDecimal outstandingAfter) { this.outstandingAfter = outstandingAfter; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public CardTransaction getOriginalPurchase() { return originalPurchase; }
    public void setOriginalPurchase(CardTransaction originalPurchase) { this.originalPurchase = originalPurchase; }
}
