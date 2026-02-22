# Install

Install the core and runtime modules first so you have typed namespace modeling
and runtime operations available in one baseline setup.

## Read this page when

- You are adding Konditional to a Kotlin project for the first time.
- You need the canonical dependency baseline for quickstart.
- You want to confirm your project is ready for typed feature definitions.

## Gradle Kotlin DSL

```kotlin
dependencies {
  implementation("io.amichne:konditional-core:VERSION")
  implementation("io.amichne:konditional-runtime:VERSION")
}
```

Replace `VERSION` with the release you intend to adopt. This baseline maps to
the typed namespace model and in-memory runtime registry [CLM-PR01-07A].

## Verify install

Run your normal project build and confirm the project compiles with both
modules resolved [CLM-PR01-07A].

## Next steps

1. Define your first feature in
   [Define first flag](/quickstart/define-first-flag).
2. Wire evaluation in [Evaluate in app code](/quickstart/evaluate-in-app-code).
3. Continue with [Add deterministic ramp-up](/quickstart/add-deterministic-ramp-up).

## Claim citations

| Claim ID | Explicit claim | Local evidence linkage | Registry link |
|---|---|---|---|
| CLM-PR01-07A | Installation targets the core namespace model and runtime in-memory registry implementation. | `#gradle-kotlin-dsl` | `/reference/claims-registry#clm-pr01-07a` |
