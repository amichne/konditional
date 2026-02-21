# Add deterministic ramp-up

Add a rollout gate so only a percentage of users receives the feature while
assignment remains stable.

## What you will achieve

You will add a ramp-up rule and verify that a fixed `stableId` gets consistent
assignment.

## Prerequisites

Complete [Evaluate in app code](/quickstart/evaluate-in-app-code).

## Main content

Update your feature definition:

```kotlin
object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false) {
        rule(true) { rampUp { 10.0 } }
    }
}
```

Konditional computes a deterministic bucket from `salt`, feature key, and
`stableId`. For unchanged inputs, assignment remains stable.

Use a durable identifier:

```kotlin
val ctx = Context(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.IOS,
    appVersion = Version.of(2, 0, 0),
    stableId = StableId.of(userId), // Use a persistent identity source.
)
```

## Verify

Run repeated evaluations for the same user and confirm a stable result:

```kotlin
val sameUserResults = (1..100).map { AppFeatures.darkMode.evaluate(ctx) }
check(sameUserResults.distinct().size == 1)
```

Then run a broad sample to check rough distribution:

```kotlin
val sample = 10_000
val treated = (0 until sample).count { i ->
    val c = ctx.copy(stableId = StableId.of("user-$i"))
    AppFeatures.darkMode.evaluate(c)
}
println("percentage=${treated * 100.0 / sample}")
```

## Common issues

- using random IDs, which makes assignment unstable;
- changing `salt` unintentionally, which reshuffles all users;
- expecting exact percentages on very small sample sizes.

## Next steps

- [Load first snapshot safely](/quickstart/load-first-snapshot-safely)
- [Roll out gradually guide](/how-to-guides/rolling-out-gradually)
