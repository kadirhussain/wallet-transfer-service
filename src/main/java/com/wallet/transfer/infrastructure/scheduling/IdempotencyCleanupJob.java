package com.wallet.transfer.infrastructure.scheduling;

import com.wallet.transfer.domain.enums.IdempotencyStatus;
import com.wallet.transfer.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyCleanupJob {

    private final IdempotencyKeyRepository repo;

    /** Delete expired idempotency keys. Runs every hour. */
    @Scheduled(fixedRateString = "PT1H")
    @Transactional
    public void cleanupExpired() {
        int n = repo.deleteExpiredBefore(OffsetDateTime.now());
        if (n > 0) log.info("Idempotency cleanup: deleted {} expired keys", n);
    }

    /** Warn on stale IN_PROGRESS keys (> 30 min = possible crash). Runs every 5 min. */
    @Scheduled(fixedRateString = "PT5M")
    @Transactional(readOnly = true)
    public void detectStale() {
        var stale = repo.findStale(IdempotencyStatus.IN_PROGRESS, OffsetDateTime.now().minusMinutes(30));
        if (!stale.isEmpty())
            log.warn("Stale IN_PROGRESS idempotency keys ({}): {}", stale.size(),
                    stale.stream().map(ik -> ik.getIdempotencyKey()).toList());
    }

}
