# Idempotent Payment / Wallet Event Processor

A Spring Boot based Wallet Event Processor that safely processes debit transactions with **idempotency, concurrency control, transaction management, and insufficient-funds protection**.

##  Features

- Process wallet debit transactions
- Idempotent transaction processing
- Prevent duplicate transaction processing
- Pessimistic database row locking
- Thread-safe concurrent wallet debits
- Prevent negative wallet balance
- Insufficient funds handling
- Global exception handling
- H2 in-memory database
- REST API
- JUnit 5 concurrency tests

---

## ️ Tech Stack

- Java 17+
- Spring Boot 3.5.5
- Spring Web
- Spring Data JPA
- Hibernate
- H2 Database
- Maven
- JUnit 5
- Mockito

---

##  Project Structure

```text
wallet-event-processor
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.example.wallet
│   │   │       ├── controller
│   │   │       ├── dto
│   │   │       ├── entity
│   │   │       ├── exception
│   │   │       ├── repository
│   │   │       ├── service
│   │   │       └── WalletEventProcessorApplication.java
│   │   │
│   │   └── resources
│   │       └── application.properties
│   │
│   └── test
│       └── java
│           └── com.example.wallet
│               └── TransactionServiceTest.java
│
├── DECISIONS.md
├── pom.xml
├── mvnw
└── mvnw.cmd

 API Endpoint
Process Transaction

POST

/api/v1/transactions/process
Request
{
  "transactionId": "11111111-1111-1111-1111-111111111111",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 100.00,
  "type": "DEBIT"
}
Successful Response
HTTP 200 OK

Example:

{
  "transactionId": "11111111-1111-1111-1111-111111111111",
  "status": "SUCCESS"
}
 Idempotency

The system uses transactionId as a unique identifier.

If the same transaction is submitted multiple times:

Request 1 → SUCCESS → ₹100 deducted
Request 2 → CONFLICT → No deduction
Request 3 → CONFLICT → No deduction

Therefore, the wallet balance is never deducted twice for the same transaction.

 Concurrency Control

The application uses Pessimistic Database Row Locking.

@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Wallet> findById(UUID id);

This ensures that when multiple requests try to debit the same wallet simultaneously, only one transaction can modify the wallet row at a time.

Example

Initial balance:

₹500

10 concurrent requests:

₹100 × 10 requests

Expected result:

5 requests  → SUCCESS
5 requests  → INSUFFICIENT_FUNDS
Final balance → ₹0

The balance can never become negative.

 Insufficient Funds

Before processing a debit:

amount <= wallet balance

If the wallet does not have sufficient balance, the transaction fails with:

HTTP 409 Conflict

The wallet balance remains unchanged.

 Testing

The project includes JUnit 5 tests covering:

1. Happy Path
Initial Balance: ₹500
Debit: ₹100
Final Balance: ₹400
2. Idempotency Test

Three concurrent requests use the same transactionId.

Expected:

1 SUCCESS
2 CONFLICT

Balance is deducted only once.

3. Race Condition Test

Ten concurrent requests attempt:

₹100 × 10

against a wallet containing:

₹500

Expected:

5 SUCCESS
5 INSUFFICIENT_FUNDS
Final Balance: ₹0
▶️ How to Run
Clone Repository
git clone https://github.com/gopal-karhale15/wallet-event-processor.git
Navigate to Project
cd wallet-event-processor
Run Application

Windows:

.\mvnw.cmd spring-boot:run

Linux / macOS:

./mvnw spring-boot:run

Application starts on:

http://localhost:8080
 Run Tests

Windows:

.\mvnw.cmd test

Run only transaction tests:

.\mvnw.cmd -Dtest=TransactionServiceTest test
️ Database

The project uses an H2 in-memory database.

Database URL:

jdbc:h2:mem:walletdb

H2 Console:

http://localhost:8080/h2-console

JDBC URL:

jdbc:h2:mem:walletdb

Username:

sa

Password:

Architecture
Client
  │
  ▼
TransactionController
  │
  ▼
TransactionService
  │
  ├── Check Transaction ID
  │
  ├── Lock Wallet Row
  │
  ├── Check Balance
  │
  ├── Deduct Amount
  │
  └── Save Transaction
  │
  ▼
H2 Database
 Transaction Flow
POST Request
     │
     ▼
Validate Request
     │
     ▼
Acquire Wallet Row Lock
     │
     ▼
Check Duplicate Transaction
     │
     ├── Duplicate → Conflict
     │
     ▼
Check Wallet Balance
     │
     ├── Insufficient → Error
     │
     ▼
Deduct Amount
     │
     ▼
Save Transaction
     │
     ▼
COMMIT
     │
     ▼
SUCCESS
 Design Decisions

Detailed concurrency and implementation decisions are documented in:

DECISIONS.md

The document explains:

Why database-level locking was selected
Why JVM-level synchronization is not suitable
Transaction atomicity
Idempotency strategy
Race condition handling
Testing strategy
 Author

Gopal Karhale

Java Backend Developer | Spring Boot | SQL | REST API

 License

This project was created as part of a backend development assignment.