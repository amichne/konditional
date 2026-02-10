# Mobile Value Demonstration Prompt Set

## Why this prompt set exists

Use these prompts to identify high-signal, low-complexity candidates that demonstrate concrete value from adopting a type-safe feature flag and configuration library in mobile environments where teams currently rely on string-keyed maps, boolean-only flags, and enum-based capability toggles.

This set is optimized for enterprises that currently have:

- Stringly-keyed and stringly-valued maps for config, labels, and translation management.
- Feature flag scopes across server, user, and product.
- Mostly boolean flags.
- Enum classes for capability types.
- A single hot-reloadable capability (`RampUpFeature`).

## Operator guidance (how to use this document)

- Use **one candidate-discovery prompt + one solution prompt + one value-statement prompt per branch**.
- Pick branches so each branch demonstrates one of these narratives:
  - How easy X is once adopted.
  - How simple reuse of X becomes.
  - What testing becomes.
  - Errors surface in X, instead of Y.
- Prefer small vertical slices over broad rewrites.
- Capture baseline and after metrics even if approximate (time-to-change, files touched, escaped defects, test setup effort).

---

## A) Candidate-discovery prompts (find realistic problems worth demonstrating)

### A1. Developer-velocity slowdown audit

```text
You are assessing a mobile codebase that uses string-keyed maps and boolean feature flags.

Goal:
Find the top 5 change workflows where developer velocity is slowed by:
- string keys,
- duplicated flag lookup logic,
- weak typing around config/capabilities,
- late discovery of mistakes.

For each workflow, produce:
1) Trigger event (what business change causes this work).
2) Current change steps (including all files/systems touched).
3) Failure modes and where they are detected (compile, test, runtime, production).
4) Typical time-to-complete and review friction.
5) Why this is a good demo candidate (high frequency x high pain x low migration effort).

Constraints:
- Focus on server-scoped, user-scoped, and product-scoped boolean flags.
- Include RampUpFeature hot-reload behavior where relevant.
- Avoid hypothetical platform redesign; stay close to current architecture.
```

### A2. Operational-impact scoring prompt

```text
You are scoring candidate mobile feature-flag/config problems for demo value.

Use this weighted model:
- Frequency of change (25%)
- Severity of production impact when wrong (30%)
- Developer time currently spent (20%)
- Ease of incremental adoption (15%)
- Cross-team visibility/storytelling value (10%)

Input:
A list of candidate changes involving string-map config, labels/translations, boolean flags, and enum capabilities.

Output:
A ranked table with:
- Candidate name
- Weighted score
- Evidence notes
- “Demo narrative fit” tag:
  - ease-on-adoption
  - reuse-simplicity
  - testing-upgrade
  - earlier-error-surfacing
- Suggested branch scope (small/medium)

Then recommend top 2 candidates with rationale.
```

### A3. Error-surface shift finder

```text
Analyze where errors are currently discovered in mobile feature/config flows.

Map each error class to discovery stage:
- typo in key
- wrong scope lookup (server/user/product)
- missing translation/config entry
- wrong capability enum mapping
- stale fallback assumptions after RampUpFeature change

For each error class:
1) Current discovery stage (runtime, QA, production, etc.).
2) Desired discovery stage after type-safe adoption.
3) Minimal design change required to shift discovery left.
4) Candidate demo scenario that makes this shift obvious.

Return only scenarios that can be demonstrated in <= 1 branch with minimal blast radius.
```

### A4. Reuse-friction prompt

```text
Identify repeated mobile patterns where the same business flag/config rule is reimplemented in multiple places (UI gating, networking, analytics, experiments, copy selection).

Output a list of top reuse-friction hotspots with:
- Number of call sites
- Variants of duplicated logic
- Risk from drift between implementations
- Candidate abstraction that a typed flag/config library would centralize
- Why this is demonstrably valuable in a short branch
```

---

## B) Solution prompts (describe the implementation shape you want)

### B1. “How easy X is once adopted” implementation prompt

