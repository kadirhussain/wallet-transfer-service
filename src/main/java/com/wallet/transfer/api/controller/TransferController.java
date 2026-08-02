package com.wallet.transfer.api.controller;

import com.wallet.transfer.api.dto.request.TransferRequest;
import com.wallet.transfer.api.dto.response.PagedResponse;
import com.wallet.transfer.api.dto.response.TransferResponse;
import com.wallet.transfer.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Repository
@RequestMapping("v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> create(@Valid @RequestBody TransferRequest req){

        return ResponseEntity.status(HttpStatus.CREATED).body(transferService.execute(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> get(@PathVariable UUID id){
        return ResponseEntity.ok(transferService.findById(id));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<TransferResponse>> list(
            @RequestParam UUID walletId,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {

        Pageable p = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return ResponseEntity.ok(PagedResponse.of(transferService.findByWalletId(walletId, p), t -> t));
    }

}
