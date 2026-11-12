# LedgerLine – Secure Digital Ledger & Money Transfer System
**Submission for Snovasys Fresher Hiring Challenge 2026**

---

## 🏛️ Executive Summary

**LedgerLine** is a modern, enterprise-grade banking and digital ledger web application engineered with strict financial consistency guarantees. It prevents **money loss, duplicate transactions, unauthorized transfers, and race-condition balance corruptions**.

Built with **Java 21, Spring Boot 3.3, Spring Security 6, Spring Data JPA, MySQL 8 (InnoDB)**, and a responsive **React.js (Vite) + Tailwind CSS** frontend.

---

## 👥 Three Specialized Roles

| Role | Permissions & Guardrails |
| :--- | :--- |
| **Customer** | Manages personal bank accounts, views ledger passbook statements, executes instant deposits, withdrawals, and account-to-account transfers. Row-level ownership security ensures customers cannot view or debit other users' accounts. |
| **Admin** | Oversees all users and accounts, monitors bank-wide metrics, reviews system transactions, and possesses administrative power to **Freeze / Unfreeze** suspicious accounts or suspend compromised users. |
| **Auditor** | **Strictly Read-Only**. Inspects immutable security audit trails, double-entry ledger entries, and triggers the automated **Bank Balance Reconciliation Engine** to mathematically verify that $\text{Deposits} - \text{Withdrawals} == \sum \text{Balances}$. |

---

## 🛡️ Core Financial Safeguards (Evaluation Criteria)

### 1. Concurrency Control & Deadlock Prevention
* **Pessimistic Locking (`SELECT ... FOR UPDATE`):** When balance mutations occur, accounts are locked at the database row level using `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
* **Deadlock-Free ID Ordering:** If Account A transfers to Account B while Account B simultaneously transfers to Account A, accounts are locked in consistent numerical order (`min(idA, idB)` first, then `max(idA, idB)`). This completely eliminates database deadlocks.
* **Optimistic Safeguard:** Account entity includes `@Version` to catch any out-of-band updates.

### 2. Append-Only Double-Entry Bookkeeping
* Rather than simply mutating a mutable `balance` column, every transaction creates immutable, append-only **`LedgerEntry`** records (`@Immutable`).
* Every transfer writes a **`DEBIT`** entry for the sender and a **`CREDIT`** entry for the receiver, with real-time snapshots of `balance_after`.
* Total money in the system is mathematically conserved.

### 3. Replay & Idempotency Protection
* Client requests pass a unique `Idempotency-Key` (UUID).
* If a network retry occurs or the user double-clicks "Send Money", the second request matches the completed idempotency record and returns the cached response **without deducting funds twice**.

### 4. Zero Rounding Errors
* Financial precision uses `BigDecimal` in Java and `DECIMAL(15,2)` in MySQL. No IEEE 754 floating-point inaccuracies.

---

## 🔑 Evaluator Demo Credentials (Pre-Seeded)

The application automatically seeds standard evaluation accounts upon first boot:

| Role | Username | Password | Default Balance / Details |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `Admin@123` | Can freeze/unfreeze accounts & view system stats |
| **Auditor** | `auditor` | `Auditor@123` | Read-only ledger, audit trail & bank reconciliation |
| **Customer 1** | `john_doe` | `Customer@123` | Active Savings Account with **$1000.00** |
| **Customer 2** | `jane_smith` | `Customer@123` | Active Checking Account with **$500.00** |

> **Pro-Tip for Evaluators:** The frontend includes **1-Click Demo Login Pills** on the sign-in screen, so you can test all roles instantly without typing credentials!

---

## 🚀 Quickstart & Setup Guide

### Prerequisites
* Java 21 JDK
* Node.js (v18+) & npm
* MySQL 8.0 running on `localhost:3306`

### 1. Database Configuration
Ensure MySQL is running with database `ledgerline_db`:
```sql
CREATE DATABASE IF NOT EXISTS ledgerline_db;
```
Configure your credentials in `backend/src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ledgerline_db?createDatabaseIfNotExist=true
    username: root
    password: password
```

### 2. Run Backend (Spring Boot 3.3)
Open a terminal in `backend/`:
```bash
cd backend
mvn spring-boot:run
```
*Backend runs on: `http://localhost:8080`*

### 3. Run Frontend (React Vite + Tailwind)
Open another terminal in `frontend/`:
```bash
cd frontend
npm install
npm run dev
```
*Frontend opens at: `http://localhost:5173`*

---

## 🧪 Automated Concurrency & Idempotency Tests

Run the backend integration test suite:
```bash
cd backend
mvn test
```
* **`TransactionConcurrencyTest.java`**: Simulates 10 concurrent threads transferring money simultaneously from the same account. Verifies zero balance drift and exact conservation of total system funds.
* **`IdempotencySafetyTest.java`**: Tests idempotency key replay, verifies that frozen accounts are blocked from transacting, and checks overdraft protection.

---

## 📡 REST API Cheat Sheet

### Authentication (`/api/auth`)
* `POST /api/auth/register` – Register new user & auto-provision customer bank account.
* `POST /api/auth/login` – Authenticate with username or email, returns signed JWT token.
* `GET /api/auth/me` – Retrieve profile and linked account balances for current user.

### Transactions (`/api/transactions`)
* `POST /api/transactions/transfer` – Atomic transfer (Headers: `Idempotency-Key: UUID`).
* `POST /api/transactions/deposit` – Deposit money into account.
* `POST /api/transactions/withdraw` – Withdraw money from account.
* `GET /api/transactions/history/{accountNumber}` – Paginated transaction history.
* `GET /api/transactions/statement/{accountNumber}` – Passbook ledger statement with running balance snapshots.

### Admin Panel (`/api/admin`) – *Requires ROLE_ADMIN*
* `GET /api/admin/users` – View all users & accounts.
* `PATCH /api/admin/accounts/{accountNumber}/status` – Freeze, Unfreeze, or Close an account.
* `GET /api/admin/stats` – Real-time system deposits, reserves, and velocity.
* `GET /api/admin/transactions` – Bank-wide transaction logs.

### Audit Center (`/api/audit`) – *Requires ROLE_AUDITOR or ROLE_ADMIN*
* `GET /api/audit/logs` – Immutable security & administrative audit logs.
* `GET /api/audit/ledger` – Complete double-entry ledger lines.
* `GET /api/audit/reconciliation` – Live mathematical reconciliation report.
