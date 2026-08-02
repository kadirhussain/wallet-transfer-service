package com.wallet.transfer.service;


import com.wallet.transfer.api.dto.request.TransferRequest;
import com.wallet.transfer.api.dto.response.TransferResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TransferService {

    TransferResponse execute(TransferRequest request);
    TransferResponse findById(UUID transferId);
    Page<TransferResponse> findByWalletId(UUID walletId, Pageable pageable);

}
