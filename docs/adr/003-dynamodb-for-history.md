# ADR 003: DynamoDB for Payment History

## Status
Accepted

## Context
Payment history is a read-heavy, append-only workload. Users query their transaction
history frequently, but records are never updated once created. The payment processing
itself uses PostgreSQL for ACID guarantees, but the historical query pattern is different:

- High read volume: Users check history more than they make payments
- No complex joins: History queries are simple (by user, by date range)
- Growing data: Transaction history grows indefinitely
- No updates: Once a payment is recorded, the history entry is immutable

## Decision
Use Amazon DynamoDB (via LocalStack) for payment history storage while keeping
PostgreSQL for the transactional payment processing.

### Partition Key Design
```
PK: USER#{userId}
SK: PAYMENT#{timestamp}#{paymentId}
```

This allows efficient queries:
- Get all payments for a user (Query on PK)
- Get payments in a date range (Query on PK + SK range)
- Get a specific payment (GetItem with PK + SK)

### Write Pattern (CQRS)
```
1. Payment created in PostgreSQL (source of truth)
2. Event published to SQS
3. Consumer writes denormalized record to DynamoDB
4. History reads go directly to DynamoDB
```

## Consequences

### Positive
- Predictable performance at any scale, single-digit millisecond reads
- No impact on PostgreSQL, history reads don't compete with payment writes
- Cost-effective, DynamoDB scales independently from the relational DB
- Clear separation of command (PostgreSQL) and query (DynamoDB) models
- Eventually consistent, which is acceptable for history

### Negative
- Eventual consistency: small delay between payment completion and history visibility
- Added complexity: two data stores to manage
- Data duplication: payment data exists in both PostgreSQL and DynamoDB
- Learning curve: team must understand DynamoDB access patterns

### Mitigations
- SQS guarantees at-least-once delivery, DLQ catches failures
- Idempotency key prevents duplicate history entries
- Reconciliation job can be added later if needed

## Alternatives Considered

| Alternative | Why Rejected |
|---|---|
| PostgreSQL only | Read/write contention at scale, requires read replicas |
| Elasticsearch | Over-engineered for simple key-value lookups |
| Redis | Not durable enough for permanent history |
| MongoDB | Similar benefits to DynamoDB but less AWS integration |
