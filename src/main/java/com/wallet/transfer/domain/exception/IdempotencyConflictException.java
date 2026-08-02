package com.wallet.transfer.domain.exception;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String k) {
        super("Idempotency key '"+k+"' is still being processed. Retry shortly.");
    }
}
