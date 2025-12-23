# Context: Konditional Configuration Integrity

You are documenting the configuration loading, validation, and hot-reload system. Your audience is engineers implementing remote configuration, CI/CD pipelines, or debugging configuration issues.

## Scope

Cover the full configuration lifecycle:

- **Exporting configuration**: `SnapshotSerializer.serialize()` and snapshot structure
- **Loading configuration**: `SnapshotSerializer.fromJson()` and validation
- **Applying patches**: `SnapshotSerializer.applyPatchJson()` for incremental updates
- **Error handling**: `ParseResult.Success | ParseResult.Failure` pattern
- **Hot-reload mechanics**: How `Namespace.load()` applies updates at runtime
- **Schema validation**: What validation occurs during deserialization

## Configuration Export

Document the export process:

```kotlin
val json = SnapshotSerializer.serialize(Namespace.Global.configuration())
```

- What is included in the snapshot? (Features, rules, defaults, salts)
- What is the JSON schema structure?
- Is the output deterministic? (Same config → same JSON, always?)
- Are there options for pretty-printing, minification?

## Configuration Loading

Document the load process:

```kotlin
val json = File("flags.json").readText()
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> Namespace.Global.load(result.value)
    is ParseResult.Failure -> handleError(result.error)
}
```

### Validation Performed
- JSON syntax validation
- Schema structure validation
- Type checking (string where boolean expected?)
- Unknown key handling (ignored? error? logged?)
- Missing required field handling

### What Can Fail
- Malformed JSON → `ParseResult.Failure`
- Schema mismatch → `ParseResult.Failure`
- Type mismatch → `ParseResult.Failure`
- Unknown feature keys → (Document behavior: ignored? warning?)

## Incremental Patches

Document the patch application process:

```kotlin
when (val result = SnapshotSerializer.applyPatchJson(currentConfig, patchJson)) {
    is ParseResult.Success -> Namespace.Global.load(result.value)
    is ParseResult.Failure -> handleError(result.error)
}
```

- What patch format is supported? (JSON Merge Patch? Custom?)
- Can patches add new features? Remove features? Modify rules?
- How are conflicts resolved?

## Error Handling Pattern

Document the `ParseResult` sealed class:

```kotlin
sealed class ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>()
    data class Failure(val error: ParseError) : ParseResult<Nothing>()
}
```

- What information does `ParseError` contain?
- How should callers handle failures? (Log and continue? Abort? Retry?)
- Is there a way to get partial results on failure?

## Hot-Reload Semantics

Document what happens when configuration is updated at runtime:

```kotlin
Namespace.Global.load(newConfiguration)
```

- Is the update atomic? (All-or-nothing?)
- What happens to evaluations in progress?
- Is there a "rollback" mechanism?
- How do multiple rapid updates behave?

## Integration with Type Safety

Document the trust boundary:

- **Statically-defined features**: Types enforced at compile time
- **JSON-loaded configurations**: Types validated at parse time
- **The boundary**: `ParseResult.Success` means types are correct; `Failure` means they're not

### What JSON Loading Can Validate
- Value types match declared feature types
- Rule structures are well-formed
- Required fields are present

### What JSON Loading Cannot Validate
- Semantic correctness (is 50% the right rollout?)
- Business logic (should iOS users see this?)
- Feature key existence (if JSON references undefined feature, what happens?)

## Remote Configuration Patterns

Document common integration patterns:

### Polling
```kotlin
// Pseudocode for polling pattern
while (running) {
    val json = fetchFromServer()
    when (val result = SnapshotSerializer.fromJson(json)) {
        is ParseResult.Success -> Namespace.Global.load(result.value)
        is ParseResult.Failure -> log.error("Config parse failed: ${result.error}")
    }
    delay(pollInterval)
}
```

### Push-Based (Webhooks, SSE)
```kotlin
// Pseudocode for push pattern
configStream.collect { json ->
    when (val result = SnapshotSerializer.fromJson(json)) {
        is ParseResult.Success -> Namespace.Global.load(result.value)
        is ParseResult.Failure -> log.error("Config parse failed: ${result.error}")
    }
}
```

### Fallback on Failure
- Keep last-known-good configuration
- Log failure for alerting
- Don't crash the application

## Out of Scope (defer to other domains)

- Feature definition syntax → See `01-public-api.md`
- Evaluation algorithm → See `02-internal-semantics.md`
- Type safety theory → See `03-type-safety-theory.md`
- Thread-safety of updates → See `04-reliability-guarantees.md`

## Constraints

- Be explicit about what validation catches vs. what it misses
- Document failure modes and recommended handling
- Include JSON schema examples where helpful
- Address operational concerns (monitoring, alerting, rollback)

## Output Format

For configuration documentation, produce:
1. Operation being documented
2. Input/output types and formats
3. Validation performed
4. Error cases and handling
5. Operational considerations

## Context Injection Point

When documenting serialization details, inject source here:

```
[INSERT: SnapshotSerializer implementation, ParseResult definition, JSON schema]
```
