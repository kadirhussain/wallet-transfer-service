package com.wallet.transfer.domain.entity;

import com.wallet.transfer.domain.enums.TransferStatus;
import com.wallet.transfer.domain.exception.InvalidTransferStateException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "idempotency_Key",  nullable = false, unique = true, updatable = false)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_wallet_id", nullable = false, updatable = false)
    private Wallet fromWallet;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_wallet_id", nullable = false, updatable = false)
    private Wallet toWallet;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name="currency",nullable=false,length=3)
    private String currency;

    @Column(name="description",length=500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name="status",nullable=false,length=20)
    private TransferStatus status;

    @Column(name="failure_reason",columnDefinition="TEXT")
    private String failureReason;

    @CreationTimestamp
    @Column(name="created_at",updatable=false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name="updated_at")
    private OffsetDateTime updatedAt;

    @Column(name="processed_at")
    private OffsetDateTime processedAt;

    public void markProcessed() {
        transitionTo(TransferStatus.PROCESSED);
        this.processedAt = OffsetDateTime.now();
    }

    public void markFailed(String reason) {
        transitionTo(TransferStatus.FAILED);
        this.failureReason = reason;
    }

    public void transitionTo(TransferStatus next) {
        if(!status.canTransitionTo(next))
            throw new InvalidTransferStateException("Cannot transition "+id+" from "+status+" to "+next);
        this.status=next;

    }

    public static Transfer create(String key, Wallet from, Wallet to, BigDecimal amount, String currency, String desc) {
        Transfer t=new Transfer();
        t.setIdempotencyKey(key);
        t.setFromWallet(from);
        t.setToWallet(to);
        t.setAmount(amount);
        t.setCurrency(currency);
        t.setDescription(desc);
        t.setStatus(TransferStatus.PENDING);
        return t;
    }


}
