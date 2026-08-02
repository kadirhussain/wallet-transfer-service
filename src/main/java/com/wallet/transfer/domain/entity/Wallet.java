package com.wallet.transfer.domain.entity;

import com.wallet.transfer.domain.enums.WalletStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "wallets")
public class Wallet {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="id", updatable = false, nullable = false)
    private UUID id;

    @Column(name="owner_id",nullable=false)
    private String ownerId;

    @Column(name="currency",nullable=false,length=3)
    private String currency;

    @Column(name="balance",nullable=false,precision=19,scale=4)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WalletStatus status;

    @CreationTimestamp
    @Column(name="created_at",updatable=false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;


    public boolean isActive() {
        return  WalletStatus.ACTIVE.equals(status);
    }

    public void debit(BigDecimal amount) {
        if(amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Debit must be positive");
        if(balance.compareTo(amount) <= 0) throw new IllegalStateException("Insufficient balance: wallet="+id+" balance="+balance+" debit="+amount);
        this.balance = balance.subtract(amount);
    }

    public void creadit(BigDecimal amount){
        if(amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Creadit must be positive");
        this.balance=balance.add(amount);
    }

    public static Wallet create(String ownerId, String currency, BigDecimal initialBalance) {
        Wallet w= new Wallet();
        w.setOwnerId(ownerId);
        w.setCurrency(currency);
        w.setBalance(initialBalance);
        w.setStatus(WalletStatus.ACTIVE);
        return w;
    }

}
