# konditional-runtime

`konditional-runtime` adds lifecycle operations on top of core namespaces: loading snapshots, rollback, kill-switches,
and registry hooks.

## When to Use This Module

You should use `konditional-runtime` when you need to:
- Load feature flag configuration dynamically at runtime without redeploying
- Roll back to a previous configuration snapshot when issues are detected
- Use an emergency kill-switch to disable all features in a namespace instantly
- Attach logging and metrics hooks to track configuration changes

## What You Get

- **Atomic configuration loading**: Swap entire configuration snapshots safely
- **Rollback history**: Revert to previous configurations with bounded history
- **Kill-switch operations**: `disableAll()` and `enableAll()` for emergency control
- **Registry hooks**: Attach dependency-free logging and metrics adapters

## Alternatives

Without this module, you would need to:
- Rebuild and redeploy your application to change feature flag behavior
- Implement your own atomic configuration swap mechanism with proper thread safety
- Build custom rollback tracking and emergency controls from scratch

## Installation

```kotlin
dependencies {
    implementation("io.amichne:konditional-runtime:VERSION")
}
```

## Guarantees

**Guarantee**: Configuration swaps are atomic and never partially visible.

**Mechanism**: Namespace registries store configuration in an `AtomicReference` and swap it in a single write.

**Boundary**: Readers may see either the old snapshot or the new snapshot; there is no cross-snapshot mix.

## Next steps

- [Runtime operations](/runtime/operations)
- [Configuration lifecycle](/runtime/lifecycle)
- [Serialization module](/serialization/)
