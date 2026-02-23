---
title: Snapshot Load Options
sidebar_position: 5
---

# Snapshot Load Options

Reference for boundary-policy controls used during snapshot or patch ingestion.

## Type

```kotlin
data class SnapshotLoadOptions(
  val unknownFeatureKeyStrategy: UnknownFeatureKeyStrategy = UnknownFeatureKeyStrategy.Fail,
  val missingDeclaredFlagStrategy: MissingDeclaredFlagStrategy = MissingDeclaredFlagStrategy.Reject,
  val onWarning: (SnapshotWarning) -> Unit = {},
)
```

## Factory Modes

| Factory | Unknown keys | Missing declared flags | Typical use |
| --- | --- | --- | --- |
| `SnapshotLoadOptions.strict()` | `Fail` | `Reject` | Default production safety mode. |
| `SnapshotLoadOptions.skipUnknownKeys { ... }` | `Skip` | `Reject` | Forward compatibility while rolling out declarations. |
| `SnapshotLoadOptions.fillMissingDeclaredFlags { ... }` | `Fail` | `FillFromDeclaredDefaults` | Backfill absent fields from code defaults during migration windows. |

## Strategies

| Strategy type | Values | Effect |
| --- | --- | --- |
| `UnknownFeatureKeyStrategy` | `Fail`, `Skip` | Controls behavior for keys absent from namespace schema. |
| `MissingDeclaredFlagStrategy` | `Reject`, `FillFromDeclaredDefaults` | Controls behavior when payload omits declared features. |

## Warning Model

Warnings are emitted through `onWarning` when using non-strict behaviors. Keep this callback lightweight and side-effect-safe.

## Next Steps

- [Parse Boundary Concept](/concepts/parse-boundary)
- [Parse Don\'t Validate Theory](/theory/parse-dont-validate)
