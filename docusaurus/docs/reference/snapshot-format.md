---
title: Snapshot Format
sidebar_position: 3
---

# Snapshot Format

Reference for full snapshot JSON payloads consumed at the boundary.

## Top-Level Shape

```json
{
  "meta": {
    "version": "2026-02-23.1",
    "generatedAtEpochMillis": 1766800000000,
    "source": "s3://flags/app-snapshot.json"
  },
  "flags": [
    {
      "key": "feature::app::checkoutVariant",
      "defaultValue": { "type": "ENUM", "value": "CLASSIC", "enumClassName": "com.example.CheckoutVariant" },
      "salt": "v1",
      "isActive": true,
      "rampUpAllowlist": [],
      "rules": []
    }
  ]
}
```

## Fields

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `meta` | object | No | Optional metadata for provenance and versioning. |
| `meta.version` | string | No | Logical config revision string. |
| `meta.generatedAtEpochMillis` | number | No | UTC epoch milliseconds from publisher. |
| `meta.source` | string | No | Source identifier (URI, commit, artifact ref). |
| `flags` | array | Yes | Full set of feature definitions for namespace scope. |
| `flags[].key` | string | Yes | Feature identifier; must resolve against namespace schema. |
| `flags[].defaultValue` | object | Yes | Tagged value object (`BOOLEAN`, `INT`, `DOUBLE`, `STRING`, `ENUM`, `DATA_CLASS`). |
| `flags[].salt` | string | Yes | Salt used for deterministic bucketing. |
| `flags[].isActive` | boolean | Yes | If `false`, evaluation returns default regardless of rules. |
| `flags[].rampUpAllowlist` | `array<string>` | No | Stable IDs that bypass rollout gate for this feature. |
| `flags[].rules` | array | Yes | Ordered rule payloads evaluated by deterministic precedence. |

## Value Variants

| Variant | Shape |
| --- | --- |
| Boolean | `{ "type": "BOOLEAN", "value": true }` |
| Int | `{ "type": "INT", "value": 3 }` |
| Double | `{ "type": "DOUBLE", "value": 0.5 }` |
| String | `{ "type": "STRING", "value": "text" }` |
| Enum | `{ "type": "ENUM", "value": "CLASSIC", "enumClassName": "com.example.CheckoutVariant" }` |
| Data class | `{ "type": "DATA_CLASS", "dataClassName": "com.example.Policy", "value": { ... } }` |

## Boundary Semantics

- Successful decode/load materializes trusted configuration.
- Invalid shape or unresolved feature keys return typed `ParseError` through `Result.failure`.
- Failed loads do not partially mutate active runtime state.

## Next Steps

- [Patch Format](/reference/patch-format) - Incremental update payloads.
- [Snapshot Load Options](/reference/snapshot-load-options) - Strict vs. forward-compatible loading strategies.
