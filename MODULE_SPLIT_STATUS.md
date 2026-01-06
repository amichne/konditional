# Module Split Implementation Status

## Summary
Attempted to split the monolithic `core` module into 4 focused modules as outlined in `prompt.md`. The module structure has been created, but circular dependencies and internal type access issues prevent immediate compilation.

## What Was Completed

### 1. Module Structure Created
- ✅ Created `konditional-core/` with build.gradle.kts
- ✅ Created `konditional-serialization/` with build.gradle.kts
- ✅ Created `konditional-runtime/` with build.gradle.kts
- ✅ Created `konditional-observability/` with build.gradle.kts
- ✅ Added module entries to `settings.gradle.kts` (currently commented out)

### 2. Module Dependencies Defined
- `konditional-core`: Depends on kontracts, Moshi (minimal)
- `konditional-serialization`: Depends on konditional-core, kontracts, Moshi, config-metadata
- `konditional-runtime`: Depends on konditional-core, konditional-serialization, coroutines
- `konditional-observability`: Depends on konditional-core, konditional-runtime

### 3. Files Copied to Modules
- Copied full source tree to konditional-core
- Removed serialization/runtime/observability-specific files from konditional-core
- Copied serialization-related files to konditional-serialization
- Copied registry/lifecycle files to konditional-runtime
- Copied hooks/metrics/shadow files to konditional-observability

### 4. API Improvements
- Added factory methods to `ParseResult` (success/failure)
- Added factory method to `ParseError` (featureNotFound)
- This allows external modules to construct these types without accessing internal constructors

## Blocker: Circular Dependencies

### The Core Issue
The module split encounters circular dependencies because:

1. **konditional-core contains Feature/Context/Namespace** which need:
   - `NamespaceRegistry` (interface needs Configuration, RegistryHooks)
   - `Configuration` types (but these should be in serialization)
   - `Metrics` types (but these should be in observability)
   - `Axis` types (but these should be in runtime)

2. **konditional-serialization needs**:
   - Internal types from konditional-core (ConditionalValue, BasePredicate, etc.)
   - These are marked `internal` so can't be accessed from another module

3. **konditional-runtime needs**:
   - Everything from core
   - Serialization for lifecycle operations

4. **konditional-observability needs**:
   - Core evaluation types
   - Runtime for NamespaceRegistry

### Attempted Solutions
1. ✅ Kept Configuration, FlagDefinition, Metrics in konditional-core (they're part of core API surface)
2. ✅ Added public factory methods for internal constructors
3. ❌ Could not resolve: serialization needs access to internal core types

## Recommended Path Forward

### Option 1: Incremental Migration (Recommended)
Keep current monolithic `core` module working while incrementally extracting:

**Phase 1**: Extract observability (least coupled)
1. Create `konditional-observability` with RegistryHooks, MetricsCollector, Shadow APIs
2. Make `core` depend on `konditional-observability`
3. Add `@Deprecated` annotations in core pointing to new module

**Phase 2**: Extract serialization
1. Move ConfigurationSnapshotCodec, NamespaceSnapshotLoader to `konditional-serialization`
2. Keep Configuration types in core but mark constructors as internal
3. Serialization module provides codecs that operate on core types

**Phase 3**: Extract runtime
1. Move InMemoryNamespaceRegistry, lifecycle ops to `konditional-runtime`
2. Keep NamespaceRegistry interface in core

**Phase 4**: Finalize konditional-core
1. Rename current `core` to `konditional-all` or `konditional-legacy`
2. Create new `konditional-core` with minimal surface
3. Gradually deprecate `konditional-all`

### Option 2: Break Circular Dependencies Through Refactoring
Requires significant code changes:

1. **Extract interfaces to konditional-core**:
   - NamespaceRegistry (interface only)
   - Minimal Metrics interface
   - Configuration view types (interfaces)

2. **Move implementations to specialized modules**:
   - InMemoryNamespaceRegistry → konditional-runtime
   - Full Metrics types → konditional-observability
   - Configuration data classes → konditional-serialization

3. **Use dependency inversion**:
   - Core depends on abstractions only
   - Specialized modules provide implementations

This requires:
- Refactoring Configuration to use interfaces/views
- Splitting Metrics into interface + implementation
- Careful management of internal vs public APIs

## Files Created

### Module Build Files
- `konditional-core/build.gradle.kts`
- `konditional-serialization/build.gradle.kts`
- `konditional-runtime/build.gradle.kts`
- `konditional-observability/build.gradle.kts`

### Module Source Structure
- `konditional-core/src/main/kotlin/` - Full copy of core sources (trimmed)
- `konditional-serialization/src/main/kotlin/` - Serialization-related files
- `konditional-runtime/src/main/kotlin/` - Registry and lifecycle files
- `konditional-observability/src/main/kotlin/` - Hooks, metrics, shadow evaluation

### Configuration Files
- `konditional-core/detekt.yml` + baseline
- `konditional-serialization/detekt.yml` + baseline
- `konditional-runtime/detekt.yml` + baseline
- `konditional-observability/detekt.yml` + baseline

## Current State

- ✅ Original `core` module compiles and passes all tests
- ✅ `make check` passes (with new modules commented out)
- ❌ New modules commented out in `settings.gradle.kts` due to compilation errors
- ❌ Serialization module cannot access internal types from core
- ❌ Tests not migrated to new modules

## Compilation Errors to Fix

If uncommenting the new modules, expect errors like:
```
e: Cannot access 'data class ConditionalValue': it is internal in file
e: Cannot access 'constructor(value: T): ParseResult.Success': it is internal
e: Cannot access 'fun JsonSchema<*>.asObjectSchema()': it is internal in file
```

These stem from:
1. Internal types in konditional-core that serialization needs
2. Missing factory methods for some internal constructors
3. Mismatch between what should be internal vs public

## Next Steps

1. **Decision Required**: Choose Option 1 (incremental) or Option 2 (refactor)

2. **If Option 1 (Incremental)**:
   - Start with observability extraction (standalone)
   - Keep core as source of truth
   - Gradually deprecate symbols

3. **If Option 2 (Refactor)**:
   - Design interface layer for Configuration
   - Extract all interfaces to konditional-core
   - Implement in specialized modules
   - Significant code changes required

4. **Update Documentation**:
   - Revise public API surface docs to reflect new module boundaries
   - Update quick-start guides to show which module to depend on
   - Add migration guide for users

## Lessons Learned

1. **Circular dependencies are hard**: The current architecture has tight coupling that requires careful untangling

2. **Internal visibility is a barrier**: Kotlin's `internal` modifier prevents module-level access, requiring either:
   - Making types public (expands API surface)
   - Adding factory methods (adds boilerplate)
   - Keeping types in same module (prevents split)

3. **Tests complicate migration**: Tests often use internal APIs and need careful migration

4. **Incremental is safer**: Full module extraction in one step is risky; incremental with deprecation is better

## Related Files
- `prompt.md` - Original requirements
- `settings.gradle.kts` - Module registration (new modules commented out)
- `core/build.gradle.kts` - Original monolithic module (still works)
