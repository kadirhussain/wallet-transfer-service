package com.wallet.transfer.repository;

import com.wallet.transfer.domain.entity.Transfer;
import com.wallet.transfer.domain.entity.Wallet;
import com.wallet.transfer.domain.enums.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    Optional<Transfer> findByIdempotencyKey(String key);

    @Query("SELECT t FROM Transfer t JOIN FETCH t.fromWallet JOIN FETCH t.toWallet WHERE t.fromWallet.id=:wid OR t.toWallet.id=:wid ORDER BY t.createdAt DESC")
    Page<Transfer> findByWalletId(@Param("wid") UUID walletId, Pageable pageable);

    long countByStatus(TransferStatus status);
}
