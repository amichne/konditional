# Welcome to Konditional

Type-safe, deterministic feature flags for Kotlin

[Get Started](getting-started/introduction.md){ .md-button .md-button--primary }
[View on GitHub](https://github.com/amichne/konditional){ .md-button }

## Features

**Type-Safe**: Leverage Kotlin's type system for compile-time safety. No string-based lookups, no runtime casting.

**Deterministic**: SHA-256 based bucketing ensures consistent user experiences across sessions.

**Parse, Don't Validate**: Structured error handling with `ParseResult<T>` instead of exceptions.

**Composable**: Extensible evaluation logic through `Evaluable<C>` abstraction.

**Serializable**: Export and import flag configurations with type-safe JSON serialization.

**Flexible**: Support for version ranges, custom types, custom contexts, and complex targeting rules.
