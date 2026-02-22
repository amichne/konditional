# How-to: integrate custom business logic

Use this page to add domain-specific targeting without giving up type safety,
determinism, or parse-boundary discipline.

## Read this page when

- Base `Context` fields are not enough for your targeting rules.
- You need predicates like subscription tier, account age, or contract type.
- You want compile-time errors when context wiring is wrong.

## Prerequisites

- A typed domain model for the fields you want to target.
- A trusted mapping from external input into your context model.
- Unit tests for custom predicates.

## Deterministic steps

1. Define a trusted context type with required and domain fields.

```kotlin
data class BillingContext(
    override val stableId: StableId,
    override val platform: Platform,
    override val locale: AppLocale,
    override val appVersion: Version,
    val plan: Plan,
    val invoiceAgeDays: Int,
) : Context, Context.PlatformContext, Context.LocaleContext,
    Context.VersionContext, Context.StableIdContext

enum class Plan { FREE, PRO, ENTERPRISE }
```

2. Keep business predicates inside typed `extension { ... }` blocks.

```kotlin
object BillingFlags : Namespace("billing") {
    val dunningBanner by boolean<BillingContext>(default = false) {
        enable {
            extension {
                plan == Plan.ENTERPRISE && invoiceAgeDays >= 30
            }
        }
    }
}
```

3. Parse untrusted inputs into the trusted context model.

```kotlin
sealed interface BillingContextError {
    data object MissingStableId : BillingContextError
    data class InvalidPlan(val raw: String) : BillingContextError
}
```

Treat this parser as the boundary. Do not evaluate features until parsing
returns a trusted `BillingContext`.

4. Evaluate with the typed context only.

```kotlin
val context: BillingContext = buildBillingContext(request)
val showBanner = BillingFlags.dunningBanner.evaluate(context)
```

5. Keep rule ownership explicit.

- Put plan logic in one namespace owned by one team.
- Avoid cross-namespace business conditions.
- Move shared predicates into reusable pure functions.

## Verification checklist

- [ ] Domain fields are represented by Kotlin types, not string keys.
- [ ] No nullable shortcuts are used in core targeting logic.
- [ ] Boundary parsing returns typed errors for invalid external input.
- [ ] Features compile only with the intended custom context type.
- [ ] Tests cover both matching and non-matching business predicates.

## Next steps

- [Testing features](/how-to-guides/testing-features)
- [Namespace isolation](/how-to-guides/namespace-isolation)
- [Thread safety](/production-operations/thread-safety)
