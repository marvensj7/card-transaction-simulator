package com.marvens.capstone.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "card_transactions")
public class CardTransaction {
    public enum Type { PURCHASE, REFUND }
    public enum Status { APPROVED, DECLINED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private CreditAccount account;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private DemoCard card;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20, columnDefinition = "varchar(20)")
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "varchar(20)")
    private Status status;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "outstanding_after", nullable = false, precision = 14, scale = 2)
    private BigDecimal outstandingAfter;

    @Column(name = "merchant_name", nullable = false, length = 100)
    private String merchantName;

    @Column(name = "reason_code", length = 40)
    private String reasonCode;

    // MySQL DATETIME has no zone. The service supplies UTC, using whole seconds.
    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(6)")
    private LocalDateTime createdAt;

    @Column(name = "request_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String requestId;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "original_purchase_id")
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
