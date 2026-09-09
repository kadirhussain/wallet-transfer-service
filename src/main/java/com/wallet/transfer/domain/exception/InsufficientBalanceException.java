package com.wallet.transfer.domain.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {
    
    public InsufficientBalanceException(UUID id, BigDecimal balance, BigDecimal required) {
        super("Wallet "+id+" insufficient balance: available="+balance+" required="+required);
    }
}
