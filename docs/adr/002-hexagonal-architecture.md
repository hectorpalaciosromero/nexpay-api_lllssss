# ADR-002: Hexagonal Architecture

## Status
Accepted

## Date
2026-10-21

## Context
Each microservice needs a clear internal structure that:
- Separates business logic from infrastructure concerns
- Makes the codebase testable without Spring context
- Allows swapping databases, message brokers, or HTTP clients without changing domain logic

### Options Considered
1. Layered Architecture - Traditional Controller -> Service -> Repository
2. Hexagonal Architecture (Ports & Adapters) - Domain-centric with clear boundaries
3. Clean Architecture - Similar to hexagonal but with more layers

## Decision
We chose Hexagonal Architecture because:

```
                    +-------------------------+
                    |       Domain Layer       |
   REST API ------> |  +------------------+   |
   (Adapter)        |  |  Business Logic  |   | ------> PostgreSQL
                    |  |  (Pure Java)     |   |         (Adapter)
   SQS Consumer --> |  +------------------+   |
   (Adapter)        |                         | ------> SQS/SNS
                    |    Ports (Interfaces)    |         (Adapter)
                    +-------------------------+
```

### Package Structure
```
com.nexpay.wallet/
├── domain/          <- Pure business logic, NO framework annotations
│   ├── model/       <- Wallet, Movement (plain Java classes)
│   ├── port/in/     <- Use case interfaces (what the app can do)
│   ├── port/out/    <- Repository interfaces (what the app needs)
│   └── exception/   <- Domain-specific exceptions
├── application/     <- Use case implementations, orchestration
│   ├── service/     <- WalletServiceImpl (implements port/in)
│   └── dto/         <- Request/Response objects
├── infrastructure/  <- Framework-dependent adapters
│   ├── persistence/ <- JPA entities, Spring Data repos, adapters
│   ├── config/      <- Spring configuration
│   └── client/      <- External service clients
└── api/             <- REST controllers (driving adapter)
```

## Consequences

### Positive
- Domain model is 100% testable without Spring
- Database can be swapped (PostgreSQL to DynamoDB) by changing only the adapter
- Clear dependency rule: domain depends on nothing external

### Negative
- More boilerplate (entity to domain mapping)
- Steeper learning curve for junior developers
- Overkill for simple CRUD services (but worth it for the project)
