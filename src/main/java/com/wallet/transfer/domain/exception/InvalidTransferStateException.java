package com.wallet.transfer.domain.exception;


public class InvalidTransferStateException extends RuntimeException {

    public InvalidTransferStateException(String m) {
        super(m);
    }
}
