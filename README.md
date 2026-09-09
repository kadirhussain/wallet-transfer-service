# 💸 Wallet Transfer Service

A **production-grade, financial-domain backend service** implementing peer-to-peer wallet transfers with industry-standard reliability guarantees found in real-world payment platforms like Razorpay[...]

> Built to demonstrate deep understanding of **distributed systems**, **transactional safety**, **idempotency**, and **concurrent request handling** — the core engineering challenges of fintech b[...]

---

## What's new (feat: add authentication and user management with JWT and Spring Security)

This release introduces authentication and user management powered by Spring Security and JWT. Highlights:

- Add Spring Security and JWT (JJWT) dependencies with version management
- Implement JWT utility for token generation and validation
- Create User entity with roles and UserDetails integration
- Add CustomUserDetailsService for user authentication
- Implement UserService with CRUD operations
- Create AuthController for token generation endpoint
- Add UserController with user management endpoints (create, read, update, delete)
- Add JwtAuthFilter for request authentication validation
- Configure SecurityConfig with filter chain and password encoding
- Add role-based authorization (ROLE_USER, ROLE_ADMIN)
- Create database migrations for users and user_roles tables
- Add exception handling for DuplicateUserException and resource not found
- Add RequestIdFilter enhancements with logging (HTTP method, URI, client IP, User-Agent)
- Fix typo in TransferServiceImpl: "perssimistic" → "pessimistic"
- Fix bug in TransferServiceImpl: use correct wallet ID for locking
- Add spring-boot-devtools for development convenience
- Add health check endpoint in WalletController
- Add JWT configuration properties

---

## 📌 Table of Contents

