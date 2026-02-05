---
toc_min_heading_level: 2
toc_max_heading_level: 5
---

<!--
Generated file: do not edit docs/recipes.md directly.
Source: docusaurus/docs-templates/recipes.template.md + konditional-observability/src/docsSamples/kotlin/io/amichne/konditional/docsamples/RecipesSamples.kt
-->

# Recipes: Best-Practice Patterns

Practical patterns for real-world feature control using only Konditional building blocks. Each recipe highlights a
supported solution area and makes the guarantee boundaries explicit.

:::note Generated from real samples
Recipes are compiled from Kotlin sources in `konditional-observability/src/docsSamples/kotlin`.
Edit those sources, then regenerate docs via `:konditional-observability:generateRecipesDocs`.
:::

Covered solution areas:

- Typed features (booleans, enums, structured values)
- Deterministic rollouts and salting
- Axes and custom context targeting
- Remote configuration (snapshot/patch boundary + rollback)
- Shadow evaluation for safe migrations
- Namespace isolation and kill-switch
- Observability hooks (logging + metrics)

---

## Typed Variants Instead of Boolean Explosion {#recipe-1-typed-variants-instead-of-boolean-explosion}

When you have multiple rollout variants, model them as a typed value (enum or string) rather than composing booleans.

```kotlin
enum class CheckoutVariant { CLASSIC, FAST_PATH, NEW_UI }

object CheckoutFlags : Namespace("checkout") {
    val variant by enum<CheckoutVariant, Context>(default = CheckoutVariant.CLASSIC) {
        rule(CheckoutVariant.FAST_PATH) { rampUp { 10.0 } }
        rule(CheckoutVariant.NEW_UI) { rampUp { 1.0 } }
    }
}

fun renderCheckout(context: Context) {
    when (CheckoutFlags.variant.evaluate(context)) {
        CheckoutVariant.CLASSIC -> renderClassic()
        CheckoutVariant.FAST_PATH -> renderFastPath()
        CheckoutVariant.NEW_UI -> renderNewUi()
    }
}
```

- **Guarantee**: Variant values are compile-time correct and exhaustively handled.
- **Mechanism**: Enum-typed feature delegates (`enum<...>`) and Kotlin `when` exhaustiveness.
- **Boundary**: Remote JSON can only select enum constants already compiled into the binary.

---

## Deterministic Ramp-Up with Resettable Salt {#recipe-2-deterministic-ramp-up-with-resettable-salt}

Gradually roll out a feature without reshuffling users; use `salt(...)` when you need a clean resample.

```kotlin
object RampUpFlags : Namespace("ramp-up") {
    val newCheckout by boolean<Context>(default = false) {
        salt("v1")
        enable { rampUp { 10.0 } }
    }
}

fun isCheckoutEnabled(context: Context): Boolean =
    RampUpFlags.newCheckout.evaluate(context)
```

To restart the experiment with a fresh sample:

```kotlin
object RampUpResetFlags : Namespace("ramp-up-reset") {
    val newCheckout by boolean<Context>(default = false) {
        salt("v2")
        enable { rampUp { 10.0 } }
    }
}
```

- **Guarantee**: Same `(stableId, flagKey, salt)` always yields the same bucket.
- **Mechanism**: SHA-256 deterministic bucketing in `RampUpBucketing`.
- **Boundary**: Changing `salt` intentionally redistributes buckets.

---

## Runtime-Configurable Segments via Axes {#recipe-3-runtime-configurable-segments-via-axes}

Use axes for segment targeting you want to update via JSON (without redeploying predicates).

```kotlin
enum class Segment(override val id: String) : AxisValue<Segment> {
    CONSUMER("consumer"),
    SMB("smb"),
    ENTERPRISE("enterprise"),
}

object Axes {
    val SegmentAxis = Axis.of<Segment>("segment")
}

object SegmentFlags : Namespace("segment") {
    @Suppress("UnusedPrivateProperty")
    private val segmentAxis = Axes.SegmentAxis

    val premiumUi by boolean<Context>(default = false) {
        enable { axis(Segment.ENTERPRISE) }
    }
}

fun isPremiumUiEnabled(): Boolean {
    val segmentContext =
        object :
            Context,
            Context.LocaleContext,
            Context.PlatformContext,
            Context.VersionContext,
            Context.StableIdContext {
            override val locale = AppLocale.UNITED_STATES
            override val platform = Platform.IOS
            override val appVersion = Version.of(2, 1, 0)
            override val stableId = StableId.of("user-123")
            override val axisValues = axisValues { +Segment.ENTERPRISE }
        }

    return SegmentFlags.premiumUi.evaluate(segmentContext)
}
```

- **Guarantee**: Segment targeting is type-safe and serializable.
- **Mechanism**: Axis IDs are stored in JSON; `axis(...)` evaluates against `Context.axisValues`.
- **Boundary**: Axis IDs must remain stable across builds and obfuscation.

---

## Business Logic Targeting with Custom Context + Extension {#recipe-4-business-logic-targeting-with-custom-context-extension}

Use strongly-typed extensions for domain logic that should not be remotely mutable.

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier,
    val employeeCount: Int,
) : Context, Context.LocaleContext, Context.PlatformContext, Context.VersionContext, Context.StableIdContext

enum class SubscriptionTier { FREE, PRO, ENTERPRISE }

