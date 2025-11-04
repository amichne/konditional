# Konditional Documentation Outline
**Comprehensive Documentation Structure for v1.0.0**

---

## 1. System Architecture

### High-Level Component Architecture

```mermaid
graph TB
    subgraph "Client Application"
        APP[Application Code]
        CTX[Context Instance]
    end

    subgraph "Konditional Core"
        COND[Conditional Keys<br/>Type-Safe Enums]
        REG[FlagRegistry<br/>Thread-Safe Store]
        EVAL[Evaluation Engine]
    end

    subgraph "Configuration"
        DSL[ConfigBuilder DSL]
        KONFIG[Konfig<br/>Immutable Snapshot]
        PATCH[KonfigPatch<br/>Incremental Updates]
    end

    subgraph "Serialization"
        SER[SnapshotSerializer]
        CREG[ConditionalRegistry]
        JSON[JSON Configuration]
    end

    APP -->|evaluate| CTX
    CTX -->|evaluate flag| COND
    COND -->|query| REG
    REG -->|retrieve definition| EVAL
    EVAL -->|match rules| CTX

    DSL -->|builds| KONFIG
    KONFIG -->|loads into| REG
    PATCH -->|applies to| KONFIG

    SER -->|serializes/deserializes| KONFIG
    SER -->|resolves keys| CREG
    JSON -->|parsed by| SER

    style APP fill:#e1f5ff
    style CTX fill:#e1f5ff
    style EVAL fill:#fff4e1
    style REG fill:#fff4e1
    style KONFIG fill:#f0e1ff
```

### Core Type Hierarchy

```mermaid
classDiagram
    class Conditional~S,C~ {
        <<interface>>
        +key: String
        +registry: FlagRegistry
    }

    class Context {
        <<interface>>
        +locale: AppLocale
        +platform: Platform
        +appVersion: Version
        +stableId: StableId
    }

    class FlagRegistry {
        <<interface>>
        +load(config: Konfig)
        +update(patch: KonfigPatch)
        +featureFlag(key): FeatureFlag?
        +allFlags(): Map
    }

    class FeatureFlag~S,C~ {
        <<sealed>>
        +defaultValue: S
        +isActive: Boolean
        +conditional: Conditional
        +values: List~ConditionalValue~
        +salt: String
    }

    class Rule~C~ {
        +rollout: Rollout
        +note: String?
        +baseEvaluable: BaseEvaluable
        +extension: Evaluable
        +matches(context): Boolean
        +specificity(): Int
    }

    class Evaluable~C~ {
        <<abstract>>
        +matches(context): Boolean
        +specificity(): Int
    }

    class ConditionalValue~S,C~ {
        +rule: Rule
        +value: S
    }

    Conditional --> FlagRegistry : uses
    FlagRegistry --> FeatureFlag : stores
    FeatureFlag --> ConditionalValue : contains
    ConditionalValue --> Rule : has
    Rule --> Evaluable : extends
    Evaluable <|-- Rule

    class ParseResult~T~ {
        <<sealed>>
    }

    class Success~T~ {
        +value: T
    }

    class Failure {
        +error: ParseError
    }

    ParseResult <|-- Success
    ParseResult <|-- Failure
```

---

## 2. User Flows

### 2.1 Basic Integration Flow

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant Code as Application
    participant Enum as Flag Enum
    participant Builder as ConfigBuilder
    participant Registry as FlagRegistry
    participant Ctx as Context

    Dev->>Enum: 1. Define flag keys as enum
    Note over Enum: enum class FeatureFlags : Conditional

    Dev->>Builder: 2. Configure flags using DSL
    Note over Builder: ConfigBuilder.config { ... }

    Builder->>Registry: 3. Load configuration
    Note over Registry: Immutable flag definitions stored

    Dev->>Ctx: 4. Create evaluation context
    Note over Ctx: Context(user, device, version)

    Code->>Ctx: 5. Evaluate flag
    Ctx->>Registry: 6. Retrieve flag definition
    Registry->>Ctx: 7. Return definition
    Ctx->>Ctx: 8. Execute rule matching
    Ctx->>Code: 9. Return typed value

    Note over Code: Type-safe result:<br/>Boolean, String, Int, etc.
