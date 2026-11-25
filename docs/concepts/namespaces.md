# Namespaces

A namespace groups feature properties under one identifier. Two namespaces can each declare `enabled`. Each feature ID combines the namespace identifier with its own key.

By default, each namespace has a registry for its active configuration, kill switch, and rollback history. Evaluation uses the loaded definition when one exists. Otherwise it uses the feature declaration.

See [namespace isolation](../guarantees/namespace-isolation.md) for the scope of a load or rollback.