object PremiumFeatures : Namespace("premium") {
    val advancedAnalytics by boolean<EnterpriseContext>(default = false) {
        enable {
            extension { subscriptionTier == SubscriptionTier.ENTERPRISE && employeeCount > 100 }
        }
    }
}
```

- **Guarantee**: Extension predicates are type-safe and enforced at compile time.
- **Mechanism**: `Feature<T, EnterpriseContext>` makes the extension receiver strongly typed.
- **Boundary**: Extension logic is not serialized; only its rule parameters (e.g., ramp-up) can be updated remotely.

---

## Structured Values with Schema Validation {#recipe-5-structured-values-with-schema-validation}

Use `custom<T>` for structured configuration that must be validated at the JSON boundary.

```kotlin
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val backoffMs: Double = 1000.0,
    val enabled: Boolean = true,
) : Konstrained<ObjectSchema> {
    override val schema = schemaRoot {
        ::maxAttempts of { minimum = 1 }
        ::backoffMs of { minimum = 0.0 }
        ::enabled of { default = true }
    }
}

object PolicyFlags : Namespace("policy") {
    val retryPolicy by custom<RetryPolicy, Context>(default = RetryPolicy()) {
        rule(RetryPolicy(maxAttempts = 5, backoffMs = 2000.0)) { platforms(Platform.ANDROID) }
    }
}
```

- **Guarantee**: Invalid structured config is rejected before it reaches evaluation.
- **Mechanism**: Kontracts schema validation at `ConfigurationSnapshotCodec.decode(...)`.
- **Boundary**: Semantic correctness of field values (e.g., "appropriate backoff") remains a human responsibility.

---

## Safe Remote Config Loading + Rollback {#recipe-6-safe-remote-config-loading-rollback}

Use `ParseResult` to enforce a hard boundary at the JSON parse step, and roll back on bad updates.

```kotlin
fun loadRemoteConfig() {
    val json = fetchRemoteConfig()
    val features = AppFeatures

    when (val result = ConfigurationSnapshotCodec.decode(json)) {
        is ParseResult.Success -> features.load(result.value)
        is ParseResult.Failure -> RecipeLogger.error { "Config rejected: ${result.error.message}" }
    }
}
```

If a later update causes issues:

```kotlin
fun rollbackConfig() {
    val success = AppFeatures.rollback(steps = 1)
    if (!success) RecipeLogger.warn { "Rollback failed: insufficient history" }
}
```

- **Guarantee**: Invalid config never becomes active; swaps are atomic.
- **Mechanism**: `ParseResult` boundary + `Namespace.load(...)` atomic swap.
- **Boundary**: A valid config can still be logically wrong; rollback is the safe escape hatch.

---

## Controlled Migrations with Shadow Evaluation {#recipe-7-controlled-migrations-with-shadow-evaluation}

Compare a candidate configuration to baseline behavior without changing production outputs.

```kotlin
fun evaluateWithShadowedConfig(context: Context): Boolean {
    val candidateJson = fetchCandidateConfig()
    val candidateConfig = ConfigurationSnapshotCodec.decode(candidateJson).getOrThrow()
    val candidateRegistry =
        InMemoryNamespaceRegistry(namespaceId = AppFeatures.namespaceId).apply {
            load(candidateConfig)
        }

    val value =
        AppFeatures.darkMode.evaluateWithShadow(
            context = context,
            candidateRegistry = candidateRegistry,
            onMismatch = { mismatch ->
                RecipeLogger.warn {
                    "shadowMismatch key=${mismatch.featureKey} kinds=${mismatch.kinds} baseline=${mismatch.baseline.value} candidate=${mismatch.candidate.value}"
                }
            },
        )

    return applyDarkMode(value)
}
```

- **Guarantee**: Production behavior stays pinned to baseline while candidate is evaluated.
- **Mechanism**: `evaluateWithShadow(...)` evaluates baseline + candidate but returns baseline value.
- **Boundary**: Shadow evaluation is inline and adds extra work to the hot path; sample if needed.

---

## Namespace Isolation + Kill-Switch {#recipe-8-namespace-isolation-kill-switch}

Use separate namespaces for independent lifecycles, and a scoped kill-switch for emergencies.

```kotlin
sealed class AppDomain(id: String) : Namespace(id) {
    data object Payments : AppDomain("payments") {
        val applePay by boolean<Context>(default = false)
    }

    data object Search : AppDomain("search") {
        val reranker by boolean<Context>(default = false)
    }
}

fun disablePayments() {
    AppDomain.Payments.disableAll()
}
```

- **Guarantee**: Disabling a namespace only affects that namespace.
- **Mechanism**: Each `Namespace` has an isolated registry and kill-switch.
- **Boundary**: `disableAll()` returns defaults; it does not modify feature definitions or remote config state.

---

## Lightweight Observability Hooks {#recipe-9-lightweight-observability-hooks}

Attach logging and metrics without depending on a specific vendor SDK.

```kotlin
fun attachHooks() {
    val hooks =
        RegistryHooks.of(
            logger =
                object : KonditionalLogger {
                    override fun warn(message: () -> String, throwable: Throwable?) {
                        AppLogger.warn(message(), throwable)
                    }
                },
            metrics =
                object : MetricsCollector {
                    override fun recordEvaluation(event: Metrics.Evaluation) {
                        AppMetrics.increment("konditional.eval", tags = mapOf("feature" to event.featureKey))
                    }
                },
        )

    AppFeatures.setHooks(hooks)
}
```

- **Guarantee**: Hooks receive evaluation and lifecycle signals with consistent payloads.
- **Mechanism**: `RegistryHooks` are invoked inside the runtime's evaluation and load paths.
- **Boundary**: Hooks run on the hot path; keep them non-blocking.

---

## Next Steps

- [Rule Model](/rules)
- [Rollouts & Bucketing](/rollouts-and-bucketing)
- [Registry & Configuration](/registry-and-configuration)
- [Observability & Debugging](/observability-and-debugging)
