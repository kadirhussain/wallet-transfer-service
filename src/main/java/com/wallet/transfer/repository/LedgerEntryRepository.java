package com.wallet.transfer.repository;

import com.wallet.transfer.domain.entity.LedgerEntry;
import com.wallet.transfer.domain.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByTransferIdOrderByCreatedAtAsc(UUID transferId);

    List<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId);

    @Query("SELECT COALESCE(SUM(CASE WHEN e.entryType='CREDIT' THEN e.amount ELSE 0 END),0) - COALESCE(SUM(CASE WHEN e.entryType='DEBIT' THEN e.amount ELSE 0 END),0) FROM LedgerEntry e WHERE e.wallet.id=:wid")
    BigDecimal computeNetBalance(@Param("wid") UUID walletId);

    @Query("SELECT COUNT(e) FROM LedgerEntry e WHERE e.transfer.id=:tid")
    long countEntriesForTransfer(@Param("tid") UUID transferId);


}
