# Snapshot JSON format

`Namespace.toJson()` exports a namespace snapshot as one JSON object with an
optional `meta` section and a required `flags` array.

## Top-level shape

This is the current conceptual wire shape.

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
| `type` | `STATIC` for a flat rule, `TARGETING_V1` for a static value with an expression, or `CONTEXTUAL` for a Kotlin resolver |
| `ruleId` | Optional serialized rule identifier |
| `rampUp` | Rollout percentage as a `Double` |
| `rampUpAllowlist` | Stable IDs that bypass rule ramp-up gating |
| `note` | Optional human note |
| `locales` | Allowed locale identifiers |
| `platforms` | Allowed platform identifiers |
| `versionRange` | Serialized version-range object |
| `axes` | Map of axis ID to allowed value IDs |
| `targeting` | Expression for a `TARGETING_V1` rule |

### Expression rules

This rule admits an enterprise buyer with at least ten seats.

```json
{
  "type": "TARGETING_V1",
  "value": { "type": "BOOLEAN", "value": true },
  "targeting": {
    "op": "allOf",
    "operands": [
      { "op": "attributeIn", "key": "plan", "values": ["ENTERPRISE"] },
      { "op": "predicate", "key": "minimumSeats", "version": 1, "argument": "10" }
    ]
  }
}
```

`allOf` and `anyOf` contain ordered child expressions. Attribute values and
predicate arguments use canonical text. The client's declaration assigns a
type to each operand. Here, the predicate takes an integer and the wire
representation of ten is the string `"10"`.

Clients can still load legacy flat rules. An older client rejects a snapshot
with the new rule type. Roll out support to clients before publishing the first
expression. If a client cannot resolve a key, version, operator, or operand,
it keeps its complete previous namespace snapshot.

An anonymous Kotlin condition such as `require { enterprise }` can run locally.
`tryToJson()` returns a failure if an export would lose that condition. To
distribute that rule, declare `enterprise` as an attribute or register a named
predicate.

A `CONTEXTUAL` value comes from a Kotlin resolver. Legacy flat rules can
export this marker. A reader returns `ParseError.InvalidSnapshot` because it
cannot restore the resolver. Export rejects contextual values paired with a
rule that needs the expression format.

## Next steps

If you want to inspect how `explain(...)` describes a decision rather than how
`toJson()` serializes a namespace, open the diagnostics page.

- [Open evaluation diagnostics](evaluation-diagnostics.md)