```

### 2.2 Flag Evaluation Decision Flow

```mermaid
flowchart TD
    START([Context.evaluate]) --> FETCH[Fetch FeatureFlag from Registry]
    FETCH --> ACTIVE{Is Flag Active?}

    ACTIVE -->|No| DEFAULT[Return Default Value]
    ACTIVE -->|Yes| SORT[Sort Rules by Specificity<br/>Highest First]

    SORT --> ITERATE[Iterate Rules]
    ITERATE --> MATCH{Rule Matches<br/>Context?}

    MATCH -->|No| NEXT{More Rules?}
    MATCH -->|Yes| BUCKET[Calculate Bucket<br/>SHA-256 Hash]

    BUCKET --> ELIGIBLE{In Eligible<br/>Segment?}
    ELIGIBLE -->|No| NEXT
    ELIGIBLE -->|Yes| RETURN[Return Rule Value]

    NEXT -->|Yes| ITERATE
    NEXT -->|No| DEFAULT

    DEFAULT --> END([Return Value])
    RETURN --> END

    style START fill:#e1f5ff
    style END fill:#e1f5ff
    style MATCH fill:#fff4e1
    style ACTIVE fill:#fff4e1
    style ELIGIBLE fill:#fff4e1
    style RETURN fill:#d4edda
    style DEFAULT fill:#f8d7da
```

### 2.3 Configuration Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Defined: Developer creates flag enum

    Defined --> Configured: ConfigBuilder DSL

    Configured --> InMemory: ConfigBuilder.config { }
    Configured --> Serialized: SnapshotSerializer.serialize()

    Serialized --> Stored: Write to file/API
    Stored --> Deserialized: SnapshotSerializer.deserialize()

    Deserialized --> InMemory: FlagRegistry.load()
    InMemory --> Evaluated: Context.evaluate()

    Evaluated --> InMemory: Flag remains active

    InMemory --> Patched: KonfigPatch applied
    Patched --> InMemory: Atomic update

    InMemory --> [*]: Application shutdown
```

### 2.4 Runtime Update Flow

```mermaid
sequenceDiagram
    participant Server as Remote Server
    participant App as Application
    participant Serializer as SnapshotSerializer
    participant Registry as FlagRegistry
    participant User as User Context

    Server->>App: 1. Push configuration update (JSON)
    App->>Serializer: 2. deserializePatchToCore(json)

    alt Valid Patch
        Serializer->>App: 3. ParseResult.Success(patch)
        App->>Registry: 4. update(patch)
        Registry->>Registry: 5. Atomic CAS update
        Registry->>App: 6. Configuration updated

        User->>App: 7. Next evaluation request
        App->>Registry: 8. evaluate with new config
        Registry->>User: 9. New flag values
    else Invalid Patch
        Serializer->>App: 3. ParseResult.Failure(error)
        App->>App: 4. Log error, keep current config
        Note over App: No change to active flags
    end
```

---

## 3. Core Concepts

### 3.1 Rule Specificity & Prioritization

```mermaid
graph TD
    subgraph "Specificity Calculation"
        RULE[Rule Specificity]
        BASE[BaseEvaluable Specificity]
        EXT[Extension Evaluable Specificity]

        RULE -->|sum| BASE
        RULE -->|sum| EXT

        BASE -->|+1 if set| LOCALE[Locale Constraint]
        BASE -->|+1 if set| PLATFORM[Platform Constraint]
        BASE -->|+1 if set| VERSION[Version Constraint]

        EXT -->|custom| CUSTOM[Custom Logic Specificity]
    end

    subgraph "Evaluation Order"
        S3[Specificity 3<br/>All constraints] -->|evaluated first| S2[Specificity 2<br/>Two constraints]
        S2 --> S1[Specificity 1<br/>One constraint]
        S1 --> S0[Specificity 0<br/>No constraints]
        S0 -->|evaluated last| DEFAULT[Default Value]
    end

    style S3 fill:#d4edda
    style DEFAULT fill:#f8d7da
```

