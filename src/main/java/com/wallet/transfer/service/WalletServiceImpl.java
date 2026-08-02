package com.wallet.transfer.service;

import com.wallet.transfer.api.dto.request.CreateWalletRequest;
import com.wallet.transfer.api.dto.response.WalletResponse;
import com.wallet.transfer.domain.entity.Wallet;
import com.wallet.transfer.domain.exception.WalletNotFoundException;
import com.wallet.transfer.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public WalletResponse create(CreateWalletRequest req) {

        Wallet w = Wallet.create(
                req.getOwnerId(),
                req.getCurrency(),
                req.getInitialBalance()
        );
        w = walletRepository.save(w);

        log.info("Wallet created: id={} owner={}", w.getId(), w.getOwnerId());

        return WalletResponse.from(w);
    }

    @Override
    @Transactional(readOnly=true)
    public WalletResponse findById(UUID id) {
        return walletRepository.findById(id).map(WalletResponse::from)
                .orElseThrow(() -> new WalletNotFoundException(id));
    }


}
