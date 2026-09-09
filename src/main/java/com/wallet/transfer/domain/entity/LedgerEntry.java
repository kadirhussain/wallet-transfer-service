package com.wallet.transfer.domain.entity;

import com.wallet.transfer.domain.enums.LedgerEntryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Immutable
@Table(name="ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="id",updatable=false,nullable=false)
    private UUID id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="wallet_id",nullable=false,updatable=false)
    private Wallet wallet;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="transfer_id",nullable=false,updatable=false)
    private Transfer transfer;

    @Enumerated(EnumType.STRING) @Column(name="entry_type",nullable=false,length=10)
    private LedgerEntryType entryType;

    @Column(name="amount",nullable=false,precision=19,scale=4)
    private BigDecimal amount;

    @Column(name="balance_before",nullable=false,precision=19,scale=4)
    private BigDecimal balanceBefore;

    @Column(name="balance_after",nullable=false,precision=19,scale=4)
    private BigDecimal balanceAfter;

    @CreationTimestamp
    @Column(name="created_at",updatable=false)
    private OffsetDateTime createdAt;

    public static LedgerEntry of(Wallet w, Transfer t, LedgerEntryType type, BigDecimal amt, BigDecimal before, BigDecimal after) {
        LedgerEntry e = new LedgerEntry();
        e.wallet = w;
        e.transfer = t;
        e.entryType = type;
        e.amount = amt;
        e.balanceBefore = before;
        e.balanceAfter = after;
        return e;
    }

}
