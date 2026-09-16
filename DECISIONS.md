# Technical Decisions

## 1. Concurrency Control

The application uses database-level pessimistic locking to handle concurrent wallet transactions.

The wallet row is locked using:

`PESSIMISTIC_WRITE`

When a debit transaction is processed, the application first locks the corresponding wallet row. This ensures that two concurrent transactions cannot read and update the same wallet balance at the same time.

The balance check and balance update are performed inside a single database transaction using:

`@Transactional`

This provides atomicity for the following operations:

1. Lock wallet
2. Check available balance
3. Deduct amount
4. Save transaction record

This prevents the wallet balance from becoming negative during concurrent debit requests.

---

## 2. Idempotency

The `transactionId` is treated as the idempotency key.

A unique database constraint is applied to `transactionId` so that the same transaction cannot be inserted more than once.

Before processing a transaction, the application checks whether the transaction already exists.

If the same transaction is received again, the API returns:

`409 Conflict`

The unique database constraint provides an additional safety layer against race conditions.

Therefore, even if multiple requests with the same transaction ID arrive concurrently, the transaction cannot be successfully recorded more than once.

---

## 3. Why Database Locking Was Used

A JVM-level `synchronized` lock was considered as a simple concurrency solution.

However, this approach is suboptimal because:

- It only protects requests inside the same application instance.
- It does not provide database-level protection.
- It becomes unreliable when multiple application instances are running.
- It can unnecessarily serialize unrelated wallet transactions.

Instead, database-level pessimistic locking was implemented.

The lock is applied to the specific wallet row, so transactions for different wallets can still execute independently.

---

## 4. AI Suggestion and Improvement

An initial/simple AI-generated approach for concurrency control was to use a Java-level `synchronized` method or in-memory locking.

Example:

```java
public synchronized void processTransaction(...) {
    // check balance
    // deduct amount
}