**Specificity Rules:**
- Higher specificity = evaluated first
- Tie-breaking: lexicographic by rule note
- First matching rule wins (after rollout)
- If no rules match, return default value

### 3.2 Deterministic Rollout Bucketing

```mermaid
flowchart LR
    subgraph "Input"
        KEY[Flag Key]
        STABLE[StableId]
        SALT[Salt String]
    end

    subgraph "Hashing"
        CONCAT[Concatenate:<br/>key + stableId + salt]
        HASH[SHA-256 Hash]
        BUCKET[bucket = hash % 1000]
    end

    subgraph "Decision"
        ROLLOUT[Rollout Percentage<br/>e.g., 50.0%]
        THRESHOLD[threshold = rollout * 10]
        COMPARE{bucket <<br/>threshold?}
    end

    KEY --> CONCAT
    STABLE --> CONCAT
    SALT --> CONCAT

    CONCAT --> HASH
    HASH --> BUCKET

    BUCKET --> COMPARE
    ROLLOUT --> THRESHOLD
    THRESHOLD --> COMPARE

    COMPARE -->|Yes| INCLUDE[Include in Rollout]
    COMPARE -->|No| EXCLUDE[Exclude from Rollout]

    style INCLUDE fill:#d4edda
    style EXCLUDE fill:#f8d7da
```

**Key Properties:**
- Deterministic: Same inputs → same bucket
- Uniform distribution: SHA-256 ensures randomness
- Stable: Changing salt resets bucketing
- Granular: 0.1% precision (1000 buckets)

### 3.3 Parse, Don't Validate Philosophy

```mermaid
flowchart TD
    subgraph "Traditional Validation (Anti-Pattern)"
        INPUT1[Raw Input: String]
        VALIDATE1[Validate: check constraints]
        PASS1{Valid?}
        PASS1 -->|Yes| USE1[Use String directly]
        PASS1 -->|No| ERROR1[Throw Exception]
        USE1 --> RECHECK1[Re-validate later?]

        style VALIDATE1 fill:#f8d7da
        style RECHECK1 fill:#f8d7da
    end

    subgraph "Parse, Don't Validate (Konditional)"
        INPUT2[Raw Input: String]
        PARSE[Parse into Domain Type]
        RESULT{ParseResult}
        RESULT -->|Success| DOMAIN[Typed Domain Object<br/>e.g., Version, Rollout]
        RESULT -->|Failure| ERROR2[ParseError<br/>Structured Error]
        DOMAIN --> USE2[Use with Confidence<br/>No Re-checking]

        style PARSE fill:#d4edda
        style DOMAIN fill:#d4edda
        style USE2 fill:#d4edda
    end

    INPUT1 -.->|Old Way| INPUT2
```

**Benefits:**
- **Type Safety**: Constraints encoded in types
- **No Redundant Checks**: Parse once at boundary
- **Composability**: Parsed types compose cleanly
- **Explicit Errors**: Structured error types
- **Compiler Support**: Type system prevents misuse

### 3.4 Extension Point Hierarchy

```mermaid
graph TB
    subgraph "Core Framework"
        COND[Conditional~S,C~]
        CTX[Context]
        EVAL[Evaluable~C~]
        REG[FlagRegistry]
    end

    subgraph "User Extensions"
        CUSTOM_COND[Custom Flag Keys<br/>enum class MyFlags]
        CUSTOM_CTX[Custom Context<br/>+ domain fields]
        CUSTOM_EVAL[Custom Evaluators<br/>+ business logic]
        CUSTOM_REG[Custom Registry<br/>+ persistence]
    end

    COND -.implements.-> CUSTOM_COND
    CTX -.implements.-> CUSTOM_CTX
    EVAL -.extends.-> CUSTOM_EVAL
    REG -.implements.-> CUSTOM_REG

    CUSTOM_COND -->|works with| CUSTOM_CTX
    CUSTOM_CTX -->|evaluated by| CUSTOM_EVAL
    CUSTOM_EVAL -->|stored in| CUSTOM_REG

    style CUSTOM_COND fill:#e1f5ff
    style CUSTOM_CTX fill:#e1f5ff
    style CUSTOM_EVAL fill:#e1f5ff
    style CUSTOM_REG fill:#e1f5ff
```

