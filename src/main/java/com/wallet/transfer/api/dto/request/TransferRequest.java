package com.wallet.transfer.api.dto.request;

import com.wallet.transfer.domain.enums.TransferStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransferRequest {

    @NotBlank(message = "idempotencyKey is required") @Size(min = 1, max = 255)
    private String idempotencyKey;

    @NotNull(message="fromWalletId is required")
    private UUID fromWalletId;

    @NotNull(message = "toWalletId is required")
    private UUID toWalletId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.0001", message = "amount must be greater than 0")
    @DecimalMax(value = "10000000", message = "amount must not exceed 10,000,000")
    @Digits(integer = 11, fraction = 4)
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message="currency must be 3 uppercase letters e.g. INR")
    private String currency;

    @Size(max=500)
    private String description;


}
