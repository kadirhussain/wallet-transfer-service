package com.wallet.transfer.repository;

import com.wallet.transfer.domain.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet,UUID> {

    /** SELECT FOR UPDATE — IDs must be passed sorted ASC to prevent deadlocks. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.id in :ids order by w.id asc")
    List<Wallet> findAndLockByIds(@Param("ids") List<UUID> ids);
}
