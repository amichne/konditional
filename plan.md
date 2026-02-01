# Eager Semantic Analyzer Design

**Date**: 2026-01-25
**Status**: Proposed
**Replaces**: Marker-based `AnalysisApiSemanticAnalyzer`

## Overview

Replace the annotation-marker-based semantic analyzer with an **eager analyzer** that analyzes entire Kotlin code blocks
and produces a rich `SemanticProfile`. This enables documentation that showcases type safety, scope management, and
IDE-like insights without requiring manual `/*hover:id=X*/` annotations.

## Goals

- Remove marker annotation requirement entirely
- Eagerly resolve all semantic information for code blocks
- Expose insights at configurable granularity levels
- Output JSON consumable by React documentation components
- Highlight what makes well-designed Kotlin APIs superior

## Non-Goals

- IDE integration or compilation
- Runtime evaluation
- Backwards compatibility with marker-based system (no users)

---

## Core Model

### Category Configuration

Seven orthogonal insight categories, each with independent levels:

```kotlin
enum class InsightCategory {
    TYPE_INFERENCE,
    NULLABILITY,
    SMART_CASTS,
    SCOPING,
    EXTENSIONS,
    LAMBDAS,
    OVERLOADS,
}

enum class InsightLevel {
    OFF,        // Category disabled
    HIGHLIGHTS, // Only notable/interesting instances
    ALL         // Every occurrence
}

data class AnalysisConfig(
    val levels: Map<InsightCategory, InsightLevel> = defaults(),
) {
    companion object {
        fun defaults(): Map<InsightCategory, InsightLevel> =
            InsightCategory.entries.associateWith { InsightLevel.HIGHLIGHTS }

        fun all(): Map<InsightCategory, InsightLevel> =
            InsightCategory.entries.associateWith { InsightLevel.ALL }

        fun minimal(): Map<InsightCategory, InsightLevel> =
            InsightCategory.entries.associateWith { InsightLevel.OFF }
    }
}
```

### Output Model

```kotlin
@Serializable
data class SemanticProfile(
    val snippetId: String,
    val codeHash: String,
    val code: String,
    val insights: List<SemanticInsight>,
    val rootScopes: List<ScopeNode>,
)
```

---

## Insight Structure

### SemanticInsight

```kotlin
@Serializable
data class SemanticInsight(
    val id: String,
    val position: Range,
    val category: InsightCategory,
    val level: InsightLevel,
    val kind: InsightKind,
    val scopeChain: List<ScopeRef>,
    val data: InsightData,
    val tokenText: String,
)
```

### Scope References

```kotlin
@Serializable
data class ScopeRef(
    val scopeId: String,
    val kind: ScopeKind,
    val receiverType: String? = null,
    val position: Range,
)

@Serializable
enum class ScopeKind {
    FILE, CLASS, FUNCTION, LAMBDA, SCOPE_FUNCTION,
    WHEN_BRANCH, IF_BRANCH, TRY_BLOCK, CATCH_BLOCK,
}

@Serializable
data class ScopeNode(
    val ref: ScopeRef,
    val children: List<ScopeNode>,
    val insights: List<String>,
)
```

### Position Types

```kotlin
@Serializable
data class Range(
    val from: Position,
    val to: Position,
)

@Serializable
data class Position(
    val line: Int,
    val col: Int,
)
```

---

## InsightKind Enumeration

```kotlin
@Serializable
enum class InsightKind {
    // TypeInference
    INFERRED_TYPE,
    EXPLICIT_TYPE,
    GENERIC_ARGUMENT_INFERRED,

    // Nullability
    NULLABLE_TYPE,
    PLATFORM_TYPE,
    NULL_SAFE_CALL,
    ELVIS_OPERATOR,
    NOT_NULL_ASSERTION,

    // SmartCasts
    IS_CHECK_CAST,
    WHEN_BRANCH_CAST,
    NEGATED_CHECK_EXIT,
    NULL_CHECK_CAST,

    // Scoping
    RECEIVER_CHANGE,
    IMPLICIT_THIS,
    SCOPE_FUNCTION_ENTRY,

    // Extensions
    EXTENSION_FUNCTION_CALL,
    EXTENSION_PROPERTY_ACCESS,
    MEMBER_VS_EXTENSION_RESOLUTION,

    // Lambdas
    LAMBDA_PARAMETER_INFERRED,
    LAMBDA_RETURN_INFERRED,
    SAM_CONVERSION,
    TRAILING_LAMBDA,

    // Overloads
    OVERLOAD_RESOLVED,
    DEFAULT_ARGUMENT_USED,
    NAMED_ARGUMENT_REORDER,
}
```

