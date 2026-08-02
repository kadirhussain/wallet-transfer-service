package com.wallet.transfer.domain.exception;

import java.util.UUID;

public class WalletInactiveException extends RuntimeException {
    public WalletInactiveException(UUID id, String status) {
        super("Wallet " + id + "is not inactive status : " + status);
    }
}
