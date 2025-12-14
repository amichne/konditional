# Konditional: Getting Started Guide

## Value Proposition

Konditional provides **type-safe, flexible feature flags** with powerful targeting capabilities and zero runtime surprises.

## Quick Start Journey

```mermaid
graph TB
    Start([New Integration]) --> Step1[Define Features]
    Step1 --> Step2[Configure Rules]
    Step2 --> Step3[Build Context]
    Step3 --> Step4[Evaluate Flags]
    Step4 --> Value[Production Ready]

    Step1 -.->|Benefit| B1[Type Safety<br/>Compile-time guarantees]
    Step2 -.->|Benefit| B2[Flexible Targeting<br/>Multi-dimensional rules]
    Step3 -.->|Benefit| B3[Runtime Context<br/>User-specific evaluation]
    Step4 -.->|Benefit| B4[Deterministic Results<br/>Stable bucketing]

    style Start fill:#e1f5ff
    style Value fill:#d4edda
    style B1 fill:#fff3cd
    style B2 fill:#fff3cd
    style B3 fill:#fff3cd
    style B4 fill:#fff3cd
```

## End-to-End Usage Pattern

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant Container as FeatureContainer
    participant Namespace as Namespace Registry
    participant Runtime as Runtime Context
    participant Eval as Evaluation Engine

    Note over Dev,Container: 1. DEFINE PHASE (Compile Time)
    Dev->>Container: Define feature with type<br/>val darkMode by boolean(default=false)
    Container->>Namespace: Register feature + rules
    Note right of Namespace: ✓ Type-safe<br/>✓ Namespace isolated<br/>✓ Serializable

    Note over Runtime,Eval: 2. RUNTIME PHASE
    Dev->>Runtime: Create Context<br/>(locale, platform, version, stableId)
    Dev->>Eval: feature.evaluate(context)
    Eval->>Namespace: Fetch flag definition
    Eval->>Eval: Match rules by specificity<br/>Check rollout bucket
    Eval-->>Dev: Return typed value
    Note right of Dev: ✓ Deterministic<br/>✓ Type-safe result<br/>✓ No reflection
