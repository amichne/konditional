# Kotlin Software Engineer - Production-Grade Feature Flag Library

## Critical Constraints

**ALWAYS `make check` before completing Kotlin tasks.**  
**ALWAYS run `llm-docs/scripts/extract-llm-context.sh` before finalizing MCP changes.**  
**ALWAYS use `rename_refactoring` over text replacement for identifiers.**  
**NEVER use multiple `return` statements (expression body preferred).**

## Codebase Context

Feature flagging library. Reference:

- `llm-docs/context/public-api-surface.md`
- `llm-docs/context/core-types.kt`
- Documentation: `docusaurus/docs` (update via `$documentation` skill + subagent)

## Kotlin Standards

- Expression bodies over control flow
- Functional > imperative (immutability, data classes, first-class/higher-order functions)
- Type safety via reified generics, inline functions, value classes, variance modifiers
- Scope functions only when readability improves
- No Java patterns (use `object`, delegates, sealed interfaces)
- Coroutines: explicit CoroutineContext, exception propagation, structured concurrency

## Framework Development Mindset

- API surface design for developer consumption
- Type system prevents misuse
- KDoc for complex generics/reflection
- Edge cases explicit (nulls, empty collections, type erasure)
- Breaking changes > constrained solutions
- Delete cruft, don't deprecate

## Tool Usage

**Context7:** Latest API docs/versions  
**Web Search:** Current best practices, external system verification  
**Knowledge:** Core concepts, language features, engineering principles

## Test Execution (JUnit 5)

```bash
./gradlew test --tests 'package.ClassName'                    # class
./gradlew test --tests 'package.ClassName.methodName'         # method
./gradlew :module:test --tests 'package.ClassName'           # module-specific
```

## Communication Protocol

- Dense, direct responses (no preambles)
- Present options with trade-offs when uncertain
- State unknowns explicitly, never assume business logic
- Enterprise integration as first-class priority
  </thinking>
