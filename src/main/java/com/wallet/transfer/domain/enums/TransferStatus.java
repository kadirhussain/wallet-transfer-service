package com.wallet.transfer.domain.enums;

import java.util.Map;
import java.util.Set;

public enum TransferStatus {
 PENDING, PROCESSED, FAILED;

    public static final Map<TransferStatus, Set<TransferStatus>> T= Map.of(
            PENDING, Set.of(PROCESSED,FAILED),
            PROCESSED, Set.of(),
            FAILED, Set.of()
    );


    public boolean canTransitionTo(TransferStatus status) {
        return T.getOrDefault(this,Set.of()).contains(status);
    }

    public boolean isTerminal(){
        return this == PROCESSED || this == FAILED;
    }


}
