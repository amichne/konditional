# Persistence format

This page defines the JSON shapes used for snapshot storage and patch-based
updates.

## Read this page when

- You are generating or validating external configuration payloads.
- You are building config tooling around snapshot and patch JSON.
- You need exact field-level semantics for boundary tests.

## Concepts in scope

- **Snapshot payload**: complete set of flag definitions plus optional metadata.
- **Patch payload**: upsert flag definitions plus optional `removeKeys`.
- **Feature identity**: `key` uses stable `FeatureId` format.
- **Typed values**: `defaultValue` and rule `value` use tagged unions.
- **Targeting fields**: locales, platforms, version ranges, axes, and rollout
  controls serialize as explicit JSON fields.

## Snapshot shape

```json
{
  "meta": {
    "version": "rev-123",
    "generatedAtEpochMillis": 1700000000000,
    "source": "s3://configs/app.json"
  },
  "flags": [
    {
      "key": "feature::app::darkMode",
      "defaultValue": { "type": "BOOLEAN", "value": false },
      "salt": "v1",
      "isActive": true,
      "rampUpAllowlist": [],
      "rules": []
    }
  ]
}
```

## Patch shape

```json
{
  "meta": {
    "version": "rev-124"
  },
  "flags": [
    {
      "key": "feature::app::darkMode",
      "defaultValue": { "type": "BOOLEAN", "value": false },
      "salt": "v1",
      "isActive": true,
      "rampUpAllowlist": [],
      "rules": []
    }
  ],
  "removeKeys": ["feature::app::legacyFeature"]
}
```

## Field semantics

- `key`: `feature::<namespaceSeed>::<featureKey>`.
- `defaultValue` and `rules[].value`: tagged value objects.
- `versionRange`: tagged range object (`UNBOUNDED`, `MIN_BOUND`,
  `MAX_BOUND`, `MIN_AND_MAX_BOUND`).
- `rampUpAllowlist`: stable ID hex strings used only after criteria match.

### Value encoding (`defaultValue` / rule `value`) {#value-encoding-defaultvalue--rule-value}

Value objects are tagged unions. The `type` discriminator determines which
payload shape is valid for `value`.

## Related pages

- [Serialization reference](/serialization/reference)
- [Runtime lifecycle](/runtime/lifecycle)
- [Parse don’t validate](/theory/parse-dont-validate)
- [Type safety boundaries](/theory/type-safety-boundaries)

## Next steps

1. Build fixtures from these shapes for boundary tests.
2. Decode fixtures with strict options before runtime load.
3. Link operational handling in [Runtime operations](/runtime/operations).
