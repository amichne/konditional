# Install

Install the core modules you need for typed features and runtime evaluation.

## What you will achieve

You will add Konditional dependencies and verify your project compiles.

## Prerequisites

You need a Gradle Kotlin DSL project.

## Main content

For the default experience, install:

- `konditional-core`
- `konditional-runtime`

Replace `VERSION` with your target release version.

## Add dependencies

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.amichne:konditional-core:VERSION")
    implementation("io.amichne:konditional-runtime:VERSION")
}
```

`konditional-runtime` provides the default namespace runtime registry.

## Optional test fixtures

Add this dependency if you want built-in test fixtures:

```kotlin
dependencies {
    testImplementation(testFixtures("io.amichne:konditional-core:VERSION"))
}
```

## Verify

Run a compile cycle and confirm dependency resolution succeeds.

```bash
./gradlew :konditional-core:compileKotlin
```

If your own project is outside this repository, run your project's compile task.

## Next steps

- [Define first flag](/quickstart/define-first-flag)
- [Quickstart](/quickstart/)
