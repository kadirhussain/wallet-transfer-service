package com.wallet.transfer.api.advice;

import com.wallet.transfer.api.dto.response.ErrorResponse;
import com.wallet.transfer.domain.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> errs = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> ErrorResponse.FieldError.builder().field(f.getField()).message(f.getDefaultMessage()).build())
                .toList();
        return ResponseEntity.badRequest().body(err("VALIDATION_ERROR","Request validation failed",errs));
    }

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(WalletNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err("WALLET_NOT_FOUND",e.getMessage()));
    }
    @ExceptionHandler(WalletInactiveException.class)
    public ResponseEntity<ErrorResponse> handle(WalletInactiveException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(err("WALLET_INACTIVE",e.getMessage()));
    }
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handle(InsufficientBalanceException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err("INSUFFICIENT_BALANCE",e.getMessage()));
    }
    @ExceptionHandler(SameWalletTransferException.class)
    public ResponseEntity<ErrorResponse> handle(SameWalletTransferException e) {
        return ResponseEntity.badRequest().body(err("SAME_WALLET",e.getMessage()));
    }
    @ExceptionHandler(CurrencyMismatchException.class)
    public ResponseEntity<ErrorResponse> handle(CurrencyMismatchException e) {
        return ResponseEntity.badRequest().body(err("CURRENCY_MISMATCH",e.getMessage()));
    }
    @ExceptionHandler(TransferNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(TransferNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err("TRANSFER_NOT_FOUND",e.getMessage()));
    }
    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handle(IdempotencyConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err("IDEMPOTENCY_IN_PROGRESS",e.getMessage()));
    }
    @ExceptionHandler(InvalidTransferStateException.class)
    public ResponseEntity<ErrorResponse> handle(InvalidTransferStateException e) {
        log.error("Invalid state: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err("INVALID_STATE",e.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err("INTERNAL_ERROR","An unexpected error occurred"));
    }

    private ErrorResponse err(String code, String msg) {
        return err(code,msg,null);
    }
    private ErrorResponse err(String code, String msg, List<ErrorResponse.FieldError> errs) {
        return ErrorResponse.builder().code(code).message(msg).errors(errs).timestamp(OffsetDateTime.now()).build();
    }
}
