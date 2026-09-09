package com.wallet.transfer.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;


import java.util.List;

public record CreateUser(@NotBlank String name,
                         @NotBlank @Size(min = 8) String password,
                         @NotBlank @Pattern(regexp = "^\\+?[0-9]{7,15}$") String mobile,
                         @Email @NotBlank String email,
                         List<String> roles) {
}
