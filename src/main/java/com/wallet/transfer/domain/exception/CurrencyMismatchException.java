package com.wallet.transfer.domain.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException(String w, String r) {
        super("Wallet currency '"+w+"' does not match request currency '"+r+"'");
    }
}
