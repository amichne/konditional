# CLAUDE.md - AI Assistant Guide for Konditional

**Last Updated:** 2025-12-29
**Version:** 0.0.1
**Kotlin Version:** 2.2.20
**JVM Target:** 21

This document provides AI assistants with essential context for working on the Konditional codebase.

---

## Project Overview

**Konditional** is a type-safe, deterministic feature flags library for Kotlin that prevents runtime errors through compile-time guarantees. Unlike traditional string-based feature flag systems, Konditional uses strongly-typed property delegation to make typos, type coercion failures, and configuration drift compile-time errors instead of production incidents.

**Key Value Proposition:**
- **Compile-time correctness**: Typos don't compile, types are guaranteed
- **Typed values**: Not just booleans—strings, ints, doubles, enums, custom types
- **Deterministic ramp-ups**: SHA-256 bucketing ensures same user → same bucket, always
- **Explicit boundaries**: Parse JSON configuration with validation; reject invalid updates

**Repository:** https://github.com/amichne/konditional
**License:** MIT

---

## Quick Start for AI Assistants

### Essential Reading Order

1. **This document (CLAUDE.md)** - Start here for codebase orientation
2. **CONTRIBUTING.md** - Detailed contribution guidelines and architecture
3. **llm-docs/** directory - Domain-specific prompts for specialized tasks
4. **docusaurus/docs/** - Comprehensive user-facing documentation

### Critical Build Requirements

**REQUIRED:** Code uses Kotlin context receivers (`-Xcontext-parameters` compiler flag).
Always use `./gradlew` for builds—this flag is configured in `build.gradle.kts`.

```bash
# Correct
./gradlew test

# Will fail
kotlinc src/main/kotlin/...  # Missing context receivers flag
```

---

## Codebase Architecture

### Module Structure

```
konditional/
├── src/main/kotlin/io/amichne/konditional/    # Core library
├── kontracts/                                  # JSON Schema DSL (submodule)
├── ktor-demo/                                  # Example Ktor integration
├── docusaurus/                                 # Documentation site
└── llm-docs/                                   # LLM-specific documentation
```

### Core Package Organization

```
io/amichne/konditional/
├── api/                    # Public utilities (ShadowEvaluation, RolloutBucketing)
├── configstate/            # Configuration state and UI descriptors
├── context/                # Evaluation context (Platform, AppLocale, Version, RampUp)
│   └── axis/              # Custom context dimensions
├── core/                   # Core framework
│   ├── dsl/               # DSL builders
│   ├── evaluation/        # Evaluation logic
│   ├── features/          # Feature type implementations
│   ├── id/                # StableId, HexId for bucketing
│   ├── instance/          # Configuration snapshots and patches
│   ├── ops/               # Operations (kill switches, etc.)
│   ├── registry/          # Feature registries
│   ├── result/            # Result types (EvaluationResult, ParseResult)
│   └── types/             # Type system
├── internal/               # Internal implementation
│   ├── builders/          # Internal DSL builders
│   └── serialization/     # Moshi adapters and models
├── openapi/                # OpenAPI schema generation
├── rules/                  # Rule evaluation and targeting
│   ├── evaluable/         # Evaluable abstractions
│   └── versions/          # Version range types
├── serialization/          # JSON serialization (SnapshotSerializer)
└── values/                 # Value types (FeatureId, etc.)
```

**Navigation Tip:** When looking for functionality:
- **Public API**: Start in `api/`, `core/Namespace.kt`, `core/FlagDefinition.kt`
- **Evaluation logic**: Check `core/evaluation/` and `rules/`
- **Serialization**: Look in `serialization/` and `internal/serialization/`
- **Context types**: Found in `context/`

---

## Type System & Core Concepts

### Namespace Pattern

Features are defined as delegated properties on `Namespace` objects:

```kotlin
object AppFlags : Namespace("app") {
    val darkMode by boolean(default = false) {
        rule(true) { platforms(Platform.IOS) }
        rule(true) { rampUp { 50.0 } }
    }

    val apiEndpoint by string(default = "https://api.example.com") {
        rule("https://api-staging.example.com") {
            platforms(Platform.WEB)
        }
    }
}
```

**Key Points:**
- Each namespace has its own registry and configuration lifecycle
- Flags are compile-time properties (typos don't compile)
- Evaluation returns the declared type, never null
- Defaults are required—evaluation is total (no exceptions)

### Supported Value Types

```kotlin
boolean(default = false)              // Boolean
string(default = "value")             // String
integer(default = 42)                 // Int
double(default = 3.14)                // Double
enum<Theme>(default = Theme.LIGHT)    // Enum types
custom<T>(default = T())              // Custom types implementing KotlinEncodeable
```

### Context Requirements

All evaluations require a `Context` with these fields:

```kotlin
interface Context {
    val locale: AppLocale         // UNITED_STATES, FRANCE, etc.
    val platform: Platform        // IOS, ANDROID, WEB
    val appVersion: Version       // Semantic version
    val stableId: StableId        // For deterministic bucketing
}
```

**Custom Contexts:** Extend `Context` with additional fields for domain-specific targeting.

### Rule Evaluation Model

```
Context → Rule.matches() → specificity() → rollout bucketing → value
```

**Specificity Ordering:**
- Rules are automatically sorted by specificity (most specific first)
- Order in DSL doesn't matter—specificity determines precedence
- More criteria = higher specificity

**Deterministic Bucketing:**
- SHA-256 hash of `(salt + flag key + stable ID)`
- Consistent 0-10,000 bucketing space
- Same user + same flag → same bucket across evaluations
- Changing `salt` redistributes buckets (useful for re-running experiments)

---

## Development Workflows

### Common Commands

```bash
# Build and test
./gradlew build                 # Full build with tests
./gradlew test                  # Run tests only
./gradlew clean build           # Clean build

# Code quality
./gradlew detekt                # Static analysis
./gradlew detektBaseline        # Generate baseline for existing issues

# Publishing
./gradlew publishToMavenLocal   # Publish to local Maven
./gradlew publishToSonatype     # Publish to Maven Central (requires credentials)

# Documentation
make docs-install               # Install Docusaurus dependencies
make docs-serve                 # Serve docs locally (http://localhost:3000)
make docs-build                 # Build static docs

# Makefile shortcuts
make build                      # Equivalent to ./gradlew build
make test                       # Equivalent to ./gradlew test
make clean                      # Clean build artifacts
```

### File Locations Reference

| Purpose | Location |
|---------|----------|
| Core API | `src/main/kotlin/io/amichne/konditional/core/` |
| Context types | `src/main/kotlin/io/amichne/konditional/context/` |
| Rule evaluation | `src/main/kotlin/io/amichne/konditional/rules/` |
| Serialization | `src/main/kotlin/io/amichne/konditional/serialization/` |
| Tests | `src/test/kotlin/` |
| Test fixtures | `src/testFixtures/` |
| Documentation | `docusaurus/docs/` |
| LLM docs | `llm-docs/` |
| Build config | `build.gradle.kts`, `gradle.properties` |
| CI/CD | `.github/workflows/` |

### Testing Conventions

**Test Structure:**
- Use JUnit 5 (Jupiter) with Kotlin test extensions
- One test file per class (e.g., `ConditionEvaluationTest.kt` for `Condition.kt`)
- Tests mirror main package structure

**Naming Convention:**
```kotlin
@Test
fun `Given context with iOS platform, When evaluating rule, Then returns true`() {
    // Arrange
    val ctx = createContext(platform = Platform.IOS)

    // Act
    val result = rule.evaluate(ctx)

    // Assert
    assertTrue(result)
}
```

**Key Test Patterns:**
1. **Helper functions** for context creation
2. **Property-based testing** with large sample sizes for probabilistic tests
3. **Determinism tests** to verify same input → same output
4. **Adversarial tests** in `src/test/kotlin/io/amichne/konditional/adversarial/`

### Git & CI/CD

**Branch Strategy:**
- `main`: Primary development branch
- Feature branches: `feature/description` or `fix/description`
- All changes via pull requests with CI checks

**CI Workflows** (`.github/workflows/`):
1. **ci.yml**: Multi-platform testing (Ubuntu, macOS, Windows) with Java 17, 21
2. **snapshot.yml**: Snapshot publishing to GitHub Packages
3. **release.yml**: Maven Central publishing with GPG signing
4. **docs-docusaurus.yml**: Documentation deployment

**Required for Releases:**
- Environment variables: `OSSRH_USERNAME`, `OSSRH_PASSWORD`, `SIGNING_KEY`, `SIGNING_PASSWORD`

---

## Critical Implementation Details

### Thread Safety Guarantees

**Safe Operations:**
```kotlin
// Lock-free reads
val value = AppFlags.darkMode.evaluate(context)

// Atomic updates
AppFlags.load(newConfiguration)
```

**Implementation:**
- `Namespace.load()` uses `AtomicReference.set()` for atomic swaps
- Evaluation reads snapshot without blocking writers
- No locks on read path
- Multiple threads can evaluate concurrently during updates

**Unsafe:**
```kotlin
// DON'T: Sequential updates without synchronization
registry.update(config1)
registry.update(config2)  // May overwrite config1
```

### JSON Serialization Boundary

**Parse, Don't Validate Pattern:**

```kotlin
// Loading configuration
val json = fetchRemoteConfig()

when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> AppFlags.load(result.value)
    is ParseResult.Failure -> {
        // Invalid JSON rejected, last-known-good remains active
        logError("Config parse failed: ${result.error.message}")
    }
}
```

**Critical Rule:** Deserialization requires namespace initialization first (so features are registered).

**Incremental Updates:**
```kotlin
val currentConfig = AppFlags.configuration
when (val result = SnapshotSerializer.applyPatchJson(currentConfig, patchJson)) {
    is ParseResult.Success -> AppFlags.load(result.value)
    is ParseResult.Failure -> logError("Patch failed: ${result.error.message}")
}
```

### Kontracts Submodule

**Purpose:** Type-safe JSON Schema DSL extracted into standalone module.

**Breaking Change (0.0.1):**
```kotlin
// OLD (don't use)
import io.amichne.konditional.core.types.json.JsonSchema

// NEW (correct)
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.value.JsonValue
import io.amichne.kontracts.dsl.schemaRoot
```

**Location:** `kontracts/` directory
**Dependencies:** Zero (Kotlin stdlib only)
**Future:** Will be extracted as standalone library

---

## Code Conventions & Style

### Kotlin Style

- **Style Guide:** Official Kotlin code style (`kotlin.code.style=official`)
- **Package naming:** Lowercase, no underscores
- **Naming conventions:**
  - Classes: PascalCase
  - Functions: camelCase
  - Constants: SCREAMING_SNAKE_CASE
  - Type parameters: Single uppercase (`T`, `C`, `M`)

### Type Parameter Conventions

Consistent across the codebase:
- `T`: Actual value type (Boolean, String, Int, etc.)
- `C`: Context type (extends Context)
- `M`: Namespace type

**Example:**
```kotlin
sealed interface Feature<T : Any, C : Context, out M : Namespace>
```

### Visibility Modifiers

- **Public:** Only for APIs intended for library users
- **Internal:** Framework implementation details
- **Private:** Internal class details
- **Sealed:** Exhaustive type hierarchies

### Documentation Requirements

- **KDoc:** Required for all public APIs
- **Examples:** Include code examples for complex APIs
- **Internal APIs:** Document when non-obvious, mark `internal`
- **When changing public APIs:** Update KDoc + relevant docs in `docusaurus/docs/`

---

## Important Gotchas & Constraints

### 1. Context Receivers Required

**Issue:** Code requires `-Xcontext-parameters` compiler flag
**Solution:** Always use `./gradlew` (configured in `build.gradle.kts`)
**Don't:** Try to compile manually without this flag

### 2. Specificity Auto-Sorting

Rules are automatically sorted by specificity—order in DSL doesn't matter:

```kotlin
// Both produce identical results
rule("general") { platforms(Platform.WEB) }
rule("specific") { platforms(Platform.WEB); locales(AppLocale.UNITED_STATES) }
// "specific" wins for matching context (2 criteria > 1 criterion)
```

### 3. Rollout Bucketing Details

- Same user gets consistent experience for a flag
- Different flags can bucket the same user differently
- Bucketing formula: `SHA-256(salt + flag key + stable ID)`
- **Changing salt resets bucketing** (use for re-running experiments)

### 4. Version Parsing

```kotlin
// ✅ Correct
val version = Version.parse("2.5.0")
val version = Version(2, 5, 0)

// ❌ Don't manually parse
val parts = "2.5.0".split(".")  // Use Version.parse() instead
```

### 5. No Null Returns

Flag evaluation is **total**:
- Always returns declared type
- Never returns null
- If no rule matches, default is returned
- No exceptions thrown during evaluation

### 6. Dependencies

**Minimal external dependencies:**
- Moshi (JSON serialization only)
- Kontracts submodule (internal)
- Kotlin stdlib + reflection

**Don't add:**
- Other JSON libraries (keep Moshi as single JSON dependency)
- Heavy frameworks (keep library lightweight)

### 7. Namespace Isolation

Each namespace has independent:
- Registry
- Configuration lifecycle
- Serialization

```kotlin
sealed class AppDomain(id: String) : Namespace(id) {
    data object Payments : AppDomain("payments") {
        val applePay by boolean<Context>(default = false)
    }
}

// Evaluates against Payments namespace registry
val value = AppDomain.Payments.applePay.evaluate(context)

// For testing/shadowing, pass registry explicitly
val value = AppDomain.Payments.applePay.evaluate(context, registry = testRegistry)
```

---

## Common Task Patterns

### Adding a New Feature

```kotlin
object MyFeatures : Namespace("my-features") {
    val newFeature by boolean(default = false) {
        rule(true) {
            platforms(Platform.IOS)
            rampUp { 25.0 }
        }
    }
}

// Usage
val enabled = MyFeatures.newFeature.evaluate(ctx)
```

### Custom Context

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    // Custom fields
    val organizationId: String,
    val tier: SubscriptionTier,
) : Context

// Use with features
object EnterpriseFeatures : Namespace("enterprise") {
    val advancedAnalytics by boolean<EnterpriseContext>(default = false) {
        rule(true) {
            extension { ctx -> ctx.tier == SubscriptionTier.PREMIUM }
        }
    }
}
```

### Custom Structured Types

```kotlin
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val backoffMs: Double = 1000.0,
) : KotlinEncodeable<ObjectSchema> {
    override val schema = schemaRoot {
        ::maxAttempts of { minimum = 1 }
        ::backoffMs of { minimum = 0.0 }
    }
}

object PolicyFlags : Namespace("policy") {
    val retryPolicy by custom(default = RetryPolicy()) {
        rule(RetryPolicy(maxAttempts = 5, backoffMs = 2000.0)) {
            platforms(Platform.WEB)
        }
    }
}
```

**Note:** Custom types use reflection for JSON decoding—keep constructor parameter names stable.

---

## LLM-Specific Documentation

The `llm-docs/` directory contains domain-specific prompts for specialized tasks:

| Domain | Use When | File |
|--------|----------|------|
| **Public API** | Documenting DSL, writing examples, API reference | `domains/01-public-api.md` |
| **Internal Semantics** | Explaining evaluation logic, bucketing, specificity | `domains/02-internal-semantics.md` |
| **Type Safety Theory** | Justifying compile-time guarantees, technical briefs | `domains/03-type-safety-theory.md` |
| **Reliability Guarantees** | Thread-safety, determinism, atomicity | `domains/04-reliability-guarantees.md` |
| **Configuration Integrity** | Remote config, JSON serialization, hot-reload | `domains/05-configuration-integrity.md` |
| **Kontracts** | JSON Schema DSL documentation | `domains/06-kontracts.md` |
| **Critical Evaluation** | Production-readiness assessment | `domains/07-critical-evaluation.md` |

**Usage Pattern:**
1. Identify which domain your task falls under
2. Copy relevant prompt from `llm-docs/domains/`
3. Append context from `llm-docs/context/` if needed
4. Include specific code/questions/constraints

---

## Design Principles

### Parse, Don't Validate

Invalid configurations are rejected at the boundary—they never corrupt runtime state:

```kotlin
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> load(result.value)
    is ParseResult.Failure -> {
        // Bad config rejected, last-known-good stays active
        logError(result.error.message)
    }
}
```

### Composition Over Inheritance

Rules compose `BaseEvaluable` + extension `Evaluable` rather than deep hierarchies.

### Type State Pattern

Builders enforce valid state transitions at compile time—invalid configurations don't compile.

### Lock-Free Reads

`AtomicReference` for updates, no locks on read path. Multiple threads can evaluate concurrently.

### Sealed Hierarchies

Exhaustive type checking for feature variants, contexts, results—compiler enforces completeness.

---

## Related Documentation

### Internal Documentation
- **CONTRIBUTING.md**: Comprehensive contribution guide (codebase structure, architecture, testing)
- **CHANGELOG.md**: Version history and migration guides
- **docusaurus/docs/**: User-facing documentation site
- **llm-docs/**: LLM-specific domain prompts
- **kontracts/README.md**: JSON Schema DSL documentation

### External Resources
- **Repository**: https://github.com/amichne/konditional
- **Kotlin Docs**: https://kotlinlang.org/docs/
- **Gradle Docs**: https://docs.gradle.org/
- **Moshi**: https://github.com/square/moshi

---

## Working with This Codebase

### Before Making Changes

1. **Read this document** to understand architecture and conventions
2. **Review CONTRIBUTING.md** for detailed guidelines
3. **Check llm-docs/** for domain-specific context
4. **Run tests** to ensure baseline passes: `./gradlew test`
5. **Review related code** in the same package before modifying

### Making Changes

1. **Follow existing patterns** in the package you're modifying
2. **Maintain type safety** - don't weaken compile-time guarantees
3. **Add tests** for new functionality (mirror package structure)
4. **Update documentation** if changing public APIs
5. **Run detekt** before committing: `./gradlew detekt`

### Code Review Checklist

- [ ] Tests pass: `./gradlew test`
- [ ] Detekt passes: `./gradlew detekt`
- [ ] No new external dependencies (unless justified)
- [ ] Type safety maintained
- [ ] Thread safety preserved
- [ ] Public APIs documented with KDoc
- [ ] Tests added for new functionality
- [ ] Documentation updated if API changed

### When Stuck

1. **Search similar patterns** in the codebase (Grep, Glob tools)
2. **Check test files** for usage examples
3. **Review llm-docs/** domain prompts for context
4. **Read CONTRIBUTING.md** architecture section
5. **Check docusaurus/docs/** for conceptual explanations

---

## Key Files to Understand

**Core Framework:**
- `core/Namespace.kt` - Namespace abstraction and delegation
- `core/FlagDefinition.kt` - Flag definition with rules
- `core/evaluation/` - Evaluation logic
- `serialization/SnapshotSerializer.kt` - JSON serialization

**Type System:**
- `core/types/` - Value type system
- `values/FeatureId.kt` - Feature identification

**Context:**
- `context/Context.kt` - Context interface
- `context/Platform.kt`, `context/AppLocale.kt` - Standard dimensions
- `context/RampUp.kt` - Rollout bucketing

**Rules:**
- `rules/Rule.kt` - Rule evaluation
- `rules/ConditionalValue.kt` - Criteria-to-value mappings
- `rules/evaluable/` - Evaluable abstractions

**Serialization:**
- `serialization/SnapshotSerializer.kt` - Main serialization API
- `internal/serialization/` - Moshi adapters and models

**Testing:**
- `src/test/kotlin/io/amichne/konditional/core/` - Core tests
- `src/test/kotlin/io/amichne/konditional/adversarial/` - Attack surface tests

---

## Version History

**Current Version:** 0.0.1 (pre-release)

**Recent Changes:**
- Kontracts module extraction (JSON Schema DSL)
- Breaking change: Package reorganization (`io.amichne.konditional.core.types.json` → `io.amichne.kontracts`)
- Multi-module project structure

See **CHANGELOG.md** for detailed version history and migration guides.

---

## Contact & Resources

**Maintainer:** Austin Michne (@amichne)
**Repository:** https://github.com/amichne/konditional
**Issues:** https://github.com/amichne/konditional/issues
**License:** MIT

---

**End of CLAUDE.md**
