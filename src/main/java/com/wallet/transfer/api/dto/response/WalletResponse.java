package com.wallet.transfer.api.dto.response;

import com.wallet.transfer.domain.entity.Wallet;
import com.wallet.transfer.domain.enums.WalletStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class WalletResponse {

    private UUID walletId;
    private String ownerId;
    private String currency;
    private BigDecimal balance;
    private WalletStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static WalletResponse from(Wallet w) {
        WalletResponse r=new WalletResponse();
        r.setWalletId(w.getId());
        r.setOwnerId(w.getOwnerId());
        r.setCurrency(w.getCurrency());
        r.setBalance(w.getBalance());
        r.setStatus(w.getStatus());
        r.setCreatedAt(w.getCreatedAt());
        r.setUpdatedAt(w.getUpdatedAt());
        return r;
    }
}
