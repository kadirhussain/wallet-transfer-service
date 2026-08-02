package com.wallet.transfer.domain.entity;

import com.wallet.transfer.domain.enums.IdempotencyStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name="idempotency_keys")
public class IdempotencyKey {

    @Id
    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 255)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="transfer_id")
    private Transfer transfer;

    @Enumerated(EnumType.STRING) @Column(name="status",nullable=false,length=20)
    private IdempotencyStatus status;

    @Column(name="response_body",columnDefinition="TEXT")
    private String responseBody;

    @CreationTimestamp
    @Column(name="created_at",updatable=false)
    private OffsetDateTime createdAt;

    @Column(name="expires_at",nullable=false)
    private OffsetDateTime expiresAt;

    public boolean isExpired(){
        return OffsetDateTime.now().isAfter(expiresAt);
    }

    public static IdempotencyKey create(String idempotencyKey, int ttlHours){
        IdempotencyKey ik=new IdempotencyKey();
        ik.setIdempotencyKey(idempotencyKey);
        ik.setStatus(IdempotencyStatus.IN_PROGRESS);
        ik.setExpiresAt(OffsetDateTime.now().plusHours(ttlHours));
        return ik;
    }


}
