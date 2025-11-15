# CLAUDE.md - AI Assistant Guide for Konditional

This document provides comprehensive guidance for AI assistants working with the Konditional codebase.

## Table of Contents

- [Project Overview](#project-overview)
- [Codebase Structure](#codebase-structure)
- [Development Workflow](#development-workflow)
- [Architecture & Design Principles](#architecture--design-principles)
- [Code Conventions](#code-conventions)
- [Testing Guidelines](#testing-guidelines)
- [Build & Publishing](#build--publishing)
- [Common Tasks](#common-tasks)
- [Important Gotchas](#important-gotchas)

---

## Project Overview

**Konditional** is a type-safe, deterministic feature flags library for Kotlin that prioritizes compile-time guarantees over traditional string-based configuration systems.

### Key Characteristics

- **Language**: Pure Kotlin (Kotlin 2.2.0, JVM target)
- **Build System**: Gradle with Kotlin DSL
- **JVM Target**: Java 17 minimum (tested on 17 and 21)
- **Version**: 0.0.1 (pre-release, preparing for Maven Central)
- **License**: MIT
- **Repository**: https://github.com/amichne/konditional

### Core Philosophy

1. **Type Safety First**: Eliminate runtime errors through compile-time guarantees
2. **Deterministic by Default**: Same inputs always produce same outputs
3. **Zero Dependencies**: Pure Kotlin with minimal external libs (only Moshi for JSON)
4. **Thread-Safe**: Lock-free reads with atomic updates
5. **Extensible**: Custom contexts, rules, and value types

---

## Codebase Structure

```
konditional/
├── src/
│   ├── main/kotlin/io/amichne/konditional/
│   │   ├── context/              # Evaluation context (Context, Platform, Rollout, Version)
│   │   ├── core/                 # Core framework components
│   │   │   ├── types/           # EncodableValue type system
│   │   │   ├── instance/        # Konfig (snapshot) and KonfigPatch
│   │   │   ├── result/          # EvaluationResult, ParseResult, exceptions
│   │   │   └── id/              # HexId and StableId for bucketing
│   │   ├── rules/                # Rule evaluation and targeting
│   │   │   ├── evaluable/       # Evaluable and BaseEvaluable abstractions
│   │   │   └── versions/        # Version range types
│   │   ├── serialization/        # JSON serialization with Moshi
│   │   ├── internal/             # Internal implementation details
│   │   │   ├── builders/        # DSL builder implementations
│   │   │   └── serialization/   # Serialization models and adapters
│   │   └── example/              # Example usage patterns
│   ├── test/kotlin/              # Unit tests (13 test files)
│   └── testFixtures/             # Shared test fixtures
├── docs/                         # 14 markdown documentation files
├── scripts/                      # Build and utility scripts
├── .github/workflows/            # CI/CD pipelines
├── build.gradle.kts              # Build configuration
└── gradle.properties             # Project properties
```

### Key Modules

#### **context/** - Evaluation Context
- `Context.kt`: Base interface requiring locale, platform, appVersion, stableId
- `Platform.kt`: Enum (IOS, ANDROID, WEB, DESKTOP)
- `AppLocale.kt`: Supported locales (EN_US, FR_FR, DE_DE, etc.)
- `Version.kt`: Semantic versioning with comparison
- `Rollout.kt`: Value class enforcing 0-100% rollout with SHA-256 bucketing

#### **core/** - Core Framework
- `Feature.kt`: Sealed interface for type-safe feature flags
- `FeatureModule.kt`: Sealed class for team-scoped isolation
- `ModuleRegistry.kt`: Abstract registry for flag configurations
- `InMemoryModuleRegistry.kt`: Default in-memory implementation
- `Config.kt`: DSL entry point for configuration
- `FlagDefinition.kt`: Flag with default value, rules, and evaluation logic
- `*Feature.kt`: Type aliases (BooleanFeature, StringFeature, IntFeature, DoubleFeature)

#### **rules/** - Advanced Targeting
- `Rule.kt`: Composable rule with evaluation logic
- `ConditionalValue.kt`: Pairs Rule with value
- `Evaluable.kt`, `BaseEvaluable.kt`: Matching conditions + specificity scoring
- `versions/`: Version range types (FullyBound, LeftBound, RightBound, Unbounded)

#### **serialization/** - JSON Persistence
- `SnapshotSerializer.kt`: Main API for Konfig ↔ JSON
- `ModuleSnapshotSerializer.kt`: FeatureModule-scoped serialization
- `Serializer.kt`: Generic interface for custom implementations

---

## Development Workflow

### Setting Up

```bash
# Clone the repository
git clone https://github.com/amichne/konditional.git
cd konditional

# Run tests
./gradlew test

# Build the library
./gradlew assemble

# Run all checks
./gradlew check
```

### Git Branch Strategy

- **main**: Primary development branch
- **Feature branches**: Use descriptive names (e.g., `feature/custom-contexts`, `fix/serialization-bug`)
- **PR workflow**: All changes go through pull requests with CI checks

### CI/CD Pipeline

The project uses GitHub Actions with 3 workflows:

1. **CI** (`.github/workflows/ci.yml`):
    - Runs on: Ubuntu, macOS, Windows
    - Java versions: 17, 21 (matrix testing)
    - Steps: checkout → setup JDK → run tests → build → upload artifacts

2. **Snapshot** (`.github/workflows/snapshot.yml`):
    - On-demand snapshots to GitHub Packages

3. **Release** (`.github/workflows/release.yml`):
    - Full Maven Central publishing with GPG signing

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "io.amichne.konditional.core.ConditionEvaluationTest"

# Run with stacktrace (useful for debugging)
./gradlew test --stacktrace

# Run with test reports
./gradlew test --no-daemon
# Results in: build/reports/tests/
```

---

## Architecture & Design Principles

### Type System

Konditional enforces type safety through sealed class hierarchies:

```kotlin
EncodableValue<T>
├── BooleanEncodeable         (Boolean)
├── StringEncodeable          (String)
├── IntEncodeable            (Int)
├── DecimalEncodeable        (Double)
├── JsonObjectEncodeable<T>  (complex data classes)
└── CustomEncodeable<T, P>   (wrapper types like DateTime, UUID)
```

**Key Principle**: All value types must extend `EncodableValue<T>` to ensure compile-time type safety.

### Module Isolation

Each `FeatureModule` provides compile-time and runtime isolation:

```kotlin
sealed class FeatureModule {
    object Core : FeatureModule()

    sealed class Team : FeatureModule() {
        object Authentication : Team()
        object Payments : Team()
        object Messaging : Team()
        // Add new teams here
    }
}
```

**Benefits**:
- Each module has its own `ModuleRegistry` instance
- Feature type binding prevents cross-module pollution
- Independent configuration and deployment

### Evaluation Pipeline

```
Context → Rule.matches() → specificity() → rollout bucketing → value
```

**Specificity Ordering**: More specific rules automatically take precedence.

**Deterministic Bucketing**:
- SHA-256 hash of (salt + flag key + stable ID)
- Consistent 0-10,000 bucketing space
- Independent per-flag (no cross-contamination)

### Design Patterns

1. **Sealed Hierarchies**: For exhaustive type checking (FeatureModule, EncodableValue, Context)
2. **DSL Builder Pattern**: Fluent configuration with ConfigBuilder, FlagBuilder
3. **TypeState Pattern**: Builders enforce valid state transitions at compile time
4. **Composition Over Inheritance**: Rule composes BaseEvaluable + extension Evaluable
5. **Lock-free Reads**: AtomicReference with atomic updates only (no locks on read path)
6. **Parse, Don't Validate**: EncodableValue restricts valid types at compile time

---

## Code Conventions

### Kotlin Style

- **Style Guide**: Official Kotlin code style (`kotlin.code.style=official` in gradle.properties)
- **Package Structure**: Follow existing package organization (context, core, rules, serialization)
- **Naming Conventions**:
    - Classes: PascalCase
    - Functions: camelCase
    - Constants: SCREAMING_SNAKE_CASE
    - Type parameters: Single uppercase letter (S, T, C, M)

### Type Parameters

Consistent naming across the codebase:

- `S`: EncodableValue type (wraps T)
- `T`: Actual value type (Boolean, String, Int, etc.)
- `C`: Context type (extends Context)
- `M`: FeatureModule type

**Example**:
```kotlin
sealed interface Feature<S : EncodableValue<T>, T : Any, C : Context, M : FeatureModule>
```

### Documentation

- **KDoc**: Use for all public APIs
- **Examples**: Include code examples in KDoc for complex APIs
- **Internal APIs**: Mark with `internal` visibility, document when non-obvious
- **Package Documentation**: Not currently used, but recommended for major packages

### Visibility Modifiers

- **Public**: Only for APIs intended for library users
- **Internal**: For framework implementation details
- **Private**: For internal class details
- **Sealed**: For exhaustive type hierarchies

---

## Testing Guidelines

### Test Structure

All tests use JUnit 5 (Jupiter) with Kotlin test extensions:

```kotlin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExampleTest {
    @Test
    fun `Given context, When condition, Then expected result`() {
        // Arrange
        val expected = "value"

        // Act
        val actual = evaluateFlag()

        // Assert
        assertEquals(expected, actual)
    }
}
```

### Test Naming

Use backtick strings with Given-When-Then format:

```kotlin
@Test
fun `Given rule with 50 percent rollout, When evaluating many users, Then approximately half match`()
```

**Benefits**:
- Natural language descriptions
- Clear test intent
- Easy to read in test reports

### Test Organization

- **Unit tests**: `src/test/kotlin/` (mirrors main package structure)
- **Test fixtures**: `src/testFixtures/` (shared test utilities)
- **Test files**: One test file per class (e.g., `ConditionEvaluationTest.kt` for `Condition.kt`)

### Common Test Patterns

1. **Helper Functions**: Create context helpers for test data
   ```kotlin
   private fun ctx(
       idHex: String,
       locale: AppLocale = AppLocale.EN_US,
       platform: Platform = Platform.IOS,
       version: String = "1.0.0",
   ) = Context(locale, platform, Version.parse(version), StableId.of(idHex))
   ```

2. **Test Enums**: Define test-specific feature enums within test class
   ```kotlin
   enum class TestFlags(override val key: String) :
       StringFeature<Context, FeatureModule.Core> {
       TEST_FLAG("test_flag");
       override val module: FeatureModule.Core get() = FeatureModule.Core
   }
   ```

3. **Property-based Testing**: Use large sample sizes for probabilistic tests
   ```kotlin
   val sampleSize = 5000
   repeat(sampleSize) { i ->
       // Test assertions
   }
   ```

4. **Determinism Tests**: Verify same input produces same output
   ```kotlin
   repeat(100) {
       assertEquals(firstResult, condition.evaluate(ctx(id)))
   }
   ```

### Test Coverage

Current test files (13 total):

- `core/ConditionEvaluationTest.kt` - Flag evaluation logic
- `core/EvaluationResultTest.kt` - Result type handling
- `core/FlagEntryTypeSafetyTest.kt` - Type safety guarantees
- `core/ParseResultTest.kt` - Parsing logic
- `core/FeatureContainerTest.kt` - FeatureContainer delegation and registration
- `rules/BaseRuleGuaranteesTest.kt` - Rule evaluation guarantees
- `rules/RuleMatchingTest.kt` - Rule matching logic
- `rules/versions/VersionRangeTest.kt` - Version range behavior
- `context/ContextPolymorphismTest.kt` - Context polymorphism

**When adding features**: Add corresponding tests in the same package structure.

---

## Build & Publishing

### Gradle Configuration

**Key settings** in `build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "2.2.0"
    `java-test-fixtures`          // Shared test fixtures
    `maven-publish`                // Publishing to Maven
    signing                        // GPG signing
}

kotlin {
    jvmToolchain(17)              // Java 17 toolchain
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")  // Context receivers
    }
}
```

**Dependencies**:
```kotlin
// JSON Serialization (only external dependency)
implementation("com.squareup.moshi:moshi:1.15.0")
implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
implementation("com.squareup.moshi:moshi-adapters:1.15.0")

// Testing
testImplementation(kotlin("test"))
testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
```

### Publishing Targets

1. **GitHub Packages**: Development snapshots
2. **Maven Central**: Official releases (via Sonatype)
3. **Maven Local**: Local development

### Release Process

```bash
# 1. Update version in gradle.properties
VERSION=1.0.0

# 2. Validate release readiness
./gradlew prepareRelease

# 3. Build and test
./gradlew clean build

# 4. Publish to Maven Central (requires credentials)
./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository

# 5. Tag release
git tag v1.0.0
git push origin v1.0.0
```

**Publishing credentials** (required environment variables):
- `OSSRH_USERNAME` and `OSSRH_PASSWORD` (Sonatype)
- `SIGNING_KEY` and `SIGNING_PASSWORD` (GPG signing)
- `GITHUB_TOKEN` (GitHub Packages)

---

## Common Tasks

### Adding a New Feature Type

1. **Define the feature enum**:
   ```kotlin
   enum class MyFeatures(override val key: String) :
       BooleanFeature<Context, FeatureModule.Core> {
       NEW_FEATURE("new_feature");
       override val module: FeatureModule.Core get() = FeatureModule.Core
   }
   ```

2. **Configure the feature**:
   ```kotlin
   FeatureModule.Core.config {
       MyFeatures.NEW_FEATURE with {
           default(false)
           rule {
               platforms(Platform.IOS)
               rollout = Rollout.of(25.0)
           } implies true
       }
   }
   ```

3. **Add tests** in `src/test/kotlin/` following existing patterns

### Adding a Custom Context

1. **Extend Context interface**:
   ```kotlin
   data class EnterpriseContext(
       override val locale: AppLocale,
       override val platform: Platform,
       override val appVersion: Version,
       override val stableId: StableId,
       // Custom fields
       val organizationId: String,
       val subscriptionTier: SubscriptionTier,
   ) : Context
   ```

2. **Use with features**:
   ```kotlin
   enum class EnterpriseFeatures(override val key: String) :
       BooleanFeature<EnterpriseContext, FeatureModule.Core> {
       ADVANCED_ANALYTICS("advanced_analytics");
       override val module: FeatureModule.Core get() = FeatureModule.Core
   }
   ```

3. **Add context-specific tests**

### Adding a New FeatureModule

1. **Add to FeatureModule sealed hierarchy**:
   ```kotlin
   sealed class FeatureModule {
       // ... existing modules ...

       sealed class Team : FeatureModule() {
           // ... existing teams ...
           object Analytics : Team()  // New module
       }
   }
   ```

2. **Use in feature definitions**:
   ```kotlin
   enum class AnalyticsFeatures(override val key: String) :
       BooleanFeature<Context, FeatureModule.Team.Analytics> {
       EVENT_TRACKING("event_tracking");
       override val module get() = FeatureModule.Team.Analytics
   }
   ```

### Extending EncodableValue

For custom value types:

```kotlin
// 1. Define the custom encoder
val dateTimeEncoder = EncodableValue.customString<DateTime> { dateTime ->
    dateTime.toIso8601()
}

// 2. Use in feature configuration
FeatureModule.Core.config {
    MyFeatures.TIMESTAMP with {
        default(DateTime.now())
        encoder = dateTimeEncoder
    }
}
```

---

## Important Gotchas

### 1. Compiler Flag Required

**Issue**: Code uses context receivers (`-Xcontext-parameters`)
**Solution**: Always build with `./gradlew` (configured in build.gradle.kts)
**Don't**: Try to compile without this flag

### 2. Thread Safety

**Safe**:
```kotlin
// Reading flags (lock-free)
val value = feature.evaluate(context)

// Atomic updates
registry.update(newKonfig)
```

**Unsafe**:
```kotlin
// DON'T: Multiple sequential updates without synchronization
registry.update(konfig1)
registry.update(konfig2)  // Might overwrite konfig1
```

### 3. Specificity Ordering

**Rules are automatically sorted by specificity** - order in DSL doesn't matter:

```kotlin
// Both produce the same result (most specific first)
FlagDefinition(
    values = listOf(
        generalRule.targetedBy("general"),
        specificRule.targetedBy("specific"),  // This wins for matching context
    )
)
```

### 4. Rollout Bucketing

**Deterministic per flag**:
- Same user gets consistent experience for a flag
- Different flags can bucket the same user differently
- Bucketing uses: SHA-256(salt + flag key + stable ID)

**Changing salt resets bucketing**:
```kotlin
FlagDefinition(
    salt = "v2",  // New salt = new bucketing
    // ...
)
```

### 5. EncodableValue Type Safety

**Compile-time enforcement**:
```kotlin
// ✅ Correct
enum class StringFlag : Feature<StringEncodeable, String, Context, FeatureModule.Core>

// ❌ Won't compile - type mismatch
enum class StringFlag : Feature<BooleanEncodeable, String, Context, FeatureModule.Core>
```

### 6. Module Registry Isolation

Each `FeatureModule` has its own registry:

```kotlin
// ✅ Correct - same module
FeatureModule.Core.registry.get(CoreFeature.FLAG)

// ❌ Wrong - different modules, won't find flag
FeatureModule.Team.Payments.registry.get(CoreFeature.FLAG)
```

### 7. Version Parsing

**Use `Version.parse()` for strings**:
```kotlin
// ✅ Correct
val version = Version.parse("2.5.0")  // Version(2, 5, 0)

// ✅ Also correct
val version = Version(2, 5, 0)

// ❌ Don't manually parse
val parts = "2.5.0".split(".")  // Use Version.parse() instead
```

### 8. JSON Serialization

**Moshi is the only external dependency**:
- Use `SnapshotSerializer` for Konfig ↔ JSON
- Custom adapters in `internal/serialization/adapters/`
- Don't add other JSON libraries (keep dependencies minimal)

### 9. Test Fixtures

**Shared test utilities** go in `src/testFixtures/`:
```kotlin
// ✅ Reusable test features
testFixtures/kotlin/CommonTestFeatures.kt

// ❌ Don't duplicate in each test file
```

### 10. Documentation Updates

**When changing public APIs**:
1. Update KDoc comments
2. Update relevant docs in `docs/` directory
3. Add examples for new features
4. Update README.md if needed

---

## Resources

### Documentation

- **[README.md](README.md)**: Project overview and quick start
- **[docs/](docs/)**: Comprehensive guides (14 files)
    - `QuickStart.md`: 5-minute introduction
    - `CoreConcepts.md`: Type-safe building blocks
    - `Evaluation.md`: Flag evaluation mechanics
    - `Builders.md`: DSL mastery
    - `Rules.md`: Advanced targeting
    - `Context.md`: Custom contexts
    - `Serialization.md`: JSON handling
    - `RegistryAndConcurrency.md`: Thread-safety
    - `Architecture.md`: System design
    - `WhyTypeSafety.md`: Type safety benefits
    - `ErrorPrevention.md`: Error elimination
    - `Migration.md`: Migration from string-based systems

### External Links

- **Repository**: https://github.com/amichne/konditional
- **Kotlin Docs**: https://kotlinlang.org/docs/home.html
- **Gradle**: https://docs.gradle.org/
- **Moshi**: https://github.com/square/moshi

---

## Quick Reference

### File Locations

| Purpose | Location |
|---------|----------|
| Core API | `src/main/kotlin/io/amichne/konditional/core/` |
| Context types | `src/main/kotlin/io/amichne/konditional/context/` |
| Rule evaluation | `src/main/kotlin/io/amichne/konditional/rules/` |
| Serialization | `src/main/kotlin/io/amichne/konditional/serialization/` |
| Tests | `src/test/kotlin/` |
| Test fixtures | `src/testFixtures/` |
| Documentation | `docs/` |
| Build config | `build.gradle.kts`, `gradle.properties` |
| CI/CD | `.github/workflows/` |

### Common Commands

```bash
# Build
./gradlew assemble

# Test
./gradlew test

# Clean build
./gradlew clean build

# Publish to Maven Local
./gradlew publishToMavenLocal

# Generate checksums
./gradlew assemble && cd build/libs && sha256sum *.jar
```

### Key Interfaces

```kotlin
// Feature definition
Feature<S : EncodableValue<T>, T : Any, C : Context, M : FeatureModule>

// Context (required fields)
interface Context {
    val locale: AppLocale
    val platform: Platform
    val appVersion: Version
    val stableId: StableId
}

// Rule evaluation
Rule<C : Context>

// Module registry
ModuleRegistry
```

---

## Contributing Guidelines

When contributing to Konditional:

1. **Follow existing conventions**: Match the style and patterns in the codebase
2. **Add tests**: All new features require corresponding tests
3. **Update documentation**: Keep docs/ in sync with code changes
4. **Type safety**: Never compromise compile-time guarantees
5. **Zero dependencies**: Don't add new dependencies without discussion
6. **Thread safety**: Ensure new code is thread-safe
7. **Determinism**: All evaluation must be deterministic

---

**Last Updated**: 2025-11-15
**Codebase Version**: 0.0.1
**Kotlin Version**: 2.2.0
**Java Target**: 17+
