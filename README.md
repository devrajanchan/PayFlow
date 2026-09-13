# PayFlow

PayFlow is an educational, event-driven payment processing and reconciliation platform. It models the engineering concerns behind commercial payments: authenticated intake, idempotency, asynchronous processing, immutable ledger entries, reconciliation, resilience, and operational visibility.

This repository uses synthetic data only. It must never contain JPMorgan code, customer data, credentials, tokens, or internal architecture.

## Current checkpoint

The repository currently contains:

   - A Java 25 Maven multi-module build.
- The initial `payment-intake` Spring Boot service.
- Local PostgreSQL and Kafka infrastructure definitions.
- Actuator health, metrics, and Kubernetes probe endpoints.
- A PostgreSQL-backed `POST /payments` intake API.
- Flyway-managed schema migration for the `payments` table.
- Validation for payment amount and ISO-style three-letter currency codes.
- Idempotency protection using `Client-Id` and `Idempotency-Key`.
- A unit test proving that a retry does not create a second payment.

Kafka is configured locally but is not yet used by the application. The next slice will publish an accepted payment event.

## What We Have Built

### Business capability

The intake service accepts a payment instruction and records it as `RECEIVED`. It does not move money and it does not claim that settlement has happened. This distinction is important: intake records the customer's instruction, while later processing and reconciliation determine what happened operationally.

The current flow is:

```text
HTTP client
      |
      | POST /payments
      | Client-Id + Idempotency-Key
      v
PaymentController
      |
      v
PaymentService
      |
      | find by (client_id, idempotency_key)
      | create only when absent
      v
PaymentRepository -> PostgreSQL payments table
```

### Why idempotency matters

Payment clients retry when they receive a timeout or a lost network response. The server may have committed the first request even though the client did not receive its response. Without idempotency, the retry could create two instructions and potentially debit an account twice.

The client supplies an idempotency key for one logical payment attempt. PayFlow scopes that key by client, so two clients may independently use the same key without colliding:

```text
UNIQUE (client_id, idempotency_key)
```

The application checks for an existing record first. The database constraint is still required because two identical requests can arrive concurrently and both pass the application lookup before either inserts. The database is the final correctness boundary.

### Why PostgreSQL and Flyway

Payments need durable, transactional records with constraints that protect financial invariants. PostgreSQL provides ACID transactions, unique constraints, numeric precision, indexes, and reliable recovery behavior.

Flyway keeps schema changes versioned and repeatable. `ddl-auto: validate` means Hibernate checks that the Java mapping agrees with the schema but does not silently alter a financial database. Schema changes must be explicit migrations reviewed like code.

The amount uses `NUMERIC(19, 4)` rather than floating point. Binary floating-point types can represent decimal currency values imprecisely; a database decimal type preserves the intended monetary precision.

### Why the status is `RECEIVED`

The status represents the lifecycle stage, not a vague success flag. `RECEIVED` means the intake boundary accepted and persisted the instruction. Later states will be introduced as separate processing capabilities, such as `ACCEPTED`, `PROCESSING`, `COMPLETED`, `FAILED`, and reconciliation outcomes.

We should not mark a payment `COMPLETED` in this service because intake has not yet performed ledger processing or settlement.

### Code responsibilities

| Component | Responsibility |
|---|---|
| `PaymentController` | HTTP contract, headers, validation trigger, `201 Created` response |
| `PaymentRequest` | Input shape and field constraints |
| `PaymentService` | Idempotent creation use case and transaction boundary |
| `Payment` | JPA persistence model and initial lifecycle state |
| `PaymentRepository` | Database access and idempotency lookup |
| `V1__create_payments.sql` | Explicit database schema, constraints, and index |
| `PaymentServiceTest` | Regression test for duplicate retry behavior |

## How To Exercise The Current Slice

Start PostgreSQL after Docker Desktop is running:

```bash
docker compose -f infra/docker-compose.yml up -d postgres
docker compose -f infra/docker-compose.yml ps
```

Start the service:

```bash
./mvnw -pl services/payment-intake spring-boot:run
```

Submit a payment:

```bash
curl -i -X POST http://localhost:8081/payments \
   -H 'Content-Type: application/json' \
   -H 'Client-Id: demo-client' \
   -H 'Idempotency-Key: payment-001' \
   -d '{
      "sourceAccountId": "account-001",
      "destinationAccountId": "vendor-001",
      "amount": 125.50,
      "currency": "USD"
   }'
```

Repeat the exact request. The response should contain the same `paymentId`, demonstrating idempotent retry handling.

Invalid examples should be rejected before persistence:

- `amount` less than `0.01`
- blank account IDs
- currency not matching three uppercase letters
- missing `Client-Id`
- missing `Idempotency-Key`

Run the focused verification with:

```bash
./mvnw -pl services/payment-intake clean verify
```

## Current Limitations And Deliberate Trade-offs

