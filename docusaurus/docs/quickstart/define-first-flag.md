# Define first flag

Define your first namespace-owned typed feature to establish compile-time
feature contracts [CLM-PR01-08A].

## Example

```kotlin
object AppFeatures : Namespace("app") {
  val checkoutVariant by enum<CheckoutVariant, Context>(default = CheckoutVariant.CLASSIC)
}
```

Namespace metadata feeds boundary codecs and snapshot loaders, so declaration
shape directly influences ingestion and materialization behavior
[CLM-PR01-08B].

## Claim citations

| Claim ID | Explicit claim | Local evidence linkage | Registry link |
|---|---|---|---|
| CLM-PR01-08A | Feature declarations are represented by Namespace, Feature, and FlagDefinition types. | `#example` | `/reference/claims-registry#clm-pr01-08a` |
| CLM-PR01-08B | Namespaces provide a compiled schema used by boundary codecs and loaders. | `#example` | `/reference/claims-registry#clm-pr01-08b` |
