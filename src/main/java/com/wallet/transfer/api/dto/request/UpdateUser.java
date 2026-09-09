package com.wallet.transfer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUser(@NotBlank String name,
                         @NotBlank @Pattern(regexp = "^\\+?[0-9]{7,15}$") String mobile) {
}
