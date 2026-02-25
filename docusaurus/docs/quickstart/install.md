---
title: Install
sidebar_position: 2
---

# Install

Add the minimal modules for typed declarations, runtime loading, and JSON
boundary support.

**Prerequisites:** You have completed [Quickstart](/quickstart/).

<span id="claim-clm-pr01-07a"></span>
Installation targets the facade module that bundles the default runtime stack
(`runtime` + transitive `core` and `serialization`).

```kotlin
// build.gradle.kts

dependencies {
  implementation("io.github.amichne:konditional:VERSION")
}
```

Run a compile task:

```bash
./gradlew compileKotlin
```

## Expected Outcome

After this step, your project resolves Konditional dependencies and compiles successfully.

## Next Steps

- [Define First Flag](/quickstart/define-first-flag) - Declare your first typed feature.

## Claim Coverage

| Claim ID | Statement |
| --- | --- |
| CLM-PR01-07A | Installation targets the default facade module that resolves runtime, core, and serialization transitively. |
