# Core DSL best practices

This page describes the recommended way to compose Konditional features using
what the current public API actually exposes. Use these patterns as your
baseline for production definitions and reviews.

## Build from stable namespace boundaries

Start by grouping related flags into one `Namespace`. Keep each namespace
focused so you can load, rollback, and disable behavior without cross-domain
side effects.

```kotlin
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace

object CheckoutFlags : Namespace("checkout") {
    val fastPath by boolean<Context>(default = false)
    val timeoutMs by integer<Context>(default = 1500)
}
```

## Prefer explicit, typed rule composition

Use the value-first rule form for straightforward rules. Use the
criteria-first `rule { ... } yields value` form when the targeting block is
long and the produced value is easier to read at the end.

```kotlin
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform

object AppFlags : Namespace("app") {
    val checkoutVariant by string<Context>(default = "classic") {
        rule("fast") { ios() }

        rule {
            platforms(Platform.ANDROID)
            rampUp { 25.0 }
            note("Android fast-path canary")
        } yields "fast"
    }
}
```

## Use `whenContext<R : Context>` for capability narrowing

When a feature is defined on a broad context type, add gated logic with
`whenContext` so rules remain total and deterministic for all contexts.

```kotlin
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.dsl.rules.targeting.scopes.whenContext

enum class Tier { FREE, ENTERPRISE }

data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val tier: Tier,
) : Context

object EntitlementFlags : Namespace("entitlements") {
    val premiumUi by boolean<Context>(default = false) {
        rule(true) {
            whenContext<EnterpriseContext> { tier == Tier.ENTERPRISE }
        }
    }
}
```

If the runtime context is not `EnterpriseContext`, the `whenContext` predicate
returns `false` for that leaf instead of throwing.

## Treat axes as first-class dimensions

Prefer explicit axis handles for readability and locality. Use inferred axis
values only when the namespace already owns the axis registration for that
value type.

```kotlin
enum class Environment(override val id: String) : AxisValue<Environment> {
    PROD("prod"),
    STAGE("stage"),
}

object RolloutFlags : Namespace("rollouts") {
    private val environmentAxis = axis<Environment>("environment")

    val checkout by boolean<Context>(default = false) {
        rule(true) { axis(environmentAxis, Environment.PROD) }
        rule(true) { axis(Environment.STAGE) }
    }
}
```

## Reuse rule logic with `RuleSet` and `include`

Use `ruleSet` to capture reusable targeting blocks, then include them in
multiple features. Composition order is deterministic and left-to-right.

```kotlin
import io.amichne.konditional.core.dsl.ruleSet

object AppFlags : Namespace("app") {
    val checkout by string<Context>(default = "classic")

    private val iosCanary = checkout.ruleSet {
        rule("fast") {
            ios()
            rampUp { 10.0 }
        }
    }

    val checkoutUi by string<Context>(default = "classic") {
        include(iosCanary)
        rule("classic") { android() }
    }
}
```

## Keep rollout intent explicit

Use `rampUp` for deterministic progressive rollout, `allowlist` for explicit
exceptions, and `salt("...")` to intentionally reshuffle cohorts.

```kotlin
object PaymentsFlags : Namespace("payments") {
    val v2Flow by boolean<Context>(default = false) {
        salt("v2")
        allowlist(StableId.of("employee-123"))

        rule(true) {
            rampUp { 20.0 }
            note("20% rollout with tester bypass")
        }
    }
}
```

## Model custom values with `Konstrained`

Use object schemas for multi-field values and value classes for primitive or
array wrappers. Keep schema definitions deterministic and construction typed.

```kotlin
import io.amichne.konditional.core.types.Konstrained
import io.amichne.kontracts.dsl.arraySchema
import io.amichne.kontracts.dsl.stringSchema
import io.amichne.kontracts.schema.ArraySchema

@JvmInline
value class Tags(val values: List<String>) : Konstrained<ArraySchema<String>> {
    override val schema = arraySchema { elementSchema(stringSchema { minLength = 1 }) }
}

object ContentFlags : Namespace("content") {
    val promotedTags by custom<Tags, Context>(default = Tags(listOf("default")))
}
```

## Keep the runtime boundary explicit

Use `NamespaceSnapshotLoader` as the JSON boundary. Handle `Result` directly,
log typed parse errors, and rely on last-known-good behavior when load fails.

```kotlin
import io.amichne.konditional.core.result.parseErrorOrNull
import io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader

val result = NamespaceSnapshotLoader(AppFlags).load(remoteJson)
result.onFailure { failure ->
    val parseError = result.parseErrorOrNull()
    println("Config rejected: ${parseError?.message ?: failure.message}")
}
```

`NamespaceSnapshotLoader.load(...)` decodes and loads atomically on success.
For recovery workflows, use `rollback(...)` from `io.amichne.konditional.runtime`.

## Add observability without changing semantics

Attach hooks for logs and metrics, and use shadow evaluation for migration
comparisons while preserving baseline outputs.

```kotlin
import io.amichne.konditional.api.evaluateWithShadow
import io.amichne.konditional.core.ops.KonditionalLogger
import io.amichne.konditional.core.ops.RegistryHooks

AppFlags.setHooks(
    RegistryHooks.of(
        logger = object : KonditionalLogger {
            override fun warn(message: () -> String, throwable: Throwable?) {
                println(message())
            }
        },
    ),
)

val value = AppFlags.checkout.evaluateWithShadow(
    context = ctx,
    candidateRegistry = candidateRegistry,
    onMismatch = { mismatch ->
        println("Shadow mismatch for ${mismatch.featureKey}: ${mismatch.kinds}")
    },
)
```

## Next steps

- [Rule DSL reference](/core/rules)
- [Core types](/core/types)
- [Runtime operations](/runtime/operations)
- [Type safety boundaries](/theory/type-safety-boundaries)
