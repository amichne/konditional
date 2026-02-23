---
title: Patch Format
sidebar_position: 4
---

# Patch Format

Reference for incremental update payloads applied against an existing trusted snapshot.

## Top-Level Shape

```json
{
  "meta": {
    "version": "2026-02-23.2",
    "generatedAtEpochMillis": 1766801000000,
    "source": "git:configs/patch-42.json"
  },
  "flags": [
    {
      "key": "feature::app::checkoutVariant",
      "defaultValue": { "type": "ENUM", "value": "SMART", "enumClassName": "com.example.CheckoutVariant" },
      "salt": "v1",
      "isActive": true,
      "rampUpAllowlist": [],
      "rules": []
    }
  ],
  "removeKeys": ["feature::app::legacyFlow"]
}
```

## Fields

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `meta` | object | No | Optional metadata; when present it can replace prior metadata. |
| `flags` | array | No | Upserts for feature definitions. |
| `removeKeys` | `array<string>` | No | Feature keys removed from resulting configuration. |

## Merge Semantics

1. Start from current trusted snapshot.
2. Remove all `removeKeys`.
3. Upsert `flags` payload by key.
4. Revalidate merged result against namespace schema and load options.

If validation fails at any step, patch application returns failed `Result` and active runtime state remains unchanged.

## Error Conditions

- Invalid JSON shape.
- Feature key not found under strict unknown-key policy.
- Value type mismatch for declared feature schema.
- Missing required declared flags under reject policy.

## Next Steps

- [Remote Configuration Guide](/guides/remote-configuration)
- [Incremental Updates Guide](/guides/incremental-updates)
