# Intra-feature Rule Composition (Monoid RuleSet)

## Goal
Design a type-safe, deterministic, Kotlin-first composition system that allows multiple rule contributors to define the
*same* feature while preserving Konditional’s evaluation semantics and compile-time guarantees.

## Recommended Approach (Monoid RuleSet)
Use a **feature-keyed, contravariant** `RuleSet` with an **associative `plus`** operator and an **identity `empty`** to
compose rule contributors into a single feature definition.

### Why this fits Konditional
- **Type-safe access & return types**: `RuleSet` is keyed by a *feature witness* (not strings).
- **Total evaluation**: default value remains on the feature; rules only refine.
- **Deterministic behavior**: stable rule ordering is preserved by ordered concatenation.
- **Zero runtime context checks**: compatibility is proven by the type system.
- **Extension-friendly**: contributors are just functions that return a `RuleSet`.

## Core Types (API Sketch)

```kotlin
// Feature identity witness (no strings, no reflection).
interface FeatureId<T, C, M : Namespace>

// Every Feature exposes its own identity witness.
interface Feature<T, C, M : Namespace> {
  val id: FeatureId<T, C, M>
  fun evaluate(context: C): T
}

// Rule is contravariant in context, as it only *consumes* C.
fun interface Rule<in C> {
  fun matches(context: C): Boolean
}

data class RuleSpec<out T, in C>(
  val value: T,
  val predicate: Rule<C>
)

// Monoid rule set, keyed by the feature witness.
// RC is a *supertype* of the feature context type.
class RuleSet<in RC, T, C, M : Namespace>(
  val featureId: FeatureId<T, C, M>,
  val rules: List<RuleSpec<T, RC>>
) where C : RC {
  operator fun plus(other: RuleSet<RC, T, C, M>): RuleSet<RC, T, C, M> =
    RuleSet(featureId, rules + other.rules)

  companion object {
    fun <RC, T, C, M : Namespace> empty(
      featureId: FeatureId<T, C, M>
    ): RuleSet<RC, T, C, M> where C : RC =
      RuleSet(featureId, emptyList())
  }
}

class RuleSetBuilder<T, in C> internal constructor(
  private val featureId: FeatureId<T, *, *>
) {
  private val rules = mutableListOf<RuleSpec<T, C>>()

  fun rule(value: T, predicate: Rule<C>) {
    rules += RuleSpec(value, predicate)
  }

  fun build(): List<RuleSpec<T, C>> = rules
}

// Construction is always keyed off the feature witness.
fun <T, C, M : Namespace, RC> FeatureId<T, C, M>.ruleSet(
  build: RuleSetBuilder<T, RC>.() -> Unit
): RuleSet<RC, T, C, M> where C : RC =
  RuleSet(this, RuleSetBuilder<T, RC>(this).apply(build).build())

// Feature DSL entrypoint accepts RuleSets keyed to this feature only.
class FeatureBuilder<T, C, M : Namespace> internal constructor(
  private val featureId: FeatureId<T, C, M>
) {
  private val rules = mutableListOf<RuleSpec<T, C>>()

  fun include(ruleSet: RuleSet<in C, T, C, M>) {
    rules += ruleSet.rules
  }

  fun rule(value: T, predicate: Rule<C>) {
    rules += RuleSpec(value, predicate)
  }

  internal fun build(): List<RuleSpec<T, C>> = rules
}
```

### Identity & Associativity
- **Identity**: `RuleSet.empty(featureId)` yields no rules.
- **Associative operator**: `+` concatenates in **left-to-right** order.
- **Deterministic ordering**: built by stable concatenation, no sorting or hashing.

## Usage Examples

### 1) Same feature, multiple contributors (different modules/files)

