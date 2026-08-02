package com.wallet.transfer.api.dto.response;

import com.wallet.transfer.domain.entity.Transfer;
import com.wallet.transfer.domain.enums.TransferStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class TransferResponse {

    private UUID transferId;
    private String idempotencyKey;
    private UUID fromWalletId;
    private UUID toWalletId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private TransferStatus status;
    private String failureReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime processedAt;
    private List<LedgerEntryResponse> ledgerEntries;

    public static TransferResponse from(Transfer t, List<LedgerEntryResponse> entries) {
        TransferResponse r=new TransferResponse();
        r.setTransferId(t.getId());
        r.setIdempotencyKey(t.getIdempotencyKey());
        r.setFromWalletId(t.getFromWallet().getId());
        r.setToWalletId(t.getToWallet().getId());
        r.setAmount(t.getAmount());
        r.setCurrency(t.getCurrency());
        r.setDescription(t.getDescription());
        r.setStatus(t.getStatus());
        r.setFailureReason(t.getFailureReason());
        r.setCreatedAt(t.getCreatedAt());
        r.setProcessedAt(t.getProcessedAt());
        r.setLedgerEntries(entries); return r;
    }

}
