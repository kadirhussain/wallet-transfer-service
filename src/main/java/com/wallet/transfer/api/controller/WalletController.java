package com.wallet.transfer.api.controller;

import com.wallet.transfer.api.dto.request.CreateWalletRequest;
import com.wallet.transfer.api.dto.response.WalletResponse;
import com.wallet.transfer.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<WalletResponse> create(@Valid @RequestBody CreateWalletRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(walletService.create(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(walletService.findById(id));
    }
}
