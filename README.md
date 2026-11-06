# NexPay API

Payment microservices platform built with Java 21, Spring Boot 3, AWS (LocalStack), and full observability.

[![CI](https://github.com/hectorpalaciosromero/nexpay-api/actions/workflows/ci.yml/badge.svg)](https://github.com/hectorpalaciosromero/nexpay-api/actions)
[![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot 3.2](https://img.shields.io/badge/Spring%20Boot-3.2-green?logo=springboot)](https://spring.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue?logo=docker)](https://docs.docker.com/)

---

## Overview

NexPay is a backend payment API built with fintech microservices patterns:

- 3 microservices with Hexagonal Architecture
- Saga Pattern for distributed transactions
- Event-driven communication via AWS SQS
- CQRS with PostgreSQL + DynamoDB
- Observability with Prometheus, Grafana, and Zipkin
- Runs 100% locally with LocalStack (zero AWS cost)

---

## Architecture

```
+-------------------------------------------------------------+
|                      API Gateway                             |
|              Spring Cloud Gateway + Rate Limiting            |
+----------+------------------+------------------+-------------+
           |                  |                  |
           v                  v                  v
+------------------+ +------------------+ +------------------+
|  Wallet          | |  Payment         | |  Notification    |
|  Service         | |  Service         | |  Service         |
|                  | |                  | |                  |
|  - CRUD Wallets  | |  - P2P Transfers | |  - SQS Consumer  |
|  - Balance Mgmt  | |  - Saga Pattern  | |  - DLQ Handler   |
|  - Movements     | |  - Idempotency   | |  - SNS Fanout    |
|                  | |  - Circuit Break | |                  |
|  [PostgreSQL]    | |  [PostgreSQL]    | |  [In-Memory]     |
|                  | |  [DynamoDB]      | |                  |
|                  | |  [S3]            | |                  |
+------------------+ +--------+---------+ +--------^---------+
                              |    +-----+         |
                              +--->| SQS |---------+
                                   +-----+
```

### Payment Flow (Saga)

```
1. POST /api/v1/payments         -> Create payment (PENDING)
2. Debit source wallet           -> HTTP call to wallet-service
3. Credit target wallet          -> HTTP call to wallet-service
4. Mark payment COMPLETED        -> Update PostgreSQL
5. Publish event to SQS          -> Async notification
6. notification-service consumes -> Send confirmation
```

If step 3 fails, compensation reverses step 2 automatically.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| API Gateway | Spring Cloud Gateway |
| Database | PostgreSQL 15 |
| NoSQL | DynamoDB (LocalStack) |
| Messaging | SQS + SNS (LocalStack) |
| Object Storage | S3 (LocalStack) |
| Resilience | Resilience4j (Circuit Breaker, Retry, Rate Limiter) |
| Metrics | Micrometer + Prometheus |
| Tracing | Zipkin + Micrometer Tracing |
| Logging | ELK Stack (Elasticsearch + Logstash + Kibana) |
| Migration | Flyway |
| Testing | JUnit 5 + Mockito + H2 |
| Containers | Docker Compose |
| CI/CD | GitHub Actions |
| API Docs | Swagger / OpenAPI 3.0 |

---

## Patterns Implemented

| Pattern | Where |
|---|---|
| Hexagonal Architecture | All services |
| Saga Pattern (Choreography) | payment-service |
| Circuit Breaker | payment -> wallet |
| Idempotency Key | POST /payments |
| Event-Driven | SQS between services |
| CQRS (light) | PostgreSQL / DynamoDB |
| Dead Letter Queue | SQS DLQ |
| API Gateway | Spring Cloud Gateway |
| Distributed Tracing | Zipkin + trace IDs |
| 12-Factor App | Externalized config |

---

## Quick Start

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### Run Everything

```bash
git clone https://github.com/hectorpalaciosromero/nexpay-api.git
cd nexpay-api

# Start all services + infrastructure
make up

# Or manually:
docker compose up -d
```

### Verify Services

| Service | URL | Purpose |
|---|---|---|
| API Gateway | http://localhost:8080 | Entry point |
| Wallet Service | http://localhost:8081 | Direct access |
| Payment Service | http://localhost:8082 | Direct access |
| Notification Service | http://localhost:8083 | Direct access |
| Swagger UI (Wallet) | http://localhost:8081/swagger-ui.html | API Docs |
| Swagger UI (Payment) | http://localhost:8082/swagger-ui.html | API Docs |
| Grafana | http://localhost:3000 | Dashboards (admin/admin) |
| Prometheus | http://localhost:9090 | Raw metrics |
| Zipkin | http://localhost:9411 | Distributed tracing |
| Kibana | http://localhost:5601 | Log search |
| LocalStack | http://localhost:4566 | AWS emulation |

### Quick Demo

```bash
# 1. Create Alice's wallet
curl -X POST http://localhost:8080/api/v1/wallets \
  -H "Content-Type: application/json" \
  -d '{"ownerName": "Alice", "ownerEmail": "alice@nexpay.com"}'

# 2. Create Bob's wallet
curl -X POST http://localhost:8080/api/v1/wallets \
  -H "Content-Type: application/json" \
  -d '{"ownerName": "Bob", "ownerEmail": "bob@nexpay.com"}'

# 3. Fund Alice ($1000)
curl -X POST http://localhost:8080/api/v1/wallets/{aliceId}/credit \
  -H "Content-Type: application/json" \
  -d '{"amount": 1000.00, "description": "Initial deposit"}'

# 4. Transfer $250 (Alice -> Bob)
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: $(uuidgen)" \
  -d '{
    "senderWalletId": "{aliceId}",
    "receiverWalletId": "{bobId}",
    "amount": 250.00,
    "description": "Rent payment"
  }'
```

### Makefile Commands

```bash
make up          # Start all containers
make down        # Stop all containers
make restart     # Restart everything
make logs        # Tail all logs
make test        # Run all tests
make clean       # Remove containers + volumes
make build       # Build all services
```

---

## API Documentation

### Wallet Service

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/wallets` | Create new wallet |
| `GET` | `/api/v1/wallets/{id}` | Get wallet details |
| `POST` | `/api/v1/wallets/{id}/credit` | Add funds |
| `POST` | `/api/v1/wallets/{id}/debit` | Remove funds |
| `PATCH` | `/api/v1/wallets/{id}/status` | Activate/Deactivate |
| `GET` | `/api/v1/wallets/{id}/movements` | Movement history (paginated) |

### Payment Service

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/payments` | Execute P2P transfer (requires `X-Idempotency-Key`) |
| `GET` | `/api/v1/payments/{id}` | Get payment details |
| `POST` | `/api/v1/payments/{id}/refund` | Refund a completed payment |
| `GET` | `/api/v1/payments/history` | Payment history (DynamoDB) |

Full OpenAPI specs available at `/swagger-ui.html` on each service.

---

## Observability

### Grafana Dashboards (pre-configured)

1. **Business Metrics** - Payments/min, success rate, avg amount, active wallets
2. **System Health** - JVM heap, CPU usage, HTTP latency (p99), DB connections
3. **Error Tracking** - Circuit breaker state, DLQ depth, failed payments, 5xx errors

### Distributed Tracing (Zipkin)

Every request gets a trace-id that propagates across all 3 services. Open Zipkin at `http://localhost:9411` to visualize the complete payment flow.

### Centralized Logging (ELK)

All logs are shipped to Elasticsearch via Logstash. Search by trace-id in Kibana to correlate events across services.

---

## Testing

```bash
# Run all tests (unit + integration with H2 in-memory DB)
mvn test

# Run a specific service
mvn -pl services/wallet-service test
mvn -pl services/payment-service test

# Run with coverage report
mvn test jacoco:report -B
# Reports at: services/*/target/site/jacoco/index.html
```

### Test Strategy

| Type | Framework | What it tests |
|---|---|---|
| Unit Tests | JUnit 5 + Mockito | Domain logic, service layer, state machines |
| Integration Tests | Spring Boot Test + H2 (PostgreSQL mode) | Full API flow with MockMvc |
| Contract Tests | OpenAPI specs | API contract validation |

> Integration tests use an H2 in-memory database in PostgreSQL compatibility mode, so they run in **any environment** (CI, local) without Docker. The schema is loaded via `@Sql` from `schema-test.sql`.

---

## Project Structure

```
nexpay-api/
├── services/
│   ├── api-gateway/
│   ├── wallet-service/
│   ├── payment-service/
│   └── notification-service/
├── infrastructure/
│   ├── grafana/
│   ├── prometheus/
│   ├── localstack/
│   ├── logstash/
│   └── k8s/
├── docs/
│   ├── architecture.md
│   ├── api-contracts/
│   ├── adr/
│   └── postman/
├── .github/workflows/
├── compose.yml
├── Makefile
└── pom.xml (parent)
```

### Hexagonal Architecture (per service)

```
src/main/java/com/nexpay/{service}/
├── domain/
│   ├── model/
│   ├── port/
│   │   ├── in/
│   │   └── out/
│   └── exception/
├── application/
│   ├── service/
│   └── dto/
├── infrastructure/
│   ├── persistence/
│   ├── messaging/
│   ├── client/
│   └── config/
└── api/
    ├── controller/
    └── advice/
```

---

## Architecture Decision Records

| ADR | Decision | Status |
|---|---|---|
| [001](docs/adr/001-saga-pattern.md) | Saga Pattern (Choreography) for distributed transactions | Accepted |
| [002](docs/adr/002-hexagonal-architecture.md) | Hexagonal Architecture for all services | Accepted |
| [003](docs/adr/003-dynamodb-for-history.md) | DynamoDB for payment history (CQRS) | Accepted |

---

## Author

**Hector Palacios Romero** — Java Backend Developer

## License

MIT License. See [LICENSE](LICENSE) for details.
