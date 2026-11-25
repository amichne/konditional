# Konditional

Konditional evaluates feature flags in Kotlin/JVM applications. Each feature declares its return type and the context its rules can inspect.

For example, checkout can return `ELIGIBLE` for payments and `MANUAL_REVIEW` for lending. Callers can handle each result in an exhaustive `when` expression.

- [Run the checkout quickstart](quickstart/index.md) to declare the rules and evaluate both products.
- [Decide whether Konditional fits](start-here/index.md) if your team needs remote configuration or operator controls.
- [Read the concepts](concepts/index.md) for feature types, targeting, and snapshots.
- [Use the guides](guides/index.md) to add a custom value or migrate a caller.
- [Read the guarantees](guarantees/index.md) for concurrent reads, rollout decisions, and failed imports.
- [Look up an API or JSON field](reference/index.md).