---

## 4. Public API Reference

### 4.1 Core Types

#### **Conditional<S, C>**
**Purpose**: Type-safe feature flag identifier

```kotlin
interface Conditional<S : Any, C : Context> {
    val key: String
    val registry: FlagRegistry
}
```

**Usage Pattern:**
```kotlin
enum class FeatureFlags(override val key: String) : Conditional<Boolean, AppContext> {
    DARK_MODE("dark_mode"),
    NEW_CHECKOUT("new_checkout");

    override val registry = FlagRegistry
}
```

**Type Parameters:**
- `S` - Value type (Boolean, String, Int, Double, custom)
- `C` - Context type for evaluation

---

#### **Context**
**Purpose**: Provides evaluation context for targeting

```kotlin
interface Context {
    val locale: AppLocale
    val platform: Platform
    val appVersion: Version
    val stableId: StableId
}
```

**Factory:**
```kotlin
Context(
    locale = AppLocale.EN_US,
    platform = Platform.ANDROID,
    appVersion = Version(1, 2, 3),
    stableId = HexId.from("user-123")
)
```

**Extension:**
```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val organizationId: String,
    val subscriptionTier: Tier
) : Context
```

---

#### **FlagRegistry**
**Purpose**: Thread-safe configuration store

```kotlin
interface FlagRegistry {
    fun load(config: Konfig)
    fun update(patch: KonfigPatch)
    fun <S, C> update(definition: FeatureFlag<S, C>)
    fun konfig(): Konfig
    fun <S, C> featureFlag(key: Conditional<S, C>): FeatureFlag<S, C>?
    fun allFlags(): Map<Conditional<*, *>, FeatureFlag<*, *>>
}
```

**Default Instance:**
```kotlin
FlagRegistry // Singleton via companion object
```

---

### 4.2 DSL Builders

#### **ConfigBuilder**
**Purpose**: Declarative configuration DSL

```kotlin
ConfigBuilder.config {
    DARK_MODE with {
        default(false)

        rule {
            platforms(Platform.IOS)
            versions { min(2, 0) }
            rollout = Rollout.of(50.0)
        } implies true

        rule {
            locales(AppLocale.EN_US, AppLocale.EN_GB)
            rollout = Rollout.MAX
        } implies true

        salt("v2")
    }
}
```

**Methods:**
- `config(registry, fn)` - Build and load
- `buildSnapshot(fn)` - Build without loading (testing)

---

#### **RuleBuilder<C>**
**Purpose**: Type-safe rule definition

```kotlin
rule {
    locales(AppLocale.EN_US, AppLocale.FR_FR)
    platforms(Platform.ANDROID, Platform.IOS)
    versions {
        min(1, 0)
        max(2, 5, 10)
    }
    rollout = Rollout.of(25.0)
    note("Q4 2024 Rollout")

    extension {
        CustomEvaluator(/* ... */)
    }
}
```

**Properties:**
- `locales(...)` - Target specific locales
- `platforms(...)` - Target specific platforms
- `versions { }` - Version range constraints
- `rollout` - Percentage (0-100)
- `note` - Description (for tie-breaking)
- `extension { }` - Custom evaluator

---

### 4.3 Evaluation API

#### **Extension Functions**

```kotlin
// Evaluate single flag
fun <S, C : Context> C.evaluate(
    key: Conditional<S, C>,
    registry: FlagRegistry = FlagRegistry
): S

// Evaluate all flags
fun <C : Context> C.evaluate(
    registry: FlagRegistry = FlagRegistry
): Map<Conditional<*, *>, Any?>
```

