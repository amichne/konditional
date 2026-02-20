# What is Konditional?

Konditional is a Kotlin feature flag and configuration library for teams that
want safer rollouts and less operational uncertainty.

Instead of leaning on runtime string lookups and inconsistent evaluation paths,
Konditional gives you typed namespace definitions, deterministic decisions, and
explicit boundary behavior [TH-002-C1] [TH-003-C1] [TH-001-C2].

## Value proposition

For platform teams and product engineers, Konditional turns feature management
from a source of hidden risk into a predictable operating capability.

The value is straightforward: fewer silent failures, clearer incident handling,
and more confidence that behavior stays coherent during live changes
[TH-002-C1] [TH-004-C1].

## Why this matters now

As release cadence increases, weak feature semantics show up as production
variance: typo drift, mismatched bucketing, and brittle configuration updates.

This is usually discovered at the worst time, under load, when rollback speed
matters most [TH-003-C1] [TH-004-C1].

## Journey narrative

### Before

Teams rely on runtime keys, ad hoc rule arbitration, and boundary handling that
varies by code path. The result is avoidable ambiguity during rollouts and
incident response [TH-002-C1] [LRN-002-C2] [TH-001-C2].

### Turning point

The team adopts typed namespace-backed features, deterministic evaluation, and a
typed parse boundary before runtime activation [TH-002-C1] [TH-003-C1]
[TH-001-C2].

### After

Rollouts become calmer and more repeatable. Decisions are easier to explain,
failures are easier to diagnose, and runtime behavior remains coherent during
configuration changes [TH-003-C1] [TH-004-C1] [TH-001-C2].

## What alternatives miss

| decision axis | baseline approach | konditional approach | practical value |
| --- | --- | --- | --- |
| feature access | runtime string lookup | typed namespace property access | fewer silent activation failures [TH-002-C1] |
| rule arbitration | ad hoc or file-order-dependent behavior | specificity-aware deterministic matching | clearer rollout intent and lower ambiguity [LRN-002-C2] |
| ramp-up behavior | team-specific bucketing implementations | deterministic identity tuple bucketing | reproducible experiment outcomes [TH-003-C1] |
| runtime update visibility | implementation-dependent update semantics | coherent snapshot publication | lower mixed-read risk under load [TH-004-C1] |
| invalid config handling | defaults/exceptions/coercion vary by path | typed boundary failure results | faster, safer triage and rollback choices [TH-001-C2] |

## Decision guidance

Konditional is a strong fit when:

1. You need feature semantics that are predictable across teams and services.
2. You want compile-time help for feature access and evaluation usage.
3. You need deterministic experimentation and controlled runtime updates.

If your configuration is mostly static and changes are rare, adoption urgency is
lower, but reliability benefits still apply.

## Citation-embedded proof points

- Typed namespace feature access reduces stringly runtime drift and surfaces
  definition errors earlier [TH-002-C1].
- Rule matching follows a deterministic specificity model before first-match
  resolution, which improves explainability [LRN-002-C2].
- Deterministic bucketing keeps stable identities in stable assignment lanes,
  which protects experiment integrity [TH-003-C1].
- Runtime snapshot loads publish coherent state to readers, reducing in-flight
  inconsistency windows [TH-004-C1].
- Boundary failures are represented as typed parse results, not untyped
  exceptions, improving operational diagnosis [TH-001-C2].

## Integration path

1. Define one namespace with one typed feature and explicit default.
2. Replace one runtime key lookup with typed `evaluate(ctx)` access.
3. Add one targeted rule and validate specificity behavior [LRN-002-C2].
4. Add one ramp-up and verify deterministic repeated evaluation [TH-003-C1].
5. Load one remote snapshot through `NamespaceSnapshotLoader(...).load(...)` and
   validate typed failure handling [TH-001-C2].

## Adoption signals

- Primary KPI: reduced change failure rate for configuration-only releases.
- Secondary KPI: reduced time-to-diagnose configuration incidents.
- Early warning metric: rising typed parse failures by namespace.

## Risks and tradeoffs

- Stronger guarantees require discipline in namespace and rollout design.
- Deterministic infrastructure does not replace domain-level safety checks.

## Optional appendix: concise evidence map

| claim_id | status | key signatures | key test evidence |
| --- | --- | --- | --- |
| TH-002-C1 | supported | `Feature`, `Namespace` | `FlagEntryTypeSafetyTest.kt` |
| LRN-002-C2 | supported | `Rule`, `Targeting` | `RuleMatchingTest.kt` |
| TH-003-C1 | supported | `Bucketing` | `MissingStableIdBucketingTest.kt` |
| TH-004-C1 | supported | `InMemoryNamespaceRegistry`, `load(config: ConfigurationView)` | `NamespaceLinearizabilityTest.kt` |
| TH-001-C2 | supported | `ParseError`, `KonditionalBoundaryFailure` | `BoundaryFailureResultTest.kt` |

## Next steps

- [Installation](/getting-started/installation)
- [Your first feature](/getting-started/your-first-flag)
- [Core primitives](/learn/core-primitives)
- [Determinism proofs](/theory/determinism-proofs)
- [Parse, don't validate](/theory/parse-dont-validate)
