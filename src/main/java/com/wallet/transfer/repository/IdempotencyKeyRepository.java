package com.wallet.transfer.repository;

import com.wallet.transfer.domain.entity.IdempotencyKey;
import com.wallet.transfer.domain.enums.IdempotencyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    Optional<IdempotencyKey> findByIdempotencyKey(String key);

    /**
     * Atomic guard: 1=new key (proceed), 0=duplicate (short-circuit).
     */
    @Modifying
    @Query(value = "INSERT INTO idempotency_keys (idempotency_key, status, expires_at) VALUES (:key, 'IN_PROGRESS', NOW()+INTERVAL '24 hours') ON CONFLICT(idempotency_key) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("key") String key);

    @Modifying
    @Query("DELETE FROM IdempotencyKey ik WHERE ik.expiresAt < :t")
    int deleteExpiredBefore(@Param("t") OffsetDateTime t);

    @Query("SELECT ik FROM IdempotencyKey ik WHERE ik.status=:s AND ik.createdAt < :t")
    List<IdempotencyKey> findStale(@Param("s") IdempotencyStatus s, @Param("t") OffsetDateTime t);


}
