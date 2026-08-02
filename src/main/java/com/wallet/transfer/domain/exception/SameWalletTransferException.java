package com.wallet.transfer.domain.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class SameWalletTransferException extends RuntimeException {
    public SameWalletTransferException() {
        super("Source and destination wallets must differ");
    }
}
