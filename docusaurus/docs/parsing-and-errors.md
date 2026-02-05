---
title: Parsing & Errors (Hard Boundaries)
---

# Parsing & Errors

Konditional treats configuration as a **hard boundary**:

- Parsing returns typed results.
- Invalid inputs produce typed failures.
- The active configuration is not partially mutated by a bad update.

## ParseResult / ParseError philosophy

Parsing is modeled as a value:

- `ParseResult.Success(value)`
- `ParseResult.Failure(error)`

The goal is to keep evaluation and application logic free of “maybe it parsed” states.

## Common error classes

While the exact error set depends on which codec you use at the boundary, common categories include:

- invalid ramp-up percentage
- invalid versions / version ranges
- unknown or malformed feature keys
- invalid axis IDs or axis values

Next:

- [Registry & Configuration](registry-and-configuration)
- [FAQ](faq)

