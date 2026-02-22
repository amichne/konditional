# How-to: enforce namespace isolation

Use this page to design namespace boundaries that limit blast radius and keep
ownership explicit across teams.

## Read this page when

- Multiple teams own different feature sets.
- You need independent refresh and rollback paths per domain.
- You want configuration failures contained to one subsystem.

## Deterministic steps

1. Define boundaries before writing flags.

Use one namespace per ownership and failure domain. Keep the namespace key
stable over time.

2. Declare namespaces explicitly in code.

```kotlin
object CheckoutFlags : Namespace("checkout")
object SearchFlags : Namespace("search")
```

3. Keep features and loading scoped to their namespace.

```kotlin
val checkoutResult = NamespaceSnapshotLoader(CheckoutFlags).load(checkoutJson)
val searchResult = NamespaceSnapshotLoader(SearchFlags).load(searchJson)
```

4. Operate namespaces independently.

- Roll back only the affected namespace.
- Do not mix unrelated features in shared JSON payloads.
- Track ownership metadata per namespace in your runbooks.

5. Prove isolation with tests.

- Loading `checkout` must not alter `search` evaluations.
- `disableAll()` and `rollback(...)` should be exercised per namespace.
- Incident drills should include one-namespace failure simulation.

## Isolation checklist

- [ ] Namespace key maps to one team and one domain.
- [ ] Snapshot loading is executed per namespace, not globally.
- [ ] Rollback and kill-switch procedures are namespace-scoped.
- [ ] Cross-namespace mutation is blocked by tests.

## Next steps

- [Safe remote config](/how-to-guides/safe-remote-config)
- [Failure modes](/production-operations/failure-modes)
- [Theory: namespace isolation](/theory/namespace-isolation)
