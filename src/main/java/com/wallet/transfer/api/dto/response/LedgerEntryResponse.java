package com.wallet.transfer.api.dto.response;

import com.wallet.transfer.domain.entity.LedgerEntry;
import com.wallet.transfer.domain.enums.LedgerEntryType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class LedgerEntryResponse {

    private UUID entryId;
    private UUID walletId;
    private LedgerEntryType type;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private OffsetDateTime createdAt;

    public static LedgerEntryResponse from(LedgerEntry e) {
        LedgerEntryResponse r = new LedgerEntryResponse();
        r.setEntryId(e.getId());
        r.setWalletId(e.getWallet().getId());
        r.setType(e.getEntryType());
        r.setAmount(e.getAmount());
        r.setBalanceBefore(e.getBalanceBefore());
        r.setBalanceAfter(e.getBalanceAfter());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }
}
