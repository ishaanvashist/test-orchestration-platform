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

Uses its own separate PostgreSQL instance (port 5433, to avoid conflicting with other local projects). Run `docker compose up` to start it.

## Progress

- [x] Project design and problem statement
- [x] Spring Boot project setup (Web, JPA, Postgres, Validation)
- [x] Entities created: `TestCase`, `TestRun`, `TestResult` — schema confirmed working, including the double foreign key relationship on `TestResult`
- [x] Ingest endpoint (POST /api/test-runs) — validated, transactional, tested with multi-result payload
- [x] Query endpoints — list runs, get run by id, and test flakiness history (pass rate + full history), using a fetch join to avoid N+1
- [ ] JWT authentication

## Planned Endpoints

- `POST /api/test-runs` — receive a new test run (webhook from CI), with all its individual results
- `GET /api/test-runs` — list all test runs
- `GET /api/test-runs/{id}` — view one run's full details
- `GET /api/tests/{name}/history` — view one specific test's pass/fail history across all runs (the flakiness view)