**Usage:**
```kotlin
val context = Context(
    locale = AppLocale.EN_US,
    platform = Platform.ANDROID,
    appVersion = Version(1, 5, 0),
    stableId = HexId.from(userId)
)

// Single flag
val isDarkMode = context.evaluate(FeatureFlags.DARK_MODE)

// All flags
val allValues = context.evaluate()
```

---

### 4.4 Serialization API

#### **SnapshotSerializer**

```kotlin
class SnapshotSerializer(moshi: Moshi = defaultMoshi()) {
    // Serialize full config
    fun serialize(konfig: Konfig): String

    // Deserialize full config
    fun deserialize(json: String): ParseResult<Konfig>

    // Serialize patch
    fun serializePatch(patch: KonfigPatch): String

    // Deserialize patch
    fun deserializePatchToCore(json: String): ParseResult<KonfigPatch>

    // Apply patch to existing config
    fun applyPatch(
        currentKonfig: Konfig,
        patch: SerializablePatch
    ): ParseResult<Konfig>

    // Apply patch from JSON
    fun applyPatchJson(
        currentKonfig: Konfig,
        patchJson: String
    ): ParseResult<Konfig>
}
```

**Full Serialization Flow:**
```kotlin
val serializer = SnapshotSerializer()

// Serialize
val json = serializer.serialize(FlagRegistry.konfig())

// Deserialize
when (val result = serializer.deserialize(json)) {
    is ParseResult.Success -> FlagRegistry.load(result.value)
    is ParseResult.Failure -> handleError(result.error)
}
```

**Incremental Update Flow:**
```kotlin
val patchJson = fetchFromServer()

when (val result = serializer.applyPatchJson(FlagRegistry.konfig(), patchJson)) {
    is ParseResult.Success -> FlagRegistry.load(result.value)
    is ParseResult.Failure -> handleError(result.error)
}
```

---

#### **ConditionalRegistry**

```kotlin
object ConditionalRegistry {
    fun <S, C> register(conditional: Conditional<S, C>)

    inline fun <reified T> registerEnum()
        where T : Enum<T>, T : Conditional<*, *>

    fun get(key: String): ParseResult<Conditional<*, *>>
    fun contains(key: String): Boolean
}
```

**Registration (Required before deserialization):**
```kotlin
fun main() {
    ConditionalRegistry.registerEnum<FeatureFlags>()
    ConditionalRegistry.registerEnum<ApiConfig>()

    // Now safe to deserialize
    val result = serializer.deserialize(json)
}
```

---

### 4.5 Result Types

#### **ParseResult<T>**

```kotlin
sealed interface ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>
    data class Failure(val error: ParseError) : ParseResult<Nothing>
}
```

**Usage:**
```kotlin
when (val result = serializer.deserialize(json)) {
    is ParseResult.Success -> {
        val konfig = result.value
        FlagRegistry.load(konfig)
    }
    is ParseResult.Failure -> {
        when (val error = result.error) {
            is ParseError.InvalidJson -> log("Invalid JSON: ${error.message}")
            is ParseError.ConditionalNotFound -> log("Unknown key: ${error.key}")
            is ParseError.InvalidSnapshot -> log("Invalid format: ${error.message}")
        }
    }
}
```

---

## 5. Extension Points

### 5.1 Custom Context Types

**Use Case**: Add domain-specific targeting dimensions

```mermaid
classDiagram
    class Context {
        <<interface>>
        +locale: AppLocale
        +platform: Platform
        +appVersion: Version
        +stableId: StableId
    }

    class EnterpriseContext {
        +organizationId: String
        +subscriptionTier: Tier
        +teamSize: Int
        +features: Set~String~
    }

    class GamingContext {
        +playerLevel: Int
        +guildId: String?
        +isPremium: Boolean
    }

    Context <|-- EnterpriseContext
    Context <|-- GamingContext
```

