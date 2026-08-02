package com.wallet.transfer.service;



import com.wallet.transfer.api.dto.request.CreateWalletRequest;
import com.wallet.transfer.api.dto.response.WalletResponse;

import java.util.UUID;

public interface WalletService {

    WalletResponse create(CreateWalletRequest request);
    WalletResponse findById(UUID walletId);
}
