# ADR-001: Saga Pattern for Distributed Transactions

## Status
Accepted

## Date
2026-10-21

## Context
NexPay processes P2P payments that involve multiple microservices:
1. `payment-service` - orchestrates the payment
2. `wallet-service` - manages wallet balances (debit sender, credit receiver)

In a monolith, we would use a single database transaction. In a microservices architecture,
each service has its own database, making traditional ACID transactions impossible.

### Options Considered
1. Two-Phase Commit (2PC) - Strong consistency but high latency, poor scalability, tight coupling
2. Saga Pattern (Orchestration) - Central coordinator manages the saga steps
3. Saga Pattern (Choreography) - Services communicate via events, no central coordinator

## Decision
We chose choreography-based Saga using AWS SQS for the following reasons:

- Decoupling: Services don't need to know about each other
- Resilience: If a step fails, compensation events are published
- Scalability: Each service processes events at its own pace
- AWS-native: SQS provides reliable message delivery with DLQ support

### Saga Flow
```
1. payment-service: Create payment (PENDING)
2. payment-service: Call wallet-service to DEBIT sender -> (PROCESSING)
3. payment-service: Call wallet-service to CREDIT receiver
4. On success: Mark COMPLETED, publish PAYMENT_COMPLETED to SQS
5. On credit failure: COMPENSATE, refund sender, mark FAILED, publish PAYMENT_FAILED
```

## Consequences

### Positive
- Services remain loosely coupled
- Natural fit with event-driven architecture
- Failed messages go to DLQ for investigation
- Idempotency keys prevent duplicate payments

### Negative
- Eventual consistency (not immediate)
- More complex debugging (need distributed tracing)
- Compensation logic adds complexity

### Mitigations
- Zipkin for distributed tracing across services
- Idempotency keys to handle retries safely
- Dead Letter Queue for messages that fail after 3 retries
- Circuit Breaker to fail fast when wallet-service is down