**Implementation:**
```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val organizationId: String,
    val subscriptionTier: SubscriptionTier
) : Context

enum class EnterpriseFlags(override val key: String)
    : Conditional<Boolean, EnterpriseContext> {
    ADVANCED_ANALYTICS("advanced_analytics"),
    SSO_LOGIN("sso_login");

    override val registry = FlagRegistry
}
```

---

### 5.2 Custom Evaluators

**Use Case**: Implement custom targeting logic

```mermaid
classDiagram
    class Evaluable~C~ {
        <<abstract>>
        +matches(context): Boolean
        +specificity(): Int
    }

    class BaseEvaluable~C~ {
        +locales: Set~AppLocale~
        +platforms: Set~Platform~
        +versionRange: VersionRange
    }

    class PremiumUserEvaluable {
        +matches(context): Boolean
        +specificity(): Int
    }

    class GuildMemberEvaluable {
        +guildIds: Set~String~
        +matches(context): Boolean
        +specificity(): Int
    }

    Evaluable <|-- BaseEvaluable
    Evaluable <|-- PremiumUserEvaluable
    Evaluable <|-- GuildMemberEvaluable
```

**Implementation:**
```kotlin
class PremiumUserEvaluable : Evaluable<EnterpriseContext>() {
    override fun matches(context: EnterpriseContext): Boolean {
        return context.subscriptionTier == SubscriptionTier.PREMIUM
    }

    override fun specificity(): Int = 1
}

// Usage in rules
ConfigBuilder.config {
    ADVANCED_ANALYTICS with {
        default(false)

        rule {
            extension { PremiumUserEvaluable() }
            rollout = Rollout.MAX
        } implies true
    }
}
```

---

### 5.3 Custom Value Types

**Use Case**: Return complex configuration objects

```kotlin
data class ThemeConfig(
    val primaryColor: String,
    val secondaryColor: String,
    val fontFamily: String
)

enum class ThemeFlags(override val key: String)
    : Conditional<ThemeConfig, Context> {
    APP_THEME("app_theme");

    override val registry = FlagRegistry
}

ConfigBuilder.config {
    ThemeFlags.APP_THEME with {
        default(ThemeConfig("#000", "#fff", "Roboto"))

        rule {
            locales(AppLocale.JA_JP)
        } implies ThemeConfig("#e60012", "#ffffff", "Noto Sans JP")
    }
}
```

---

### 5.4 Custom Registry Implementation

**Use Case**: Alternative storage backends

```kotlin
class DistributedFlagRegistry(
    private val cacheClient: CacheClient,
    private val eventBus: EventBus
) : FlagRegistry {

    override fun load(config: Konfig) {
        cacheClient.set("flags:snapshot", config)
        eventBus.publish(ConfigUpdatedEvent(config))
    }

    override fun <S, C> featureFlag(
        key: Conditional<S, C>
    ): FeatureFlag<S, C>? {
        val snapshot = cacheClient.get<Konfig>("flags:snapshot")
        return snapshot?.flags?.get(key) as? FeatureFlag<S, C>
    }

    // ... other methods
}
```

---

## 6. Key User Journeys

### Journey 1: First-Time Integration (5 minutes)

```mermaid
journey
    title First-Time Integration Journey
    section Setup
        Add dependency: 5: Developer
        Create flag enum: 5: Developer
        Configure flags: 4: Developer
    section Testing
        Create test context: 5: Developer
        Evaluate flags: 5: Developer
        Verify behavior: 5: Developer
    section Production
        Load from JSON: 4: Developer
        Deploy to prod: 5: Developer
```

**Steps:**
1. Add Gradle dependency
2. Define flag enum implementing `Conditional`
3. Configure flags with `ConfigBuilder.config { }`
4. Create `Context` instance
5. Call `context.evaluate(FLAG_KEY)`

**Time to First Value**: ~5 minutes

---

### Journey 2: Adding Dynamic Configuration