- Authentication is not implemented yet; `Client-Id` is currently an identifying header, not proof of identity. JWT resource-server security comes in a later phase.
- The application lookup plus unique constraint protects duplicate creation, but the HTTP response does not yet distinguish a first submission from an idempotent replay. We can add that contract deliberately later.
- There is no outbox yet. Publishing an event after the database commit will be the next consistency problem to solve with Kafka and an outbox design.
- There is no account-balance or double-entry ledger yet. `RECEIVED` is only an instruction record.
- The current test is a unit test. A Testcontainers integration test should verify Flyway and real PostgreSQL behavior once Docker is consistently available.
- Kafka is present in local infrastructure but no event is published yet.

These limitations are intentional. Each one creates a clear next engineering problem rather than hiding complexity inside the first endpoint.

## 30-Second Interview Answer

> I started PayFlow with the payment-intake boundary because payment correctness begins before asynchronous processing. The service validates a payment instruction, persists it in PostgreSQL through an explicit Flyway migration, and requires a client-scoped idempotency key. A retry after a timeout looks up the original payment instead of creating another one, while a database unique constraint protects against concurrent duplicate requests. The service only records `RECEIVED`; it does not claim settlement. The next step is publishing an event to Kafka while addressing the database-to-message consistency problem.

## Interview Questions And Answers

### Why is idempotency necessary for a payment API?

Because clients retry when responses are lost or delayed. The server may have completed the first request even when the client believes it failed. An idempotency key lets the server recognize the retry and return the original result instead of applying the financial operation twice.

### Why is the key scoped by client?

The key belongs to the client namespace. If the uniqueness rule were only `idempotency_key`, unrelated clients could collide accidentally. `(client_id, idempotency_key)` allows each client to generate keys independently while preventing duplicates within that client's request space.

### Why do we need a database unique constraint if the service checks first?

The check-then-insert sequence is vulnerable to a race. Two concurrent requests can both observe no existing row and then both attempt to insert. The unique constraint makes the database enforce the invariant atomically. In a production implementation, the service should also handle a uniqueness violation and resolve it as an idempotent replay.

### Does this provide exactly-once processing?

No. It provides idempotent creation at the intake boundary. Exactly-once behavior across HTTP, a database, Kafka, and external payment systems is not something we should casually claim. The broader platform will use at-least-once delivery with idempotent consumers and durable state transitions.

### Why use `NUMERIC` instead of `double` for money?

Floating-point values can introduce representation errors for decimal amounts. PostgreSQL `NUMERIC(19, 4)` stores exact decimal values at the chosen precision. Java `BigDecimal` keeps the application representation aligned with that choice.

### Why does Hibernate use `ddl-auto: validate`?

Financial schemas should change through reviewed, versioned migrations. Automatically generating or modifying tables at application startup can create uncontrolled production changes. Hibernate validation still detects mapping drift without owning schema evolution.

### Why is `RECEIVED` not `COMPLETED`?

The intake service has only accepted and stored an instruction. It has not validated account balances, created ledger entries, called a settlement rail, or reconciled an external result. Status names should describe verified business facts, not optimistic assumptions.

### Where should authentication happen?

At the API boundary, using a JWT validated by a resource server or an API gateway. The token should establish client identity and permissions; the idempotency key remains a separate request-level correctness mechanism. In later phases we will add authorization such as who may submit payments and who may view operational data.

### What happens if the database is unavailable?

The request should fail without claiming that the payment was accepted. A client may retry with the same idempotency key after the dependency recovers. Timeouts, connection-pool limits, and an appropriate error contract will matter more once external dependencies are introduced.

### What is the next consistency problem?

After storing a payment, the service needs to publish an event for asynchronous processing. If it writes the database and then crashes before publishing, the payment exists but no processor knows about it. If it publishes first and then the database transaction fails, consumers may process a payment that was not persisted. The Kafka phase will address this with a transactional outbox or a carefully justified alternative.

## Kafka Slice

The intake service now publishes a `PaymentAcceptedEvent` to the `payment.accepted` topic after saving a new payment. An idempotent replay returns the existing database row and does not publish a second event.

The event contains the payment ID, client, accounts, amount, currency, and event timestamp. It is a separate event contract rather than the JPA entity, so database implementation details do not become an accidental integration API.

The producer uses:

```text
topic: payment.accepted
key:   paymentId
value: PaymentAcceptedEvent JSON
```

Kafka hashes the key to select a partition. Using `paymentId` means events for one payment stay ordered relative to each other, while different payments can be processed concurrently across the three partitions. This is the right initial ordering scope because a payment's lifecycle must not move from one state to another out of order.

The topic has one local replica because this is a single-broker development environment. That is not a production availability setting; production replication would normally be greater than one and would be chosen with durability and cost requirements in mind.

### Kafka request flow

```text
POST /payments
   |
   v
find idempotency key
   |
   +-- existing payment --> return it, publish nothing
   |
   +-- new payment -------> INSERT payment
                          |
                          v
                   publish payment.accepted
```

