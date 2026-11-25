# Snapshot JSON format

`Namespace.toJson()` exports a namespace snapshot as one JSON object with an
optional `meta` section and a required `flags` array.

## Top-level shape

This example describes one enum feature in the checkout namespace.

```json
{
  "meta": {
    "version": "v1",
    "generatedAtEpochMillis": 1735689600000,
    "source": "control-plane"
  },
  "flags": [
    {
      "key": "feature::checkout::productEligibility",
      "defaultValue": {
        "type": "ENUM",
        "value": "ELIGIBLE",
        "enumClassName": "com.example.ProductEligibility"
      },
      "salt": "v1",
      "isActive": true,
      "rampUpAllowlist": [],
      "rules": []
    }
  ]
}
```

`meta` is omitted when all metadata fields are null.

## Flag entries

Each entry in `flags` represents one feature definition for the namespace.

| Field | Meaning |
| --- | --- |
| `key` | Serialized `FeatureId` |
| `defaultValue` | Typed value envelope for the flag's default |
| `salt` | Stable bucketing salt |
| `isActive` | Whether the flag is active |
| `rampUpAllowlist` | Stable IDs that bypass ramp-up gating |
| `rules` | Serialized rule list in snapshot order |

## Value envelope shapes

`defaultValue` and rule `value` fields use a typed envelope.
For each default and static rule, the envelope must match the feature's declared value type.

| `type` | Additional fields |
| --- | --- |
| `BOOLEAN` | `value: true` or `false` |
| `STRING` | `value: "..."` |
| `INT` | `value: 42` |
| `DOUBLE` | `value: 42.5` |
| `ENUM` | `value`, `enumClassName` |
| `DATA_CLASS` | `value`, `dataClassName` |
| `KONSTRAINED_PRIMITIVE` | `value`, `konstrainedClassName` |

## Rule entries

Each rule entry can carry these fields.

| Field | Meaning |
| --- | --- |
| `value` | Typed envelope for the rule value |
| `type` | Static or contextual rule value type |
| `ruleId` | Optional serialized rule identifier |
| `rampUp` | Rollout percentage as a `Double` |
| `rampUpAllowlist` | Stable IDs that bypass rule ramp-up gating |
| `note` | Optional human note |
| `locales` | Allowed locale identifiers |
| `platforms` | Allowed platform identifiers |
| `versionRange` | Serialized version-range object |
| `axes` | Map of axis ID to allowed value IDs |

If a rule contains an anonymous predicate, an `anyOf` group, or another condition the flat fields cannot preserve,
`toJson()` throws `KonditionalBoundaryFailure` with `ParseError.InvalidSnapshot`. The error identifies the feature and rule.

A `CONTEXTUAL` value comes from a Kotlin resolver. The snapshot records the rule but cannot recreate its resolver.
`fromJson()` returns `ParseError.InvalidSnapshot` for a snapshot containing such a rule. The namespace keeps its previous configuration.
