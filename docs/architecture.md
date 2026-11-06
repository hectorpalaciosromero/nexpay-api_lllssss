# NexPay API - Architecture Documentation

## Overview

NexPay is a payment API built with Spring Boot microservices, implementing
enterprise patterns for secure, resilient, and observable financial transactions.

## Architecture Style

Hexagonal Architecture (Ports & Adapters) across all services, with event-driven
communication between services and CQRS for payment history.

```
+-------------------------------------------------------------+
|                        API Gateway                           |
|              Spring Cloud Gateway + Rate Limiting            |
+----------+------------------+------------------+-------------+
           |                  |                  |
           v                  v                  v
+------------------+ +------------------+ +------------------+
|  Wallet Service  | | Payment Service  | |  Notification    |
|                  | |                  | |  Service         |
|  - CRUD Wallets  | |  - P2P Transfers | |  - SQS Consumer  |
|  - Balance Mgmt  | |  - Saga Pattern  | |  - DLQ Handler   |
|  - Movements     | |  - Idempotency   | |  - SNS Fanout    |
|                  | |  - Circuit Break | |                  |
|  [PostgreSQL]    | |  [PostgreSQL]    | |  [In-Memory]     |
|                  | |  [DynamoDB]      | |                  |
|                  | |  [S3]            | |                  |
+------------------+ +--------+---------+ +--------^---------+
                              |                    |
                              |    +-----+         |
                              +--->| SQS |---------+
                                   +-----+
```

## Service Communication

| From | To | Method | Why |
|---|---|---|---|
| Payment -> Wallet | Synchronous HTTP | Debit/Credit require immediate consistency |
| Payment -> Notification | Async via SQS | Notifications are fire-and-forget |
| Payment -> DynamoDB | Async via SQS | History is eventually consistent |

## Hexagonal Architecture (Per Service)

```
+-------------------------------------------------------+
|                    API Layer                            |
|              (REST Controllers)                        |
|                      |                                 |
|              +-------v--------+                        |
|              |  Application   |                        |
|              |  (Use Cases)   |                        |
|              |  (DTOs)        |                        |
|              +-------+--------+                        |
|                      |                                 |
|              +-------v--------+                        |
|              |    Domain      |  <-- Pure business     |
|              |  (Entities)    |      logic, no         |
|              |  (Value Objs)  |      frameworks        |
|              |  (Ports)       |                        |
|              +-------+--------+                        |
|                      |                                 |
|              +-------v--------+                        |
|              | Infrastructure |                        |
|              |  (JPA Adapter) |                        |
|              |  (AWS Adapter) |                        |
|              |  (HTTP Client) |                        |
|              +----------------+                        |
+-------------------------------------------------------+
```

Key rule: Domain layer has zero framework dependencies. It defines ports (interfaces)
that the infrastructure layer implements as adapters.

## Saga Pattern (Choreography)

The payment flow follows a choreography-based saga:

```
1. [Payment Service] Create payment (PENDING)
2. [Payment Service] Debit source wallet (via HTTP to Wallet Service)
   - If fails: Mark payment FAILED, STOP
3. [Payment Service] Credit target wallet (via HTTP to Wallet Service)
   - If fails: Compensate (re-credit source wallet), mark FAILED
4. [Payment Service] Mark payment COMPLETED
5. [Payment Service] Publish event to SQS
6. [Notification Service] Consume event, send notification
7. [Payment Service] Write to DynamoDB (history)
```

## Resilience Patterns

| Pattern | Implementation | Where |
|---|---|---|
| Circuit Breaker | Resilience4j | Payment -> Wallet calls |
| Retry | Resilience4j with exponential backoff | Payment -> Wallet calls |
| Idempotency | `X-Idempotency-Key` header + DB unique constraint | Payment creation |
| Dead Letter Queue | SQS DLQ | Notification processing |
| Rate Limiting | Spring Cloud Gateway filter | API Gateway |
| Timeout | Resilience4j TimeLimiter | All HTTP calls |

## Observability Stack

```
+-----------------+    +-----------------+    +-----------------+
|   Prometheus    |--->|    Grafana      |    |    Zipkin       |
|   (Metrics)     |    |  (Dashboards)  |    |  (Tracing)      |
+-----------------+    +-----------------+    +-----------------+
        ^                                             ^
        |                                             |
+-------+---------------------------------------------+---+
|              Spring Boot Actuator + Micrometer          |
|              (All 3 microservices)                       |
+---------------------------------------------------------+
```

### Dashboards
1. Business Metrics - Payments/min, success rate, average amount
2. System Health - JVM memory, CPU, HTTP latency, DB connections
3. Error Tracking - Circuit breaker state, DLQ depth, 5xx errors

## AWS Services (via LocalStack)

| Service | Usage |
|---|---|
| SQS | Event queue between Payment and Notification |
| SNS | Notification fan-out (email, push) |
| DynamoDB | Payment history (CQRS read model) |
| S3 | Payment receipt PDF storage |
| Secrets Manager | Database credentials |

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| API Gateway | Spring Cloud Gateway |
| Database | PostgreSQL 15 |
| NoSQL | DynamoDB (LocalStack) |
| Messaging | SQS + SNS (LocalStack) |
| Storage | S3 (LocalStack) |
| Resilience | Resilience4j |
| Metrics | Micrometer + Prometheus |
| Tracing | Zipkin + Micrometer Tracing |
| Logging | ELK Stack |
| Migration | Flyway |
| Testing | JUnit 5 + Mockito + H2 |
| Containers | Docker + Docker Compose |
| CI/CD | GitHub Actions |
| API Docs | Swagger / OpenAPI 3.0 |
