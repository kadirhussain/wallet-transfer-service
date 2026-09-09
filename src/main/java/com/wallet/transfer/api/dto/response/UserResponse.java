package com.wallet.transfer.api.dto.response;

import java.util.UUID;

public record UserResponse(UUID userId, String name, String email, String mobile) {
}
