# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Kontracts Module**: Extracted JSON Schema DSL functionality into standalone `kontracts` submodule
  - Type-safe JSON Schema DSL with compile-time guarantees
  - Type-inferred property schemas using Kotlin context receivers
  - Custom type mapping for domain types
  - Runtime JSON value validation
  - OpenAPI 3.1 compatible schemas
  - Zero dependencies (Kotlin stdlib only)
- Comprehensive documentation in `kontracts/README.md`
- Multi-module project structure

### Changed
- **BREAKING**: JSON Schema types moved from `io.amichne.konditional.core.types.json` to `io.amichne.kontracts.schema` and `io.amichne.kontracts.value`
  - `JsonSchema` → `io.amichne.kontracts.schema.JsonSchema`
  - `JsonValue` → `io.amichne.kontracts.value.JsonValue`
- **BREAKING**: JSON Schema DSL moved from `io.amichne.konditional.core.types.json` and `io.amichne.konditional.core.dsl` to `io.amichne.kontracts.dsl`
  - `schemaRoot { }` → `io.amichne.kontracts.dsl.schemaRoot`
  - `jsonObject { }` → `io.amichne.kontracts.dsl.jsonObject`
  - `buildJsonObject { }` → `io.amichne.kontracts.dsl.buildJsonObject`
  - Type mapping functions (`asString`, `asInt`, etc.) → `io.amichne.kontracts.dsl.*`
- Konditional now depends on kontracts submodule for JSON Schema functionality

### Migration Guide

#### Update Imports

**Before:**
```kotlin
import io.amichne.konditional.core.types.json.JsonSchema
import io.amichne.konditional.core.types.json.JsonValue
import io.amichne.konditional.core.types.json.schemaRoot
import io.amichne.konditional.core.dsl.jsonObject
```

**After:**
```kotlin
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.value.JsonValue
import io.amichne.kontracts.dsl.schemaRoot
import io.amichne.kontracts.dsl.jsonObject
```

#### Automated Migration

For bulk updates, use these sed commands:

```bash
# Update JsonSchema imports
find src -type f -name "*.kt" -exec sed -i '' \
  's|io\.amichne\.konditional\.core\.types\.json\.JsonSchema|io.amichne.kontracts.schema.JsonSchema|g' {} \;

# Update JsonValue imports
find src -type f -name "*.kt" -exec sed -i '' \
  's|io\.amichne\.konditional\.core\.types\.json\.JsonValue|io.amichne.kontracts.value.JsonValue|g' {} \;

# Update DSL imports
find src -type f -name "*.kt" -exec sed -i '' \
  's|io\.amichne\.konditional\.core\.types\.json|io.amichne.kontracts.dsl|g' {} \;

find src -type f -name "*.kt" -exec sed -i '' \
  's|io\.amichne\.konditional\.core\.dsl\.(jsonObject|buildJsonObject)|io.amichne.kontracts.dsl.\1|g' {} \;
```

#### Gradle Configuration

No changes required to build.gradle.kts - kontracts is automatically included as a dependency through the multi-module setup.

### Removed
- Old JSON Schema files from `src/main/kotlin/io/amichne/konditional/core/types/json/`:
  - `JsonSchema.kt` (moved to kontracts)
  - `JsonValue.kt` (moved to kontracts)
  - `JsonSchemaBuilders.kt` (moved to kontracts)
- Old DSL builder files from `src/main/kotlin/io/amichne/konditional/core/dsl/`:
  - `JsonSchemaBuilder.kt` (moved to kontracts)
  - `JsonObjectSchemaBuilder.kt` (moved to kontracts)
  - `JsonFieldSchemaBuilder.kt` (moved to kontracts)
  - `JsonObjectBuilder.kt` (moved to kontracts)

### Notes
- All functionality remains available - this is purely a reorganization
- Tests pass with 100% compatibility after import updates
- Kontracts is designed for eventual extraction as standalone library
- No API changes besides package paths

## [0.0.1] - Previous Release
<!-- Previous version history will go here -->
