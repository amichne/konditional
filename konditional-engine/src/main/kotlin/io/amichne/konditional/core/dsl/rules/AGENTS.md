# Directory Scope

This file scopes `konditional-engine/src/main/kotlin/io/amichne/konditional/core/dsl/rules/` and yields to deeper `AGENTS.md` files when present.

## Purpose
Rule-authoring scopes and builders that translate the DSL into immutable rule specs.

## Local Files
- `ContextRuleScope.kt`
- `NamespaceRuleSet.kt`
- `NoteScope.kt`
- `PendingYieldToken.kt`
- `RuleScope.kt`
- `RuleScopeBase.kt`
- `RuleSet.kt`
- `RuleSetBuilder.kt`
- `RuleSpec.kt`
- `RuleValueScope.kt`
- `YieldingScopeHost.kt`

## Go Deeper
- `targeting/`: Targeting-specific DSL fragments.

## Rules
- Rule construction order is semantically significant; keep ordinal allocation and deferred yields deterministic.
- Context narrowing and value resolution must remain type-safe across scoped and unscoped rule APIs.
