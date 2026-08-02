package com.wallet.transfer.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateWalletRequest {

    @NotBlank(message="ownerId is required")
    @Size(max=255)
    private String ownerId;

    @NotBlank(message="currency is required")
    @Pattern(regexp="^[A-Z]{3}$",message="currency must be 3 uppercase letters")
    private String currency;

    @DecimalMin(value="0.0",inclusive=true)
    @Digits(integer=15,fraction=4)
    private BigDecimal initialBalance = BigDecimal.ZERO;
}