```text
Implement a minimal vertical slice that replaces one high-frequency string-keyed boolean flag flow with a type-safe equivalent.

Requirements:
- Preserve existing behavior.
- Keep scope explicit (server/user/product).
- Replace ad-hoc string keys with typed identifiers.
- Keep migration incremental: old and new path may coexist behind a boundary.
- Add focused tests showing equivalent behavior.

Deliverables:
1) Before/after call-site comparison.
2) A small typed API surface that a mobile developer can follow quickly.
3) Notes on files touched and expected future rollout pattern.

Success criteria:
- Fewer call-site details to remember.
- Less key-string handling at usage sites.
- Clear compile-time guidance on correct scope/usage.
```

### B2. “How simple reuse of X becomes” implementation prompt

```text
Create a reusable typed definition for one business capability/flag currently duplicated across multiple mobile layers.

Requirements:
- Single source of truth for identifier, scope, and default behavior.
- Reuse from at least two distinct contexts (e.g., UI + analytics, UI + network policy).
- Keep capability values boolean and enum-compatible with current system.
- Include RampUpFeature integration only if already part of the selected workflow.

Deliverables:
1) Shared typed definition.
2) Updated usages in multiple contexts.
3) Tests proving consistent behavior across contexts.
4) Short migration note for adding next reusable definition.

Success criteria:
- Eliminate duplicated lookup/mapping logic.
- Reduce drift risk between contexts.
- Demonstrate lower marginal cost for next adoption.
```

### B3. “What the testing becomes” implementation prompt

```text
Refactor one flag/config workflow so tests move from string-map setup to typed scenario setup.

Requirements:
- Keep runtime behavior unchanged.
- Replace brittle map fixtures with typed test builders/fixtures.
- Cover at least:
  - enabled path
  - disabled path
  - missing/invalid value handling
  - scope mismatch handling (where applicable)

Deliverables:
1) Before/after test examples.
2) Explanation of reduced fixture noise and improved intent readability.
3) Any helper APIs introduced for repeatable test setup.

Success criteria:
- Tests describe business intent, not key plumbing.
- Lower setup overhead per scenario.
- Failures are easier to interpret.
```

### B4. “Errors surface in X, instead of Y” implementation prompt

```text
Implement a narrow change that shifts one common configuration/flag error from runtime/QA to compile-time or deterministic test-time.

Requirements:
- Pick one concrete error class (key typo, wrong scope, missing mapping, enum mismatch).
- Introduce typed API or validation boundary that catches it earlier.
- Show an explicit example of the old failure mode and the new early failure mode.
- Keep compatibility with existing boolean capability constraints.

Deliverables:
1) Error class selected and why it matters.
2) Code changes introducing earlier detection.
3) Test(s) proving earlier detection behavior.
4) Risk/rollout note.

Success criteria:
- Clear evidence of left-shifted detection.
- Reduced chance of production discovery.
- Minimal migration burden.
```

---

## C) Value-statement prompt (turn implementation results into enterprise messaging)

```text
Given the implemented branch changes, formulate a clear enterprise value statement.

Inputs:
- What changed (technical summary)
- Baseline pain (before)
- New workflow (after)
- Evidence (tests, compile-time checks, reduced code paths, reduced setup time)
- Any observed metrics or estimated deltas

Output format:
1) One-sentence executive value statement.
2) Three supporting bullets:
   - developer-velocity impact
   - risk/quality impact
   - scalability/reuse impact
3) “Proof points” list tied directly to artifacts in the branch.
4) “What this enables next” paragraph (incremental rollout, no big-bang rewrite).

Tone constraints:
- Concrete, not aspirational.
- Emphasize operational outcomes over framework enthusiasm.
- Explicitly mention that current system is boolean/capability constrained and still benefits.
```

---

## D) Optional branch planning prompt (if you want branch-by-branch narrative discipline)

```text
Create a 4-branch demo plan where each branch maps to exactly one narrative:
1) easy-on-adoption
2) reuse-simplicity
3) testing-upgrade
4) earlier-error-surfacing

For each branch define:
- Candidate workflow selected
- Scope boundary
- Files/components likely touched
- Expected reviewer focus
- Objective success signals
- Rollback strategy

Keep each branch independently understandable and mergeable.
```
