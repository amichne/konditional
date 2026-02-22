---
title: Value journeys
description: Decision-grade narratives with machine-validated claim evidence.
---

Value journeys are decision-support documents for engineering and product
leaders. Each journey explains why a change matters, what operating behavior
changes, and which claims are proven by signatures and tests.

## Who this is for

Use this section when you are deciding whether to adopt, migrate, or operate a
Konditional capability in production and you need evidence-backed confidence,
not feature prose.

## Claim validation model

Every journey claim is registered in
`docs/value-journeys/journey-claims.json` and must include:

- a stable `claim_id`
- one or more signature references
- one or more test references
- owner module mapping
- computed status (`supported`, `at_risk`, `missing`)

The validation pipeline checks claim evidence against `.signatures/INDEX.sig`
and repository tests, then emits a deterministic report.

## Status semantics

- `supported`: signatures and tests both resolve.
- `at_risk`: only one evidence side resolves.
- `missing`: neither evidence side resolves.

## Remediation SLA

Use the following remediation policy for claim statuses:

- `supported`: required for new journeys before release.
- `at_risk`: allowed only during an approved migration window.
- `missing`: never allowed once CI strict mode is enabled.

## Ownership and routing

Claim ownership is tracked per claim through `owner_modules` in
`docs/value-journeys/journey-claims.json`. The validator maps those modules to
CODEOWNERS-aligned paths and includes that routing in unresolved claim reports
for direct accountability.

## Drift remediation workflow

1. Run `bash ./scripts/check-signatures-drift.sh`.
2. Run `./gradlew verifyJourneyClaimsDocs -PjourneyClaimsCiMode=warn`.
3. Fix unresolved signatures/tests or update claims and docs when behavior changed.
4. Re-run in strict mode:
   `./gradlew verifyJourneyClaimsDocs -PjourneyClaimsCiMode=strict`.
5. Keep CI rollout controlled by `JOURNEY_CLAIMS_CI_MODE` in
   `.github/workflows/ci.yml`.

## Available journeys

- [JV-001: Confident rollouts without reader-side inconsistency](./jv-001-confident-rollouts)
- [JV-002: Trusted snapshot ingestion with typed failure paths](./jv-002-safe-snapshot-ingestion)
