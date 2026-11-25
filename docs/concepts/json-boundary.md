# JSON import and export

`Namespace.toJson()` exports the namespace configuration. The [snapshot format](../reference/snapshot-json-format.md) defines the feature keys, value types, and rule fields.

`Namespace.fromJson(json)` decodes a complete configuration against the receiving namespace's declarations. On success, it installs the configuration. On failure, it returns a typed error and keeps the active snapshot.

See [rejected snapshots](../guarantees/parse-boundary.md) for error types or [remote configuration](../guides/remote-configuration.md) for a caller example.