```kotlin
// Module A
object AppFlags : Namespace("app") {
  val checkoutVariant by enum<CheckoutVariant, EnterpriseContext>(
    default = CheckoutVariant.CLASSIC
  ) {
    include(CheckoutRuleSets.core)
    include(CheckoutRuleSets.platform)
  }
}

object CheckoutRuleSets {
  // contributor #1
  val core = AppFlags.checkoutVariant.id.ruleSet<EnterpriseContext> {
    rule(CheckoutVariant.OPTIMIZED) { ctx -> ctx.isPremiumUser }
  }

  // contributor #2 (different file/module)
  val platform = AppFlags.checkoutVariant.id.ruleSet<EnterpriseContext> {
    rule(CheckoutVariant.EXPERIMENTAL) { ctx -> ctx.platform == Platform.IOS }
  }
}
```

### 2) Contravariant context composition

```kotlin
interface Context { val isEmployee: Boolean }
interface EnterpriseContext : Context { val accountTier: Tier }

object AppFlags : Namespace("app") {
  val checkoutVariant by enum<CheckoutVariant, EnterpriseContext>(
    default = CheckoutVariant.CLASSIC
  ) {
    include(CheckoutRuleSets.enterprise)
    include(CheckoutRuleSets.global) // written against Context (supertype)
  }
}

object CheckoutRuleSets {
  val enterprise = AppFlags.checkoutVariant.id.ruleSet<EnterpriseContext> {
    rule(CheckoutVariant.OPTIMIZED) { ctx -> ctx.accountTier == Tier.PLATINUM }
  }

  // Contravariant: Context is a *supertype* of EnterpriseContext
  val global = AppFlags.checkoutVariant.id.ruleSet<Context> {
    rule(CheckoutVariant.EXPERIMENTAL) { ctx -> ctx.isEmployee }
  }
}
```

## Misuse Examples (Should Not Compile)

```kotlin
// Wrong feature (different feature id)
val badFeatureMix = AppFlags.checkoutVariant.id.ruleSet<EnterpriseContext> {
  rule(CheckoutVariant.OPTIMIZED) { true }
}

object OtherFlags : Namespace("other") {
  val billingVariant by enum<BillingVariant, EnterpriseContext>(default = BillingVariant.CLASSIC) {}
}

// ❌ cannot include: feature id mismatch
AppFlags.checkoutVariant { include(OtherFlags.billingVariant.id.ruleSet<EnterpriseContext> { }) }
```

```kotlin
// Wrong value type
val wrongValueType = AppFlags.checkoutVariant.id.ruleSet<EnterpriseContext> {
  rule(123) { true } // ❌ Int does not match CheckoutVariant
}
```

```kotlin
// Unsafe context direction: rule set requires a *narrower* context
interface AdminContext : EnterpriseContext { val adminLevel: Int }

val adminOnly = AppFlags.checkoutVariant.id.ruleSet<AdminContext> {
  rule(CheckoutVariant.EXPERIMENTAL) { ctx -> ctx.adminLevel > 5 }
}

// ❌ cannot include: AdminContext is not a supertype of EnterpriseContext
object AppFlags : Namespace("app") {
  val checkoutVariant by enum<CheckoutVariant, EnterpriseContext>(default = CheckoutVariant.CLASSIC) {
    include(adminOnly)
  }
}
```

## Determinism Notes
- **Stable definition order** is guaranteed by list concatenation in `RuleSet.plus` and `FeatureBuilder.include`.
- **Tie-breaking** is preserved because rules are evaluated in the exact order they are composed.
- **Associativity** ensures that combining multiple contributors is deterministic regardless of grouping:
  `(a + b) + c == a + (b + c)` with identical evaluation order.
- **No runtime checks**: composition is validated by the type system via `FeatureId` and context bounds.

## Integration Notes
- Existing extension-function rule helpers can remain unchanged: they just target `RuleSetBuilder<T, C>` or
  return `RuleSet<C, T, ...>`.
- Feature identity is explicit: the only way to build a `RuleSet` is from a `FeatureId`.
- The monoid core allows a builder DSL on top while keeping semantics deterministic and testable.