### Important limitation: dual write

The current implementation performs two separate operations:

1. Commit the payment row in PostgreSQL.
2. Send the Kafka event.

That creates a failure window. If PostgreSQL commits and the process crashes before Kafka accepts the message, the payment remains `RECEIVED` but downstream processing never hears about it. If Kafka accepts the message and the database transaction later rolls back, a consumer could process an instruction that does not exist in the source database.

This is deliberately visible in the project because it is a valuable distributed-systems interview discussion. The next reliability improvement is a transactional outbox: write the payment and an unpublished event record in the same PostgreSQL transaction, then publish outbox records with a retryable worker and mark them published.

### Kafka interview questions

#### Why use Kafka instead of a synchronous REST call?

Payment intake should acknowledge a valid instruction without waiting for every downstream system. Kafka decouples intake from processing, absorbs bursts, supports consumer groups, and allows downstream services to replay events. The trade-off is eventual consistency and more operational complexity.

#### Does Kafka guarantee ordering globally?

No. Kafka guarantees ordering within a partition. By using `paymentId` as the key, all events for one payment go to the same partition. There is no global ordering across unrelated payments, and the system should not require one.

#### Why not use account ID as the key?

Account ID would preserve ordering for all payments affecting one account, which may matter for balance calculations. It can also create hot partitions for high-volume accounts and does not directly express the lifecycle ordering of one payment. We start with `paymentId` and will revisit the key when the ledger consumer and account concurrency rules are implemented.

#### What delivery guarantee does the producer provide?

The current producer is asynchronous and does not yet expose a durable delivery result to the HTTP caller. Kafka can retry producer network failures, but that does not solve the database-to-Kafka dual-write gap. The outbox worker will make publication observable and retryable.

#### What happens when Kafka is unavailable?

The payment database write and Kafka send are not one atomic transaction. Depending on when the failure occurs, the API may fail after the database write or the database may contain an unannounced payment. This is why the current design is not the final reliability design; the outbox is the correct next step.

#### Why does an idempotent retry not publish again?

The service first looks up `(client_id, idempotency_key)`. If it finds a payment, it returns that record directly. Publishing only occurs in the new-payment branch, so the client can safely retry without creating duplicate downstream work.

#### How would a consumer handle duplicate Kafka messages?

Kafka consumers should assume at-least-once delivery. The consumer will store a processed event ID or use a business uniqueness constraint in the same database transaction as its state change. A redelivered event then becomes a no-op rather than a second ledger effect.

#### What would you change before production?

I would add a transactional outbox, explicit producer delivery handling, schema compatibility/versioning, consumer idempotency, retry topics and a dead-letter topic, security between clients and brokers, metrics for publish latency and failures, and integration tests against Kafka.

## Next Reliability Slice: Transactional Outbox

Before adding a processing consumer, we should close the dual-write gap:

1. Add an `outbox_events` table.
2. Save the payment and its `payment.accepted` event in one database transaction.
3. Publish unpublished outbox rows from a scheduled worker.
4. Mark an event published only after Kafka acknowledges it.
5. Make the worker safe to retry.
6. Add a failure test proving an unsent event remains available for retry.

## Setup on macOS

Install these prerequisites:

1. Install a Java 25 JDK, such as Eclipse Temurin 25, and verify:

   ```bash
   java -version
   ```

2. Install Docker Desktop and start it. Verify:

   ```bash
   docker --version
   docker compose version
   ```

3. Git is already available on this machine. Install Maven temporarily to generate the Maven Wrapper; after that, the repository uses the wrapper so the build version stays consistent across machines.

If Homebrew is preferred, install it from https://brew.sh, then use:

```bash
brew install --cask temurin@25
brew install --cask docker
```

After Java and Maven are installed, generate the wrapper once:

```bash
mvn wrapper:wrapper
```

Confirm that `./mvnw -version` reports Java 25 or newer.

Do not install cloud resources yet. Local development comes first.

## First local run

From the repository root:

```bash
cd /Users/devrajanchan/projects/PayFlow
docker compose -f infra/docker-compose.yml up -d
./mvnw clean verify
./mvnw -pl services/payment-intake spring-boot:run
```

In another terminal, check the service:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8081/actuator/health/readiness
```

Stop local infrastructure with:

```bash
docker compose -f infra/docker-compose.yml down
```

Use `down -v` only when you intentionally want to delete the local PostgreSQL volume and all local data.

## Learning rule

For each slice, we will follow this loop:

1. Understand the business invariant and failure mode.
2. Implement the smallest production-shaped version.
3. Run a focused test and inspect the result.
4. Explain the design in a 30-second interview answer.
5. Record the decision and trade-off in this README.

## Cloud cost guardrail

AWS is optional and comes after the local Kubernetes deployment. Before any AWS experiment, configure a billing alarm, confirm free-tier eligibility for the account and region, and destroy resources with Terraform immediately after use. Free-tier eligibility and pricing can change.
