# Test Orchestration Platform

A backend system that receives automated test results from CI pipelines, stores them, and tracks test flakiness over time.

## Problem Statement

CI pipelines run automated tests constantly, but results usually just disappear into individual pipeline logs — nobody has a central place to track patterns over time. One of the most costly patterns to miss is **flaky tests** — tests that fail unpredictably, not because the code is broken, but because the test itself is unreliable. Without historical tracking, teams waste time re-investigating the same flaky tests repeatedly, or worse, start ignoring real failures because "that test always randomly fails anyway."

This project solves that by giving every test run a permanent home, and letting flakiness be measured with real data instead of gut feeling.

## Core Entities

- **TestRun** — one batch of tests executing together, at a specific time, from a specific CI pipeline.
- **TestCase** — a single, standing test (e.g. `test_login`), tracked once, regardless of how many times it's actually run.
- **TestResult** — the outcome of one specific test, within one specific run (pass/fail). Connects a TestRun and a TestCase together.

## Database Setup

Uses its own separate PostgreSQL instance (port 5433, to avoid conflicting with other local projects). Run `docker compose up` to start it. Deployed instance uses Neon (managed Postgres), configured via environment variables with local fallback.

## Progress

- [x] Project design and problem statement
- [x] Spring Boot project setup (Web, JPA, Postgres, Validation)
- [x] Entities created: `TestCase`, `TestRun`, `TestResult` — schema confirmed working, including the double foreign key relationship on `TestResult`
- [x] Ingest endpoint (POST /api/test-runs) — validated, transactional, tested with multi-result payload
- [x] Query endpoints — list runs, get run by id, and test flakiness history (pass rate + full history), using a fetch join to avoid N+1
- [x] JWT authentication — login endpoint issues tokens, all other endpoints require a valid token, tested end-to-end
- [x] Input validation — `@PastOrPresent` on run timestamps, `@Size` limits on test names
- [x] Rate limiting — login endpoint capped via Bucket4j (5 attempts/minute per IP), tested by hammering the endpoint
- [ ] JWT refresh tokens
- [x] Secrets management audit — JWT signing key fixed to use environment variable, no longer regenerates on restart
- [ ] Full OWASP self-audit documented

## Planned Endpoints

- `POST /api/test-runs` — receive a new test run (webhook from CI), with all its individual results
- `GET /api/test-runs` — list all test runs
- `GET /api/test-runs/{id}` — view one run's full details
- `GET /api/tests/{name}/history` — view one specific test's pass/fail history across all runs (the flakiness view)

## Concepts Learned

### Transactions & ACID
`@Transactional` on the ingest endpoint guarantees atomicity — a whole batch of results either all save or none do, never a partial state. Backed by all four ACID properties: Atomicity (all-or-nothing), Consistency (foreign key constraints prevent invalid states, e.g. a `TestResult` can never point at a nonexistent `TestRun`), Isolation (concurrent transactions don't corrupt each other's work), Durability (committed data survives a crash).

### Isolation Levels
Concurrent transactions can produce three specific problems if not properly isolated: dirty reads (seeing another transaction's uncommitted data), non-repeatable reads (re-reading the same row twice within one transaction and getting a different value), and phantom reads (re-running the same query twice and getting a different row count). Postgres defaults to Read Committed, which prevents dirty reads but allows the other two — a deliberate tradeoff between safety and performance, not a flaw.

### Locking & Deadlocks
Optimistic locking (version-number based, detects conflicts at save time) vs. pessimistic locking (locks a resource immediately, forcing other transactions to wait). The project's `TestCase` find-or-create logic has a real race condition risk under concurrent writes — a candidate for pessimistic locking if this became a production concern. Deadlocks (two transactions permanently waiting on each other) are resolved by the database automatically killing one transaction, and prevented by consistent lock ordering.

### JWT Refresh Tokens & OAuth2
Access tokens are deliberately short-lived to limit damage if stolen; a separate, longer-lived refresh token allows getting a new access token without re-entering credentials. OAuth2 ("Sign in with Google") works by redirecting to the identity provider's own login page, so the third-party app never sees or stores the user's real password.

### Database Normalization
1NF (atomic values, no lists in a single column), 2NF (every column depends on the *whole* primary key, relevant for composite keys), 3NF (every column depends on the key *directly*, not through another column). Applied retroactively to this project's own schema — `Task`/`Category` in the sibling project already follows 3NF by design (category details live in their own table, not duplicated per task).

### SQL vs. NoSQL
SQL's enforced foreign key constraints are a deliberate fit for this project specifically, since its core value (accurate flakiness calculation) depends on `TestResult` rows never silently pointing at nonexistent runs or test cases — a risk NoSQL's typically unenforced references would allow.

### OWASP Top 10 (2025) Self-Audit
Audited against the current OWASP Top 10; real gaps found and tracked: no per-user authorization (Broken Access Control), hardcoded test credentials (Security Misconfiguration), no dependency vulnerability scanning (Supply Chain Failures), JWT signing key regenerates on every restart (Cryptographic Failures), no rate limiting on login — **fixed** (Insecure Design), no password strength requirements (Authentication Failures), no CI dependency verification (Integrity Failures), failed logins not logged (Logging & Alerting Failures).

### Input Validation & SQL Injection Defense
Spring Data JPA's standard query methods (`findByX`, `@Query` with named parameters) are safe against SQL injection by default, since they always send user input as a separate parameter rather than concatenating it into the SQL command text. Added `@PastOrPresent` and `@Size` validation to close real gaps found in the OWASP audit. Along the way, found and fixed a real security config bug: Spring's internal `/error` redirect (used to report validation failures) was itself being blocked by `.anyRequest().authenticated()`, silently turning every validation error into a misleading 403 instead of the correct 400.

### Rate Limiting
Implemented using Bucket4j's token bucket algorithm — each IP gets a bucket of 5 tokens, one consumed per request, refilling over time. Prevents brute-force login attempts and controls cost/abuse independent of malicious intent. Applied before the username/password check, so excessive attempts are rejected without doing any real work (database lookups, password hashing).

### Secrets Management & Encryption
No secrets belong in source code or committed config — following the same environment-variable pattern established for `task-api`'s database credentials (Day 29). Applied here to fix a real gap found during the OWASP audit: the JWT signing key was regenerating randomly on every app restart (a Cryptographic Failure), silently invalidating every issued token. Fixed by deriving the key from a `JWT_SECRET` environment variable (with a clearly-marked, dev-only fallback for local use), so the same key persists across restarts — verified by confirming a token issued before a restart still works correctly after one. Also covered encryption in transit (TLS/HTTPS, already in use via Neon's `sslmode=require`) vs. encryption at rest (database-level) as two genuinely separate protections — one covers data while traveling, the other while stored, and neither substitutes for the other.