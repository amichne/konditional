# konditional-runtime

`konditional-runtime` adds lifecycle operations on top of core namespaces: loading snapshots, rollback, kill-switches,
and registry hooks.

Use this module when you want dynamic configuration updates at runtime.

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
- [Serialization module](/serialization/index)
