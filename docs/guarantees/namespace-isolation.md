# Namespace isolation

Two namespaces can declare the same key. Their features have different identities because each feature ID includes the namespace identifier.

With the default registry, loading a configuration or restoring a snapshot affects only the owning namespace. Each namespace has its own kill switch and rollback history.

The [checkout example](../quickstart/verify-behavior.md) evaluates the same key and context in two namespaces and checks both results. Rollout buckets use a separate [calculation](determinism.md).
