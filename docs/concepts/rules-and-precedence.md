# Rules and precedence

Rules are the conditional part of a flag definition. Each rule pairs a
targeting expression with a value, and evaluation chooses the highest-priority
eligible rule before falling back to the default value.

## What a rule can target

The public DSL can target standard context dimensions and custom axes.

- locale
- platform
- version range
- axis selections through `constrain(...)`
- guarded context narrowing
- ramp-up and allowlists

## Declare targeting that can change after release

An evaluation context describes the subject and its attributes. Declare a typed
attribute for a field that rules can compare. Declare a named predicate for a
business operation that clients must implement. Both can appear in the same
targeting expression. [OpenFeature evaluation context](https://openfeature.dev/specification/sections/evaluation-context/)

```kotlin
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.rules.targeting.*

enum class Plan { ENTERPRISE, TRIAL, PERSONAL }

data class Buyer(val plan: Plan, val seats: Int, val preview: Boolean) : Context

object Checkout : Namespace("checkout") {
    val plan = ContextAttribute("plan", TargetingType.enum<Plan>(), Buyer::plan)
    val preview = ContextAttribute("preview", TargetingType.boolean, Buyer::preview)
    val hasSeats = NamedPredicate<Buyer, Int>("minimumSeats", version = 1, argumentType = TargetingType.int) {
        minimum -> seats >= minimum
    }
    val fields = TargetingSchema(plan, preview, hasSeats)

    val express by boolean<Buyer>(default = false) {
        targetingSchema(fields)
        enable {
            targeting(allOf(
                plan.isOneOf(Plan.ENTERPRISE, Plan.TRIAL),
                anyOf(hasSeats(10), preview.isOneOf(true)),
            ))
        }
    }
}
```

An enterprise buyer with ten seats qualifies. A trial buyer with preview
access also qualifies. A personal buyer with both ten seats and preview access
still receives the default.

Each flag declares which attributes and predicate versions a snapshot can use.
Several flags can share a schema. A snapshot can change the seat count, add a
comparison, or combine declared predicates in a new order. Each client resolves
those references against its own declarations before it installs the snapshot.

Keep predicate keys and argument encodings stable. Give changed predicate
semantics a new version. Supply evaluation context from explicit inputs so a
disconnected client can reproduce the result. Kotlin checks the argument type
but cannot enforce the purity of application code.

## How precedence works

`FlagDefinition` sorts candidate values by descending `rule.specificity()`
before evaluation. More specific rules outrank less specific ones.

That means a rule that targets several dimensions will usually win over a
broader rule that targets only one. If two rules are equally specific, keep
their intent clearly separated instead of relying on subtle tie behavior.

## What happens during evaluation

Evaluation walks the rules in precedence order, finds the first rule whose
targeting matches the context, checks ramp-up eligibility, and returns the
resolved value. If no eligible rule produces a value, the feature default is
returned.

This same decision path is what `explain(...)` exposes through diagnostics.

## Next steps

The next concept page shows how those decisions relate to runtime state and
whole-snapshot operations.

- [Read about runtime snapshots](runtime-snapshots.md)

