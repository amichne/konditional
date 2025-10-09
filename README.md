# Konditional

Idiomatic Kotlin framework for boolean configuration parameters with market, locale, platform, and version constraints
plus deterministic percentage rollout.
No DI. Pure, thread-safe evaluation.

## Why this works

1. **Deterministic identity**: 
   - Each user is keyed by a stable 32‑hex UserId.
   - Hashing `(salt:flagKey:id)` with SHA‑256 yields a uniform pseudo‑random value per `(flag,user)`.
   - The same user and flag always map to the same bucket.
2. **Independent cohorts per flag**:
   - The flag key is inside the hash input.
   - Buckets for different flags are statistically independent.
   - Changing one flag does not perturb another.
3. **Two‑decimal precision**:
   - 10,000 buckets map cleanly to coverage percentages with 0.01% resolution.
4. **Rule precedence by specificity**:
   - More constrained rules win.
   - Non‑cohort users fall through to the next matching rule.
   - Defaults apply last with their own coverage, giving a simple way to express “30% true by default”.
5. **Pure, allocation‑light evaluation**:
   - All data is immutable.
   - A single `AtomicReference` holds the registry and supports hot‑reload.

## “Proof‑like” correctness sketch

Let `H(flag, id, salt)` be SHA‑256 as a 128‑bit stream; we take the first 32 bits and reduce mod 10,000.
Assume SHA‑256 is a Pseudo Random Function (PRF) for our purposes.

- **Determinism**: For fixed `flag`, `id`, `salt`, `H` is constant. Therefore each user’s bucket is constant.
- **Uniformity**: For a PRF, the modulo of a large power‑of‑two sized integer onto 10,000 approximates uniform on
  `[0,9999]` with negligible bias (< 1/2^32). The empirical tests bound observed deviation within a small interval (±3%
  around the target for N=10k).
- **Independence**: For different `flag` values, inputs differ, so outputs act as independent samples.
- **Coverage**: A cohort defined by threshold `t = round(p * 100)` selects all buckets `< t`. The expected share is
  `p%`.
- **Precedence correctness**: Sorting rules by number of constrained dimensions implements a lattice where more specific
  contexts shadow broader ones. For any context `c`, the first matching rule is the maximally constrained element wrt
  `c`.
- **Default semantics**: If no rule assigns, default coverage assigns `true` to a cohort of size `p%`
  (defaulting to 100% when the declared default is `true`, otherwise 0%). Everyone else receives `false`,
  ensuring a total function `EvalContext → Boolean`.

## Project layout

```
konditional/
  build.gradle.kts
  settings.gradle.kts
  src/main/kotlin/io/amichne/flags/Flags.kt
  src/test/kotlin/io/amichne/flags/FlagsTests.kt
  README.md
```

## Quick start

```bash
./gradlew test
```

## DSL example

```kotlin
val registry = ConfigBuilder().apply {
    flag("enable_compact_cards") {
        default(false)
        rule {
            markets(Market.US); platforms(Platform.IOS); versions(min = "7.10.0")
            value(true, coveragePct = 50.0)
        }
        rule { locales(AppLocale.HI_IN); value(true) }
    }
}.build()

Flags.load(registry)

val ctx = EvalContext(
    locale = AppLocale.EN_US,
    platform = Platform.IOS,
    appVersion = Version.parse("7.12.3"),
    userId = UserId("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
)
val result: Boolean = Flags.eval("enable_compact_cards", ctx)
```

## Operational notes

- Change `salt` on a flag to reshuffle cohorts for that flag only.
- Keep rule sets small and specific; rely on defaults for broad behavior.
- Evaluate once per request and cache in your login response.