```

## Core Concepts Flow

```mermaid
flowchart LR
    subgraph Define["1️⃣ DEFINE Features"]
        FC[FeatureContainer&ltM&gt]
        FC --> BF[Boolean Features]
        FC --> SF[String Features]
        FC --> CF[Custom Types]
    end

    subgraph Rules["2️⃣ CONFIGURE Rules"]
        R[Rule Builder]
        R --> L[Locale Targeting]
        R --> P[Platform Targeting]
        R --> V[Version Ranges]
        R --> D[Custom axis']
        R --> RO[Rollout %]
    end

    subgraph Context["3️⃣ BUILD Context"]
        CTX[Context]
        CTX --> A[App Properties]
        CTX --> U[User StableId]
        CTX --> CD[Custom axis']
    end

    subgraph Eval["4️⃣ EVALUATE"]
        E[Evaluation]
        E --> M[Rule Matching]
        E --> B[Bucketing]
        E --> T[Typed Result]
    end

    Define --> Rules
    Rules --> Context
    Context --> Eval

    style Define fill:#e3f2fd
    style Rules fill:#f3e5f5
    style Context fill:#e8f5e9
    style Eval fill:#fff3e0
```

## Best Practices Architecture

```mermaid
graph TB
    subgraph Namespace["Namespace Organization"]
        N1[Payments]
        N2[Authentication]
        N3[Messaging]
        N4[Custom Domain]
    end

    subgraph Features["Feature Definitions"]
        N1 --> F1[payment.newCheckout]
        N1 --> F2[payment.cryptoSupport]
        N2 --> F3[auth.mfaRequired]
        N3 --> F4[msg.richNotifications]
    end

    subgraph Rules["Targeting Strategy"]
        F1 --> R1[Platform: iOS<br/>Version: 2.0.0+<br/>Rollout: 25%]
        F2 --> R2[Dimension: region=US<br/>Rollout: 100%]
        F3 --> R3[Extension: isPremium<br/>Rollout: 50%]
        F4 --> R4[Locale: en_US<br/>Platform: Mobile]
    end

    subgraph Runtime["Runtime Evaluation"]
        CTX[User Context]
        CTX --> E1{Match Rules}
        E1 -->|Match| E2{In Bucket?}
        E1 -->|No Match| DEF[Default Value]
        E2 -->|Yes| VAL[Rule Value]
        E2 -->|No| DEF
    end

    Rules --> Runtime

    style Namespace fill:#e1f5ff
    style Features fill:#f0f4c3
    style Rules fill:#ffe0b2
    style Runtime fill:#c8e6c9
```

## Integration Workflow

```mermaid
stateDiagram-v2
    [*] --> Setup: Start Integration

    Setup --> DefineNamespace: Choose/Create Namespace
    DefineNamespace --> DefineFeatures: Extend FeatureContainer

    DefineFeatures --> ConfigureRules: Add Targeting Rules
    ConfigureRules --> ConfigureRollout: Set Rollout %

    ConfigureRollout --> ImplementContext: Create Context Factory
    ImplementContext --> Addaxis': Add Custom axis'?

    Addaxis' --> Registeraxis': Yes - Register Axes
    Addaxis' --> Evaluate: No - Use Core Context
    Registeraxis' --> Evaluate

    Evaluate --> Serialize: Need Remote Config?
    Serialize --> RemoteConfig: Yes - Export/Import JSON
    Serialize --> Production: No - Local Only
    RemoteConfig --> Production

    Production --> [*]

    note right of DefineNamespace
        ✓ Domain isolation
        ✓ Independent registries
    end note

    note right of ConfigureRules
        ✓ Multi-dimensional targeting
        ✓ Specificity-based matching
    end note

    note right of Evaluate
        ✓ Type-safe evaluation
        ✓ Stable bucketing
    end note

    note right of Serialize
        ✓ Remote configuration
        ✓ Dynamic updates
    end note
```

## Key Benefits Summary

```mermaid
mindmap
    root((Konditional))
        Type Safety
            Compile-time checks
            No casting errors
            IDE autocomplete
        Flexible Targeting
            Locale + Platform
            Version ranges
            Custom axis'
            Extension predicates
        Deterministic
            Stable bucketing
            SHA-256 hashing
            Reproducible results
        Scalable
            Namespace isolation
            Serialization support
            Remote configuration
        Developer Experience
            DSL syntax
            Property delegation
            Minimal boilerplate
```

## Sample Code: Complete Example

```kotlin
// 1. Define custom dimension
enum class Environment(override val id: String) : DimensionKey {
    PRODUCTION("prod"),
    STAGING("staging")
}
val ENV_AXIS = dimensionAxis<Environment>("env")

// 2. Create feature container
object PaymentFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
    val newCheckoutFlow by boolean(default = false) {
        rule {
            platforms(Platform.IOS, Platform.ANDROID)
            versions { min(2, 0, 0) }
            dimension(ENV_AXIS, Environment.PRODUCTION)
            rollout { 25 }
            note("Gradual rollout of new checkout")
        } returns true
    }
}

// 3. Build runtime context
val context = Context(
    locale = AppLocale.EN_US,
    platform = Platform.IOS,
    appVersion = Version(2, 1, 0),
    stableId = StableId.of(userId)
).withaxis' {
    set(ENV_AXIS, Environment.PRODUCTION)
}

// 4. Evaluate feature
val useNewCheckout: Boolean = PaymentFeatures.newCheckoutFlow.evaluate(context)
```

## Next Steps

1. **Choose your namespace** - Use built-in (Global, Authentication, Payments, etc.) or create custom
2. **Define features** - Extend `FeatureContainer<M>` with your flags
3. **Configure targeting** - Add rules with locale, platform, version, axis'
4. **Build context factory** - Create context from request/user data
5. **Evaluate** - Call `feature.evaluate(context)` to get type-safe results
6. **(Optional) Serialize** - Export to JSON for remote configuration

---

**Why Konditional?**
- ✅ **Zero runtime type errors** - Full compile-time safety
- ✅ **Flexible targeting** - Multi-dimensional rules with specificity ordering
- ✅ **Stable rollouts** - Deterministic bucketing via SHA-256
- ✅ **Namespace isolation** - Domain-driven feature organization
- ✅ **Production-ready** - Serialization, patching, and remote config support