```mermaid
journey
    title Dynamic Configuration Journey
    section Development
        Export config to JSON: 5: Developer
        Set up config server: 4: DevOps
        Implement fetch logic: 4: Developer
    section Runtime
        Fetch JSON from server: 5: App
        Deserialize config: 5: App
        Load into registry: 5: App
        Flags immediately active: 5: User
    section Updates
        Push new config: 5: Ops
        App fetches patch: 5: App
        Apply atomically: 5: App
        Users see changes: 5: User
```

**Steps:**
1. Serialize initial config: `serializer.serialize(konfig)`
2. Store JSON in config management system
3. Implement fetch logic in application
4. Register flag enums: `ConditionalRegistry.registerEnum<T>()`
5. Deserialize and load: `serializer.deserialize(json)`
6. For updates, use `applyPatchJson(currentKonfig, patchJson)`

---

### Journey 3: Adding Custom Targeting

```mermaid
journey
    title Custom Targeting Journey
    section Design
        Identify targeting needs: 4: Product
        Design context extension: 5: Developer
        Design evaluator: 4: Developer
    section Implementation
        Extend Context interface: 5: Developer
        Implement Evaluable: 5: Developer
        Update flag definitions: 4: Developer
    section Testing
        Test matching logic: 5: Developer
        Test specificity: 4: Developer
        Verify in prod: 5: Developer
```

---

## 7. Comparison with Alternatives

### Konditional vs. String-Based Systems

```mermaid
graph LR
    subgraph "String-Based (LaunchDarkly, etc.)"
        S1[String Key] -->|runtime lookup| S2{Exists?}
        S2 -->|No| S3[Runtime Error]
        S2 -->|Yes| S4[Untyped Value]
        S4 -->|manual cast| S5[Usage]

        style S3 fill:#f8d7da
        style S4 fill:#fff3cd
    end

    subgraph "Konditional (Type-Safe)"
        K1[Typed Enum] -->|compile-time check| K2[Typed Value]
        K2 -->|direct usage| K3[Usage]

        style K1 fill:#d4edda
        style K2 fill:#d4edda
        style K3 fill:#d4edda
    end
```

**Benefits:**
- **Compile-time safety**: Typos caught at compile time
- **Refactoring support**: IDE can rename across codebase
- **Type safety**: No runtime casting or type errors
- **Autocomplete**: Full IDE support

---

## 8. Advanced Topics

### 8.1 Thread Safety Guarantees

```mermaid
sequenceDiagram
    participant T1 as Thread 1
    participant T2 as Thread 2
    participant Reg as FlagRegistry<br/>(AtomicReference)
    participant Snap as Konfig<br/>(Immutable)

    T1->>Reg: evaluate(FLAG_A)
    Reg->>Snap: Read snapshot

    Note over T2: Concurrent update
    T2->>Reg: load(newKonfig)
    Reg->>Reg: AtomicReference.set()

    Snap->>T1: Return value from old snapshot

    T1->>Reg: evaluate(FLAG_B)
    Reg->>Snap: Read NEW snapshot
    Snap->>T1: Return value from new snapshot

    Note over Reg: Lock-free reads<br/>Atomic writes<br/>No torn reads
```

**Guarantees:**
- Lock-free reads (no contention)
- Atomic snapshot updates (CAS)
- No torn reads (immutable snapshots)
- Eventually consistent (new evaluations see new config)

---

### 8.2 Serialization Format

**Full Snapshot JSON:**
```json
{
  "flags": [
    {
      "key": "dark_mode",
      "default": true,
      "isActive": true,
      "salt": "v1",
      "rules": [
        {
          "value": false,
          "rollout": 50.0,
          "note": "Gradual rollout",
          "locales": ["en_US"],
          "platforms": ["ANDROID"],
          "versionRange": {
            "type": "FullyBound",
            "min": "1.0.0",
            "max": "2.0.0"
          }
        }
      ]
    }
  ]
}
```

