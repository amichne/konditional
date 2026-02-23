---
title: Install
sidebar_position: 2
---

# Install

Add the minimal modules for typed declarations and runtime loading.

**Prerequisites:** You have completed [Quickstart](/quickstart/).

<span id="claim-clm-pr01-07a"></span>
Installation targets the core namespace model and runtime in-memory registry implementation.

```kotlin
// build.gradle.kts

dependencies {
  implementation("io.github.amichne:konditional-core:VERSION")
  implementation("io.github.amichne:konditional-runtime:VERSION")
}
```

Run a compile task:

```bash
./gradlew :konditional-core:compileKotlin
```

## Expected Outcome

After this step, your project resolves Konditional dependencies and compiles successfully.

## Next Steps

- [Define First Flag](/quickstart/define-first-flag) - Declare your first typed feature.

## Claim Coverage

| Claim ID | Statement |
| --- | --- |
| CLM-PR01-07A | Installation targets the core namespace model and runtime in-memory registry implementation. |