- [System Overview](#system-overview)
- [Architecture](#architecture)
- [Security & Authentication](#security--authentication)
- [User Management](#user-management)
- [Key Engineering Decisions](#key-engineering-decisions)
- [Database Design](#database-design)
- [API Reference](#api-reference)
- [Transaction & Concurrency Strategy](#transaction--concurrency-strategy)
- [Idempotency Design](#idempotency-design)
- [Observability](#observability)
- [Tech Stack](#tech-stack)
- [Running Locally](#running-locally)
- [Running Tests](#running-tests)
- [Project Structure](#project-structure)

---

## System Overview

The Wallet Transfer Service processes wallet-to-wallet money transfers with the following **non-negotiable financial guarantees**:

| Guarantee | Implementation |
|---|---|
| **Exactly-once execution** | Idempotency keys with `INSERT ... ON CONFLICT DO NOTHING` |
| **No double-spend** | Pessimistic row-level locking (`SELECT FOR UPDATE`) |
| **No deadlocks** | Wallet locks always acquired in ascending UUID order |
| **Atomic transfers** | Single `@Transactional` boundary covers all 7 steps |
| **Auditable ledger** | Append-only double-entry ledger (never UPDATE/DELETE) |
| **Safe state transitions** | State machine: `PENDING → PROCESSED / FAILED` (terminal states are final) |
| **No floating-point money** | `NUMERIC(19,4)` — never `float` or `double` |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Client / API                          │
└─────────────────────┬───────────────────────────────────────┘
                      │  HTTP
┌─────────────────────▼───────────────────────────────────────┐
│                    API Layer                                  │
│   TransferController  │  WalletController  │  GlobalExceptionHandler  │
│   RequestIdFilter (MDC tracing)                              │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                  Service Layer                                │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │           TransferServiceImpl                        │    │
│  │                                                       │    │
│  │  1. Idempotency guard (INSERT ON CONFLICT DO NOTHING)│    │
│  │  2. Same-wallet validation                           │    │
│  │  3. SELECT FOR UPDATE (sorted ASC — deadlock safe)   │    │
│  │  4. Business validations (active, currency, balance) │    │
│  │  5. Double-entry ledger (DEBIT + CREDIT)             │    │
│  │  6. Balance mutation (debit source, credit dest)     │    │
│  │  7. State transition PENDING → PROCESSED             │    │
│  │  8. Cache response in idempotency record             │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                               │
│   WalletServiceImpl  │  IdempotencyCleanupJob                │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                Repository Layer                               │
│  WalletRepository  │  TransferRepository                     │
│  LedgerEntryRepository  │  IdempotencyKeyRepository          │
└─────────────────────┬───────────────────────────────────────┘
                      │  JPA / Hibernate
┌─────────────────────▼───────────────────────────────────────┐
│              PostgreSQL 16                                    │
│  wallets │ transfers │ ledger_entries                        │
│  idempotency_keys │ audit_log                                │
└─────────────────────────────────────────────────────────────┘
```

### Package Structure

```
com.wallet.transfer/
├── api/
│   ├── controller/          # HTTP handlers — thin layer, no business logic
│   ├── dto/request/         # Validated request DTOs (@NotBlank, @DecimalMin etc.)
│   ├── dto/response/        # Response DTOs with @JsonInclude(NON_NULL)
│   ├── advice/              # GlobalExceptionHandler — typed error responses
│   └── filter/              # RequestIdFilter — MDC tracing per request
├── domain/
│   ├── entity/              # JPA entities (Wallet, Transfer, LedgerEntry, IdempotencyKey)
│   ├── enums/               # TransferStatus (state machine), WalletStatus, etc.
│   └── exception/           # 8 typed domain exceptions
├── service/                 # Business logic and orchestration
├── repository/              # Spring Data JPA repositories
├── config/                  # Jackson, AppProperties
└── infrastructure/
    ├── metrics/             # Micrometer custom metrics (Prometheus)
    └── scheduling/          # Idempotency TTL cleanup job
```

---

## Key Engineering Decisions

### 1. Idempotency — Database as the Atomic Guard

The idempotency check is a single atomic SQL operation:

```sql
INSERT INTO idempotency_keys(idempotency_key, status, expires_at)
VALUES(:key, 'IN_PROGRESS', NOW() + INTERVAL '24 hours')
ON CONFLICT(idempotency_key) DO NOTHING
```

**Returns 1** → new key, proceed with transfer.  
**Returns 0** → duplicate, return cached response immediately.

This is the only reliable approach. Application-level `findById + conditional insert` has a race window between the read and the write. The database `PRIMARY KEY` constraint enforces uniqueness a[...]

---

### 2. Deadlock-Safe Pessimistic Locking

Concurrent transfers involving the same wallets must not deadlock:

```
T1: Alice → Bob    tries to lock Alice first, then Bob
T2: Bob → Alice    tries to lock Bob first, then Alice
                   ↑ Classic deadlock scenario
```

**Solution:** Always acquire locks in ascending UUID order:

```java
List<UUID> sortedIds = new ArrayList<>(List.of(fromId, toId));
Collections.sort(sortedIds);  // ← both T1 and T2 now lock same wallet first
walletRepository.findAndLockByIds(sortedIds);  // SELECT FOR UPDATE
```

```sql
SELECT w FROM Wallet w WHERE w.id IN :ids ORDER BY w.id ASC
-- LockModeType.PESSIMISTIC_WRITE → SELECT ... FOR UPDATE
```

Both transactions lock the lower UUID first → no circular wait → no deadlock.

---

### 3. Double-Entry Ledger (Append-Only)

Every transfer creates exactly two ledger entries — one DEBIT, one CREDIT. The ledger is **append-only** — rows are never updated or deleted. This is enforced by Hibernate's `@Immutable` anno[...]

```
transfer_id  wallet      type    amount   balance_before  balance_after
T1           alice       DEBIT   ₹500     ₹10,000         ₹9,500
T1           bob         CREDIT  ₹500     ₹5,000          ₹5,500
```

**Invariant:** `SUM(DEBIT) = SUM(CREDIT)` across all transfers. Verified by a reconciliation query:

```sql
SELECT
  COALESCE(SUM(CASE WHEN entry_type='CREDIT' THEN amount ELSE 0 END), 0)
- COALESCE(SUM(CASE WHEN entry_type='DEBIT'  THEN amount ELSE 0 END), 0)
FROM ledger_entries WHERE wallet_id = :walletId
```

---

### 4. Transfer State Machine

```
         ┌─────────┐
         │ PENDING │  (created, locks acquired)
         └────┬────┘
              │
    ┌─────────┴─────────┐
    │                   │
    ▼                   ▼
┌──────────┐      ┌────────┐
│PROCESSED │      │ FAILED │
│ (final)  │      │ (final)│
└──────────┘      └────────┘
```

Terminal states (`PROCESSED`, `FAILED`) cannot transition further. Enforced in the `Transfer` entity:

```java
public void markProcessed() {
    if (!status.canTransitionTo(PROCESSED))
        throw new InvalidTransferStateException(...);
    this.status = PROCESSED;
    this.processedAt = OffsetDateTime.now();
}
```

---

### 5. Why READ_COMMITTED + SELECT FOR UPDATE (not SERIALIZABLE)

`SERIALIZABLE` isolation prevents all anomalies but causes retry storms under high load. `READ_COMMITTED` + `SELECT FOR UPDATE` gives the same safety guarantee for the specific rows we care about[...]

---

## Database Design

### Schema Overview

```sql
wallets
  id UUID PK | owner_id | currency CHAR(3) | balance NUMERIC(19,4)
  status | created_at | updated_at | version (optimistic lock)

transfers
  id UUID PK | idempotency_key UNIQUE | from_wallet_id FK | to_wallet_id FK
  amount NUMERIC(19,4) | currency | status | failure_reason
  created_at | updated_at | processed_at

ledger_entries  ← APPEND-ONLY
  id UUID PK | wallet_id FK | transfer_id FK | entry_type (DEBIT/CREDIT)
  amount | balance_before | balance_after | created_at

idempotency_keys
  idempotency_key PK | transfer_id FK | status | response_body (cached JSON)
  created_at | expires_at (TTL 24h)

audit_log  ← APPEND-ONLY
  id BIGSERIAL PK | entity_type | entity_id | action
  old_value JSONB | new_value JSONB | performed_by | performed_at
```

### Key Design Decisions

| Decision | Reason |
|---|---|
| `NUMERIC(19,4)` for money | Eliminates floating-point precision errors |
| `balance_before` + `balance_after` in ledger | Point-in-time wallet reconstruction without aggregation |
| `version` on wallets | Secondary optimistic lock safety net |
| UUID primary keys | Prevents sequential PK enumeration attacks |
| Ledger is `@Immutable` | Hibernate never issues UPDATE on ledger rows |
| Idempotency keys have TTL | 24-hour expiry; cleaned up by scheduled job |

---

## API Reference

### Create Transfer
```http
POST /api/v1/transfers
Content-Type: application/json

{
  "idempotencyKey": "unique-key-001",
  "fromWalletId":  "550e8400-e29b-41d4-a716-446655440000",
  "toWalletId":    "660f9511-f30c-52e5-b827-557766551111",
  "amount":        500.00,
  "currency":      "INR",
  "description":   "Rent payment"
}
```

**Response 201** (first call) | **200** (duplicate idempotency key):
```json
{
  "transferId": "770a1234-...",
  "status": "PROCESSED",
  "fromWalletId": "550e8400-...",
  "toWalletId": "660f9511-...",
  "amount": 500.00,
  "currency": "INR",
  "processedAt": "2024-06-18T10:30:00Z",
  "ledgerEntries": [
    { "entryId": "aaa...", "walletId": "550e...", "type": "DEBIT",  "amount": 500.00, "balanceBefore": 10000.00, "balanceAfter": 9500.00 },
    { "entryId": "bbb...", "walletId": "660f...", "type": "CREDIT", "amount": 500.00, "balanceBefore":  5000.00, "balanceAfter": 5500.00 }
  ]
}
```

### Other Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/wallets` | Create wallet |
| `GET` | `/api/v1/wallets/{id}` | Get wallet details + balance |
| `POST` | `/api/v1/transfers` | Execute transfer (idempotent) |
| `GET` | `/api/v1/transfers/{id}` | Get transfer + ledger entries |
| `GET` | `/api/v1/transfers?walletId=&page=&size=` | Paginated transfer history |
| `GET` | `/actuator/health` | Health check (liveness + readiness) |
| `GET` | `/actuator/prometheus` | Prometheus metrics scrape endpoint |

### Error Responses

```json
{ "code": "INSUFFICIENT_BALANCE",    "message": "...", "timestamp": "..." }
{ "code": "WALLET_NOT_FOUND",        "message": "...", "timestamp": "..." }
{ "code": "WALLET_INACTIVE",         "message": "...", "timestamp": "..." }
{ "code": "SAME_WALLET",             "message": "...", "timestamp": "..." }
{ "code": "CURRENCY_MISMATCH",       "message": "...", "timestamp": "..." }
{ "code": "IDEMPOTENCY_IN_PROGRESS", "message": "...", "timestamp": "..." }
{ "code": "VALIDATION_ERROR",        "message": "...", "errors": [...] }
```

---

## Transaction & Concurrency Strategy

The complete transfer executes inside a **single `@Transactional` boundary**:

```
BEGIN TRANSACTION (READ COMMITTED)
  │
  ├─ 1. INSERT idempotency_keys ON CONFLICT DO NOTHING
  │       → 0 rows = duplicate → ROLLBACK → return cache
  │
  ├─ 2. Validate fromWalletId ≠ toWalletId
  │
  ├─ 3. SELECT ... FOR UPDATE (IDs sorted ASC)
  │       → acquires row locks on both wallets
  │       → second concurrent request BLOCKS here
  │
  ├─ 4. Validate: wallets active, currency match, balance sufficient
  │
  ├─ 5. INSERT transfers (status=PENDING)
  │
  ├─ 6. INSERT ledger_entries (DEBIT)
  │       INSERT ledger_entries (CREDIT)
  │       UPDATE wallets balance (both)
  │
  ├─ 7. UPDATE transfers SET status=PROCESSED
  │
  └─ 8. UPDATE idempotency_keys SET status=COMPLETED, response_body='{...}'
COMMIT
```

If **any step fails** → entire transaction rolls back → no partial state → ledger stays consistent.

---

## Idempotency Design

```
Request arrives with idempotencyKey = "K1"
        │
        ▼
INSERT idempotency_keys(K1) ON CONFLICT DO NOTHING
        │
   ┌────┴────────────────────────┐
   │ 1 row inserted              │ 0 rows inserted
   │ (new request)               │ (duplicate)
   ▼                             ▼
Execute transfer          Fetch idempotency record
        │                        │
        ▼                        ├─ COMPLETED → return cached response ✓
Cache response in          │
idempotency record         ├─ FAILED → return cached error ✓
        │                        │
        ▼                        └─ IN_PROGRESS → throw 409 Conflict
Return 201 Created                 (another thread is processing)
```

**Stale detection:** A scheduled job runs every 5 minutes and warns if any `IN_PROGRESS` key is older than 30 minutes — indicating a possible crashed execution.

**TTL cleanup:** Expired keys (> 24 hours) are deleted hourly by `IdempotencyCleanupJob`.

---

## Observability

### Structured Logging (MDC)
Every request gets a unique `requestId` injected into MDC by `RequestIdFilter`. All log lines within a request carry this ID:

```
10:30:01 [a1b2c3d4] INFO  Transfer request: from=alice to=bob amount=500
10:30:01 [a1b2c3d4] DEBUG Acquiring wallet locks: [uuid1, uuid2]
10:30:01 [a1b2c3d4] INFO  Transfer PROCESSED: id=770a...
```

### Custom Micrometer Metrics (→ Prometheus → Grafana)

| Metric | Type | Description |
|---|---|---|
| `transfers.processed.total` | Counter | Successfully processed transfers |
| `transfers.failed.total` | Counter | Failed transfers |
| `transfers.duplicate.total` | Counter | Duplicate idempotency key hits |
| `transfers.amount` | Distribution | Transfer amount distribution (p50, p95, p99) |
| `transfer.processing.duration` | Timer | End-to-end processing time |

### Health Endpoints
```
GET /actuator/health           → liveness + readiness probes
GET /actuator/prometheus       → Prometheus metrics
GET /actuator/metrics          → all metrics
```

---

## Tech Stack

| Component | Technology | Version |
|---|---|---|
| Language | Java | 17 |
| Framework | Spring Boot | 3.3.0 |
| Security | Spring Security + JJWT | Latest |
| ORM | Spring Data JPA + Hibernate | 6.x |
| Database | PostgreSQL | 16 |
| Migrations | Flyway | Latest |
| Metrics | Micrometer + Prometheus | Latest |
| Logging | Logback + Logstash Encoder | 7.4 |
| Utilities | Lombok | Latest |
| Testing | JUnit 5 + Testcontainers | Latest |
| Build | Maven | 3.8+ |
| Container | Docker + Docker Compose | Latest |

---

## Running Locally

### Prerequisites
- Java 17+
- Docker + Docker Compose
- Maven 3.5+

### Step 1 — Start PostgreSQL
```bash
docker-compose up postgres -d
```

### Step 2 — Run the application
```bash
mvn spring-boot:run
```

Flyway automatically runs migrations on startup. Seed wallets are created via `V2__seed_wallets.sql`.

### Step 3 — Create a wallet
```bash
curl -X POST http://localhost:8080/api/v1/wallets \
  -H "Content-Type: application/json" \
  -d '{
    "ownerId": "user_alice",
    "currency": "INR",
    "initialBalance": 10000
  }'
```

### Step 4 — Execute a transfer
```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -H "X-Request-ID: my-trace-001" \
  -d '{
    "idempotencyKey": "transfer-001",
    "fromWalletId": "<wallet-id-1>",
    "toWalletId": "<wallet-id-2>",
    "amount": 500,
    "currency": "INR",
    "description": "Test transfer"
  }'
```

### Step 5 — Test idempotency (run same request again)
```bash
# Same idempotencyKey — returns same response, no duplicate transfer
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "transfer-001",
    ...same body...
  }'
```

---

## Running Tests

```bash
# All tests (requires Docker for Testcontainers)
mvn test
```

### Test Coverage

| Test Class | Phase | What It Tests |
|---|---|---|
| `TransferServiceTest` | 1 + 2 | Happy path, all failure scenarios, idempotency replay |
| `LedgerConsistencyTest` | 1 | Double-entry invariant, reconciliation, balance snapshots |
| `TransferStateMachineTest` | 1 | All state transitions including invalid ones |
| `ConcurrentTransferTest` | 3 | 20 concurrent debits, bidirectional deadlock safety, 50 concurrent same-key requests |
| `TransferControllerTest` | 3 | Full HTTP layer via REST-assured + real server |

### Concurrency Test Highlights

```java
// 20 threads simultaneously debit the same source wallet
// Final balance must be exactly 48000 (50000 - 20×100)
// Zero double-spends, zero failed transactions

// 50 threads submit the same idempotency key simultaneously
// Exactly ONE transfer executes
// All 50 responses reference the same transferId
```

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/wallet_db` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `wallet_user` | Database username |
| `DB_PASSWORD` | `wallet_pass` | Database password |
| `SPRING_PROFILES_ACTIVE` | — | Set to `prod` for JSON logging |

---

## Author

**Kadir Hussain**  
Senior Associate Consultant — Java Backend Developer  
📧 mdkadirhussain0786@gmail.com  
🔗 [LinkedIn](https://linkedin.com/in/kadirhussain) | [GitHub](https://github.com/kadirhussain)
