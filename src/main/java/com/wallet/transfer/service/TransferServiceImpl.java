package com.wallet.transfer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.transfer.api.dto.request.TransferRequest;
import com.wallet.transfer.api.dto.response.LedgerEntryResponse;
import com.wallet.transfer.api.dto.response.TransferResponse;
import com.wallet.transfer.domain.entity.IdempotencyKey;
import com.wallet.transfer.domain.entity.LedgerEntry;
import com.wallet.transfer.domain.entity.Transfer;
import com.wallet.transfer.domain.entity.Wallet;
import com.wallet.transfer.domain.enums.IdempotencyStatus;
import com.wallet.transfer.domain.enums.LedgerEntryType;
import com.wallet.transfer.domain.exception.*;
import com.wallet.transfer.infrastructure.metrics.TransferMetrics;
import com.wallet.transfer.repository.IdempotencyKeyRepository;
import com.wallet.transfer.repository.LedgerEntryRepository;
import com.wallet.transfer.repository.TransferRepository;
import com.wallet.transfer.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Core transfer orchestrator.
 * <p>
 * Full transaction boundary:
 * 1. Idempotency guard  — INSERT ... ON CONFLICT DO NOTHING
 * 2. Pessimistic lock   — SELECT FOR UPDATE (wallet IDs sorted ASC, deadlock-safe)
 * 3. Validation         — active wallets, currency match, sufficient balance
 * 4. Double-entry ledger — DEBIT + CREDIT entries (append-only)
 * 5. Balance mutation   — wallet.debit / wallet.credit
 * 6. Status transition  — PENDING → PROCESSED / FAILED
 * 7. Cache response     — idempotency_keys updated with serialised response
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final TransferMetrics metrics;
    private final ObjectMapper objectMapper;


    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransferResponse execute(TransferRequest req) {
        MDC.put("idempotencyKey", req.getIdempotencyKey());
        log.info("Transfer request: from={} to={} amount={} ccy={}", req.getFromWalletId(), req.getToWalletId(), req.getAmount(), req.getCurrency());

        //── 1. Idempotency guard ──────────────────────────────────────────────
        //Atomic INSERT ON CONFLICT DO NOTHING — DB PK is the only reliable guard.
        //Returns 1 = new key (proceed), 0 = already seen (short-circuit).
        int inserted = idempotencyKeyRepository.insertIfAbsent(req.getIdempotencyKey());
        if (inserted == 0) {
            return handleDuplicate(req.getIdempotencyKey());
        }

        // ── 2. Same-wallet guard ──────────────────────────────────────────────
        if (req.getFromWalletId().equals(req.getToWalletId())) {
            throw new SameWalletTransferException();
        }


        //--3 Acquire perssimistic row locks(deadLock-safe: always lock lower UUID first----------

        List<UUID> sortedIds = new ArrayList<>(List.of(req.getFromWalletId(), req.getFromWalletId()));
        Collections.sort(sortedIds);

        List<Wallet> locked = walletRepository.findAndLockByIds(sortedIds);

        if (locked.size() != 2) {
            // Identify which wallet is missing
            Set<UUID> found = new HashSet<>();
            locked.forEach(w -> found.add(w.getId()));
            UUID missing = !found.contains(req.getFromWalletId()) ? req.getFromWalletId() : req.getToWalletId();
            throw new WalletNotFoundException(missing);
        }

        Map<UUID, Wallet> byId = new HashMap<>();
        locked.forEach(w -> byId.put(w.getId(), w));
        Wallet fromWallet = byId.get(req.getFromWalletId());
        Wallet toWallet = byId.get(req.getToWalletId());

        // ── 4. Business validations ───────────────────────────────────────────
        if (!fromWallet.isActive())
            throw new WalletInactiveException(fromWallet.getId(), fromWallet.getStatus().name());
        if (!toWallet.isActive())
            throw new WalletInactiveException(toWallet.getId(), toWallet.getStatus().name());
        if (!fromWallet.getCurrency().equals(req.getCurrency()))
            throw new CurrencyMismatchException(fromWallet.getCurrency(), req.getCurrency());
        if (fromWallet.getBalance().compareTo(req.getAmount()) < 0)
            throw new InsufficientBalanceException(fromWallet.getId(), fromWallet.getBalance(), req.getAmount());

        // ── 5. Persist transfer record (PENDING) ──────────────────────────────

        Transfer transfer = Transfer.create(req.getIdempotencyKey(), fromWallet, toWallet, req.getAmount(), req.getCurrency(), req.getDescription());
        transfer = transferRepository.save(transfer);

        // ── 6. Double-entry ledger (capture balances BEFORE mutation) ─────────
        var debitBefore = fromWallet.getBalance();
        var creditBefore = toWallet.getBalance();

        fromWallet.debit(req.getAmount());
        toWallet.creadit(req.getAmount());

        LedgerEntry debitEntry = LedgerEntry.of(fromWallet, transfer, LedgerEntryType.DEBIT, req.getAmount(), debitBefore, fromWallet.getBalance());
        LedgerEntry creditEntry = LedgerEntry.of(toWallet, transfer, LedgerEntryType.CREDIT, req.getAmount(), creditBefore, toWallet.getBalance());

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);


        // ── 7. Mark PROCESSED ─────────────────────────────────────────────────
        transfer.markProcessed();
        transferRepository.save(transfer);

        // ── 8. Cache response in idempotency record ───────────────────────────

        List<LedgerEntryResponse> ledgerEntries = List.of(
                LedgerEntryResponse.from(debitEntry),
                LedgerEntryResponse.from(creditEntry));
        TransferResponse response = TransferResponse.from(transfer, ledgerEntries);

        IdempotencyKey idk = idempotencyKeyRepository
                .findByIdempotencyKey(req.getIdempotencyKey()).orElseThrow();
        idk.setTransfer(transfer);
        idk.setStatus(IdempotencyStatus.COMPLETED);
        idk.setResponseBody(serialize(response));
        idempotencyKeyRepository.save(idk);

        metrics.recordSuccess(req.getAmount());
        log.info("Transfer PROCESSED: id={} from={} to={} amount={}",
                transfer.getId(), fromWallet.getId(), toWallet.getId(), req.getAmount());
        MDC.remove("idempotencyKey");
        return response;


    }

    private TransferResponse handleDuplicate(String key) {
        log.info("Duplicate idempotency key: {}", key);
        metrics.recordDuplicate();
        IdempotencyKey idk = idempotencyKeyRepository.findByIdempotencyKey(key).orElseThrow(() -> new IllegalStateException("Idempotency key vanished: " + key));
        if (idk.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            if (idk.getCreatedAt().isBefore(OffsetDateTime.now().minusMinutes(30)))
                log.warn("Stale IN_PROGRESS key detected: {} created={}", key, idk.getCreatedAt());
            throw new IdempotencyConflictException(key);
        }
        return deserialize(idk.getResponseBody());
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse findById(UUID id) {
        Transfer t = transferRepository.findById(id)
                .orElseThrow(() -> new TransferNotFoundException(id));
        List<LedgerEntryResponse> entries = ledgerEntryRepository
                .findByTransferIdOrderByCreatedAtAsc(id)
                .stream().map(LedgerEntryResponse::from).toList();
        return TransferResponse.from(t, entries);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransferResponse> findByWalletId(UUID walletId, Pageable pageable) {
        walletRepository.findById(walletId).orElseThrow(() -> new WalletNotFoundException(walletId));
        return transferRepository.findByWalletId(walletId, pageable)
                .map(t -> TransferResponse.from(t, List.of()));
    }

    private String serialize(TransferResponse r) {
        try {
            return objectMapper.writeValueAsString(r);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    private TransferResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, TransferResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization failed", e);
        }
    }


}