**Patch JSON:**
```json
{
  "add": [
    {
      "key": "new_feature",
      "default": false,
      "isActive": true,
      "salt": "v1",
      "rules": []
    }
  ],
  "remove": ["deprecated_flag"]
}
```

---

## 9. Migration Patterns

### From String-Based System

```kotlin
// Before (String-based)
val isEnabled = featureFlags.getBoolean("dark_mode", false)

// After (Konditional)
enum class FeatureFlags(override val key: String) : Conditional<Boolean, Context> {
    DARK_MODE("dark_mode");
    override val registry = FlagRegistry
}

val isEnabled = context.evaluate(FeatureFlags.DARK_MODE)
```

### From If/Else Configuration

```kotlin
// Before
fun getTheme(user: User): Theme {
    return if (user.isPremium) {
        Theme.PREMIUM
    } else if (user.locale == "ja_JP") {
        Theme.JAPANESE
    } else {
        Theme.DEFAULT
    }
}

// After
ConfigBuilder.config {
    THEME_CONFIG with {
        default(Theme.DEFAULT)

        rule {
            extension { PremiumUserEvaluable() }
        } implies Theme.PREMIUM

        rule {
            locales(AppLocale.JA_JP)
        } implies Theme.JAPANESE
    }
}

val theme = context.evaluate(ConfigFlags.THEME_CONFIG)
```

---

## 10. Testing Strategies

### 10.1 Unit Testing Flags

```kotlin
@Test
fun `dark mode is enabled for premium iOS users on v2+`() {
    // Arrange
    val testRegistry = FakeRegistry()
    ConfigBuilder.config(testRegistry) {
        DARK_MODE with {
            default(false)
            rule {
                platforms(Platform.IOS)
                versions { min(2, 0) }
                extension { PremiumUserEvaluable() }
            } implies true
        }
    }

    val context = TestContext(
        platform = Platform.IOS,
        appVersion = Version(2, 1, 0),
        isPremium = true
    )

    // Act
    val result = context.evaluate(FeatureFlags.DARK_MODE, testRegistry)

    // Assert
    assertThat(result).isTrue()
}
```

### 10.2 Testing Rollouts

```kotlin
@Test
fun `50 percent rollout distributes users evenly`() {
    ConfigBuilder.config {
        FEATURE with {
            default(false)
            rule { rollout = Rollout.of(50.0) } implies true
        }
    }

    val userIds = (1..1000).map { "user-$it" }
    val contexts = userIds.map { userId ->
        Context(
            locale = AppLocale.EN_US,
            platform = Platform.ANDROID,
            appVersion = Version(1, 0, 0),
            stableId = HexId.from(userId)
        )
    }

    val enabledCount = contexts.count { it.evaluate(FEATURE) }

    // Should be ~50% (with statistical variance)
    assertThat(enabledCount).isIn(450..550)
}
```

### 10.3 Serialization Round-Trip Testing

```kotlin
@Test
fun `serialization round-trip preserves configuration`() {
    val original = ConfigBuilder.buildSnapshot {
        MY_FLAG with {
            default(true)
            rule { /* ... */ } implies false
        }
    }

    val serializer = SnapshotSerializer()
    val json = serializer.serialize(original)

    val parsed = serializer.deserialize(json)

    assertThat(parsed).isInstanceOf<ParseResult.Success>()
    val restored = (parsed as ParseResult.Success).value

    assertThat(restored.flags).isEqualTo(original.flags)
}
```

---

## Summary

This documentation structure provides:

1. **Clear Visual Architecture** - Mermaid diagrams for system understanding
2. **User-Focused Flows** - Step-by-step guidance for common tasks
3. **Comprehensive API Reference** - Public interfaces with examples
4. **Extension Points** - Clear customization pathways
5. **Comparison Context** - Why Konditional vs alternatives
6. **Testing Guidance** - Patterns for reliable tests
7. **Migration Support** - Paths from existing systems

**Next Steps:**
1. Generate detailed content for each section
2. Add code examples for every public API
3. Create interactive tutorials
4. Build quickstart templates
5. Add troubleshooting guide
