-- V1: Initial Schema
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE wallets (
                         id          UUID          NOT NULL DEFAULT gen_random_uuid(),
                         owner_id    VARCHAR(255)  NOT NULL,
                         currency    CHAR(3)       NOT NULL DEFAULT 'INR',
                         balance     NUMERIC(19,4) NOT NULL DEFAULT 0,
                         status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
                         created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                         updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                         version     BIGINT        NOT NULL DEFAULT 0,
                         CONSTRAINT pk_wallets PRIMARY KEY (id),
                         CONSTRAINT chk_wallets_balance  CHECK (balance >= 0),
                         CONSTRAINT chk_wallets_status   CHECK (status IN ('ACTIVE','SUSPENDED','CLOSED')),
                         CONSTRAINT chk_wallets_currency CHECK (currency ~ '^[A-Z]{3}$')
    );
CREATE INDEX idx_wallets_owner  ON wallets(owner_id);
CREATE INDEX idx_wallets_status ON wallets(status);

CREATE TABLE transfers (
                           id               UUID          NOT NULL DEFAULT gen_random_uuid(),
                           idempotency_key  VARCHAR(255)  NOT NULL,
                           from_wallet_id   UUID          NOT NULL,
                           to_wallet_id     UUID          NOT NULL,
                           amount           NUMERIC(19,4) NOT NULL,
                           currency         CHAR(3)       NOT NULL,
                           description      VARCHAR(500),
                           status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
                           failure_reason   TEXT,
                           created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                           updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                           processed_at     TIMESTAMPTZ,
                           CONSTRAINT pk_transfers          PRIMARY KEY (id),
                           CONSTRAINT uq_transfers_idem_key UNIQUE (idempotency_key),
                           CONSTRAINT fk_transfers_from     FOREIGN KEY (from_wallet_id) REFERENCES wallets(id),
                           CONSTRAINT fk_transfers_to       FOREIGN KEY (to_wallet_id)   REFERENCES wallets(id),
                           CONSTRAINT chk_transfers_amount  CHECK (amount > 0),
                           CONSTRAINT chk_transfers_status  CHECK (status IN ('PENDING','PROCESSED','FAILED')),
                           CONSTRAINT chk_transfers_wallets CHECK (from_wallet_id <> to_wallet_id),
                           CONSTRAINT chk_transfers_currency CHECK (currency ~ '^[A-Z]{3}$')
    );
CREATE INDEX idx_transfers_from    ON transfers(from_wallet_id, created_at DESC);
CREATE INDEX idx_transfers_to      ON transfers(to_wallet_id,   created_at DESC);
CREATE INDEX idx_transfers_status  ON transfers(status);
CREATE INDEX idx_transfers_created ON transfers(created_at DESC);

CREATE TABLE ledger_entries (
                                id             UUID          NOT NULL DEFAULT gen_random_uuid(),
                                wallet_id      UUID          NOT NULL,
                                transfer_id    UUID          NOT NULL,
                                entry_type     VARCHAR(10)   NOT NULL,
                                amount         NUMERIC(19,4) NOT NULL,
                                balance_before NUMERIC(19,4) NOT NULL,
                                balance_after  NUMERIC(19,4) NOT NULL,
                                created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                                CONSTRAINT pk_ledger          PRIMARY KEY (id),
                                CONSTRAINT fk_ledger_wallet   FOREIGN KEY (wallet_id)   REFERENCES wallets(id),
                                CONSTRAINT fk_ledger_transfer FOREIGN KEY (transfer_id) REFERENCES transfers(id),
                                CONSTRAINT chk_ledger_type    CHECK (entry_type IN ('DEBIT','CREDIT')),
                                CONSTRAINT chk_ledger_amount  CHECK (amount > 0),
                                CONSTRAINT chk_ledger_bal     CHECK (balance_after >= 0)
);
CREATE INDEX idx_ledger_wallet   ON ledger_entries(wallet_id, created_at DESC);
CREATE INDEX idx_ledger_transfer ON ledger_entries(transfer_id);

CREATE TABLE idempotency_keys (
                                  idempotency_key VARCHAR(255) NOT NULL,
                                  transfer_id     UUID,
                                  status          VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',
                                  response_body   TEXT,
                                  created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                                  expires_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW() + INTERVAL '24 hours',
                                  CONSTRAINT pk_idempotency      PRIMARY KEY (idempotency_key),
                                  CONSTRAINT fk_idempotency_xfer FOREIGN KEY (transfer_id) REFERENCES transfers(id),
                                  CONSTRAINT chk_idem_status     CHECK (status IN ('IN_PROGRESS','COMPLETED','FAILED'))
);

CREATE INDEX idx_idem_expires ON idempotency_keys(expires_at);
CREATE INDEX idx_idem_status  ON idempotency_keys(status);

CREATE TABLE audit_log (
                           id           BIGSERIAL    NOT NULL,
                           entity_type  VARCHAR(50)  NOT NULL,
                           entity_id    UUID         NOT NULL,
                           action       VARCHAR(50)  NOT NULL,
                           old_value    JSONB,
                           new_value    JSONB,
                           performed_by VARCHAR(255),
                           performed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                           request_id   VARCHAR(255),
                           CONSTRAINT pk_audit PRIMARY KEY (id)
);
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_time   ON audit_log(performed_at DESC);