---

## InsightData Sealed Hierarchy

```kotlin
@Serializable
sealed interface InsightData

@Serializable
@SerialName("TypeInference")
data class TypeInferenceData(
    val inferredType: String,
    val declaredType: String? = null,
    val typeArguments: List<String>? = null,
) : InsightData

@Serializable
@SerialName("Nullability")
data class NullabilityData(
    val type: String,
    val isNullable: Boolean,
    val isPlatformType: Boolean,
    val narrowedToNonNull: Boolean,
) : InsightData

@Serializable
@SerialName("SmartCast")
data class SmartCastData(
    val originalType: String,
    val narrowedType: String,
    val evidencePosition: Range,
    val evidenceKind: String,
) : InsightData

@Serializable
@SerialName("Scoping")
data class ScopingData(
    val scopeFunction: String? = null,
    val outerReceiver: String? = null,
    val innerReceiver: String? = null,
    val itParameterType: String? = null,
) : InsightData

@Serializable
@SerialName("Extension")
data class ExtensionData(
    val functionOrProperty: String,
    val extensionReceiverType: String,
    val dispatchReceiverType: String? = null,
    val resolvedFrom: String,
    val competingMember: Boolean,
) : InsightData

@Serializable
@SerialName("Lambda")
data class LambdaData(
    val parameterTypes: List<LambdaParam>,
    val returnType: String,
    val inferredFromContext: String? = null,
    val samInterface: String? = null,
) : InsightData

@Serializable
data class LambdaParam(
    val name: String? = null,
    val type: String,
)

@Serializable
@SerialName("Overload")
data class OverloadData(
    val selectedSignature: String,
    val candidateCount: Int,
    val resolutionFactors: List<String>,
    val defaultArgumentsUsed: List<String>? = null,
) : InsightData
```

---

## Public API

### Interface

```kotlin
interface EagerSemanticAnalyzer {
    fun analyze(
        code: String,
        config: AnalysisConfig = AnalysisConfig.defaults(),
        kotlinVersion: String = ENGINE_KOTLIN_VERSION,
    ): SemanticProfile
}
```

### Factory

```kotlin
object SemanticAnalyzerFactory {
    fun create(analysisConfig: AnalysisApiConfig): EagerSemanticAnalyzer =
        AnalysisApiEagerAnalyzer(analysisConfig)
}
```

### Usage

```kotlin
val analyzer = SemanticAnalyzerFactory.create(config)
val profile = analyzer.analyze(
    """
    val items = listOf("a", "b", "c")
    items.map { it.uppercase() }
""".trimIndent()
)

// Query insights
profile.insights
    .filter { it.category == InsightCategory.TYPE_INFERENCE }
    .filter { it.level == InsightLevel.HIGHLIGHTS }
```

---

## Implementation Architecture

### PSI Visitor

```kotlin
class KtTreeVisitor(
    private val collector: InsightCollector,
    private val scopeBuilder: ScopeTreeBuilder,
) : KtVisitorVoid() {

    override fun visitKtElement(element: KtElement) {
        val scopeChange = scopeBuilder.enterIfScope(element)
        collector.examine(element)
        element.acceptChildren(this)
        scopeChange?.let { scopeBuilder.exit() }
    }
}
```

### InsightCollector

```kotlin
class InsightCollector(
    private val session: KaSession,
    private val config: AnalysisConfig,
    private val lineMap: LineMap,
) {
    private val extractors: List<InsightExtractor> = listOf(
        TypeInferenceExtractor(session, config.levelFor(TYPE_INFERENCE)),
        NullabilityExtractor(session, config.levelFor(NULLABILITY)),
        SmartCastExtractor(session, config.levelFor(SMART_CASTS)),
        ScopingExtractor(session, config.levelFor(SCOPING)),
        ExtensionExtractor(session, config.levelFor(EXTENSIONS)),
        LambdaExtractor(session, config.levelFor(LAMBDAS)),
        OverloadExtractor(session, config.levelFor(OVERLOADS)),
    )

    private val collected = mutableListOf<SemanticInsight>()

    fun examine(element: KtElement) {
        for (extractor in extractors) {
            extractor.extract(element)?.let { raw ->
                collected.add(raw.toInsight(nextId(), lineMap, scopeBuilder.currentChain()))
            }
        }
    }

    fun insights(): List<SemanticInsight> = collected.toList()
}
```

### Extractor Contract

```kotlin
interface InsightExtractor {
    fun extract(element: KtElement): RawInsight?
}

data class RawInsight(
    val position: TextRange,
    val category: InsightCategory,
    val level: InsightLevel,
    val kind: InsightKind,
    val data: InsightData,
    val tokenText: String,
)
```

