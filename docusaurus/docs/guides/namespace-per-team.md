---
title: Namespace per Team
sidebar_position: 5
---

# Namespace per Team

Use one namespace per ownership boundary to keep blast radius explicit.

**Prerequisites:** You understand [Namespaces](/concepts/namespaces).

## Pattern

```kotlin
object PaymentsFlags : Namespace("payments") {
  val retryGateway by boolean<Context>(default = false)
}

object SearchFlags : Namespace("search") {
  val semanticSearch by boolean<Context>(default = true)
}
```

## Operational Rules

- Each team owns snapshot delivery for its namespace.
- Rollback is executed at namespace scope, not globally.
- Incident communication references namespace IDs first.

## Verification Checklist

- Loading `payments` snapshots does not change `search` evaluations.
- Team-local kill-switch does not disable unrelated namespaces.

## Expected Outcome

After this guide, your org has clear team boundaries for declaration ownership and runtime operations.

## Next Steps

- [Theory: Namespace Isolation](/theory/namespace-isolation)
- [Guide: Enterprise Adoption](/guides/enterprise-adoption)