---

## Highlight Heuristics

Each category defines what makes an insight "noteworthy" (HIGHLIGHTS vs ALL):

| Category      | HIGHLIGHTS when...                                                  |
|---------------|---------------------------------------------------------------------|
| TypeInference | Fully inferred (no declaration) or generic args inferred            |
| Nullability   | Platform type, null check narrowing, or `!!` usage                  |
| SmartCasts    | Type meaningfully narrows (nullable→non-null, supertype→subtype)    |
| Scoping       | Any scope function or receiver change                               |
| Extensions    | Extension wins over member or comes from external package           |
| Lambdas       | SAM conversion or complex multi-param inference                     |
| Overloads     | 3+ candidates, default args used, or named args affected resolution |

### Example: SmartCast Heuristic

```kotlin
private fun KaSession.isSignificantNarrowing(
    from: KaType,
    to: KaType
): Boolean {
    return from.isMarkedNullable && !to.isMarkedNullable ||
           to.isSubtypeOf(from) && !from.isSubtypeOf(to)
}
```

---

## JSON Serialization

```kotlin
object SemanticProfileSerializer {
    private val json = Json {
        prettyPrint = false
        encodeDefaults = false
    }

    fun toJson(profile: SemanticProfile): String =
        json.encodeToString(profile)

    fun fromJson(jsonString: String): SemanticProfile =
        json.decodeFromString(jsonString)
}
```

### Example Output

```json
{
  "snippetId": "example-1",
  "codeHash": "a1b2c3",
  "code": "val items = listOf(\"a\", \"b\")\nitems.map { it.uppercase() }",
  "insights": [
    {
      "id": "ins_001",
      "position": {
        "from": {
          "line": 1,
          "col": 5
        },
        "to": {
          "line": 1,
          "col": 10
        }
      },
      "category": "TYPE_INFERENCE",
      "level": "HIGHLIGHTS",
      "kind": "INFERRED_TYPE",
      "scopeChain": [
        {
          "scopeId": "scope_0",
          "kind": "FILE",
          "receiverType": null
        }
      ],
      "data": {
        "type": "TypeInference",
        "inferredType": "List<String>",
        "declaredType": null
      },
      "tokenText": "items"
    }
  ],
  "rootScopes": []
}
```

---

## File Structure

```
kodeblok-engine/src/main/kotlin/kodeblok/engine/
├── analysis/
│   ├── EagerSemanticAnalyzer.kt
│   ├── AnalysisApiEagerAnalyzer.kt
│   ├── AnalysisApiConfig.kt            # existing
│   ├── AnalysisApiEnvironment.kt       # existing
│   ├── InsightCollector.kt
│   ├── ScopeTreeBuilder.kt
│   ├── KtTreeVisitor.kt
│   └── extractors/
│       ├── InsightExtractor.kt
│       ├── TypeInferenceExtractor.kt
│       ├── NullabilityExtractor.kt
│       ├── SmartCastExtractor.kt
│       ├── ScopingExtractor.kt
│       ├── ExtensionExtractor.kt
│       ├── LambdaExtractor.kt
│       └── OverloadExtractor.kt

kodeblok-schema/src/main/kotlin/kodeblok/schema/
├── KodeblokSchema.kt                   # deprecate
└── SemanticSchema.kt                   # new models
```

### Files to Delete

- `MarkerParser.kt`
- `TokenLocator.kt`
- `SemanticAnalyzer.kt`
- `AnalysisApiSemanticAnalyzer.kt`

### Files to Modify

- `EngineModels.kt` - remove `HoverMarker`, `MarkerKind`, `HoverTarget`
- `KodeblokEngine.kt` - use new analyzer

---

## Documentation Value

This design enables documentation narratives that show why well-designed libraries shine:

| Insight                                    | Narrative                                                                             |
|--------------------------------------------|---------------------------------------------------------------------------------------|
| `TypeInferenceData` with no `declaredType` | "No type annotation needed—the compiler knows this is `List<User>`"                   |
| `SmartCastData` with `evidencePosition`    | "After the null check on line 3, the compiler knows `user` is non-null"               |
| `ExtensionData.resolvedFrom`               | "This `toJson()` comes from kotlinx.serialization—feels native but it's an extension" |
| `ScopingData.innerReceiver`                | "Inside `apply`, `this` is the `StringBuilder`—no prefix needed"                      |
| `OverloadData.resolutionFactors`           | "Selected over 3 other overloads due to more specific parameter types"                |
