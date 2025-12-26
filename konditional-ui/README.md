# Konditional UI - React Component Library

**Type-safe, reusable UI components for building Konditional configuration editors and viewers.**

The `konditional-ui` module is a Kotlin Multiplatform JavaScript library providing React functional components for rendering and editing Konditional feature flag configurations. It uses MUI (Material-UI) for styling and includes rich editors for complex data structures.

## Features

### Rich Field Editors
- **Primitive types**: Boolean (checkbox/toggle), String (text/textarea), Number (int/double), Enum (select/radio)
- **Complex types**:
  - **Array Editor**: Add/remove items, reorder with up/down buttons, recursive nested editing
  - **Object Editor**: Collapsible MUI Accordion sections, required field indicators, additional properties support
- **Validation**: Real-time validation with severity levels (ERROR, WARNING, INFO), inline feedback, error summary panel

### Component Architecture
All components are React Functional Components (FC) with typed props using MUI Material Design:

```kotlin
external interface FieldEditorProps : Props {
    var fieldType: FieldTypeDto
    var descriptor: FieldDescriptorDto
    var value: JsonElement?
    var onChange: (JsonElement) -> Unit
    var path: String
    var validationErrors: List<ValidationError>?
}

val FieldEditor: FC<FieldEditorProps> = FC { props ->
    // Polymorphic editor that dispatches to specialized editors
    // based on descriptor type (Boolean, String, Number, Array, Object, etc.)
}
```

### Type-Safe Descriptors
Configuration schemas are defined using sealed interfaces with kotlinx-serialization polymorphism:

```kotlin
@Serializable
sealed interface FieldDescriptorDto {
    val uiHints: UiHintsDto
}

@Serializable
@SerialName("BOOLEAN")
data class BooleanDescriptorDto(
    override val uiHints: UiHintsDto,
    val defaultValue: Boolean? = null,
) : FieldDescriptorDto

@Serializable
@SerialName("ARRAY")
data class ArrayDescriptorDto(
    override val uiHints: UiHintsDto,
    val itemDescriptor: FieldDescriptorDto,
    val minItems: Int? = null,
    val maxItems: Int? = null,
    val uniqueItems: Boolean = false,
) : FieldDescriptorDto

@Serializable
@SerialName("OBJECT")
data class ObjectDescriptorDto(
    override val uiHints: UiHintsDto,
    val properties: Map<String, PropertyDescriptorDto>,
    val required: Set<String> = emptySet(),
    val additionalProperties: FieldDescriptorDto? = null,
) : FieldDescriptorDto
```

### JSON Pointer Navigation
RFC-6901 compliant JSON Pointer utilities for navigating and modifying deeply nested structures:

```kotlin
object JsonPointer {
    fun get(root: JsonElement, pointer: String): Result<JsonElement>
    fun set(root: JsonElement, pointer: String, value: JsonElement): Result<JsonElement>
    fun append(root: JsonElement, pointer: String, value: JsonElement): Result<JsonElement>
    fun removeAt(root: JsonElement, pointer: String, index: Int): Result<JsonElement>
    fun move(root: JsonElement, pointer: String, fromIndex: Int, toIndex: Int): Result<JsonElement>
    fun expandTemplate(root: JsonElement, template: String): Result<List<String>>
}
```

Examples:
```kotlin
// Get nested value
JsonPointer.get(config, "/features/darkMode/enabled")  // Result<JsonElement>

// Set nested value
JsonPointer.set(config, "/features/darkMode/enabled", JsonPrimitive(true))

// Work with arrays
JsonPointer.append(config, "/features/flags", newFlag)
JsonPointer.removeAt(config, "/features/flags", index = 2)
JsonPointer.move(config, "/features/flags", fromIndex = 0, toIndex = 2)

// Template expansion for bindings
JsonPointer.expandTemplate(config, "/features/*/enabled")
// Returns: ["/features/darkMode/enabled", "/features/analytics/enabled", ...]
```

### Validation System
Type-safe validation with granular error reporting:

```kotlin
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val errors: List<ValidationError>) : ValidationResult

    fun errors(): List<ValidationError>
    fun isValid(): Boolean
}

data class ValidationError(
    val path: String,
    val message: String,
    val severity: Severity = Severity.ERROR,
)

enum class Severity { ERROR, WARNING, INFO }

// Validate any field against its descriptor
fun validate(value: JsonElement, descriptor: FieldDescriptorDto, path: String = "/"): ValidationResult
```

Example validation rules:
- **Required fields**: Object properties marked as required must be present
- **Array constraints**: minItems, maxItems, uniqueItems
- **Number ranges**: minimum, maximum (inclusive/exclusive)
- **String constraints**: minLength, maxLength, pattern (regex)
- **Enum validation**: Value must be in allowed options
- **Type checking**: Value must match descriptor type

### Default Value Generation
Generate sensible defaults from descriptors for new fields:

```kotlin
object DefaultValueGenerator {
    // Generate minimal valid default
    fun generate(descriptor: FieldDescriptorDto): JsonElement

    // Generate interesting sample value for previews
    fun sample(descriptor: FieldDescriptorDto): JsonElement
}
```

Examples:
```kotlin
val boolDesc = BooleanDescriptorDto(uiHints = UiHintsDto(label = "Enabled"), defaultValue = true)
DefaultValueGenerator.generate(boolDesc)  // JsonPrimitive(true)

val arrayDesc = ArrayDescriptorDto(
    uiHints = UiHintsDto(label = "Items"),
    itemDescriptor = StringDescriptorDto(...),
    minItems = 1
)
DefaultValueGenerator.generate(arrayDesc)  // JsonArray with 1 empty string

DefaultValueGenerator.sample(arrayDesc)  // JsonArray with interesting sample data
```

## Module Structure

```
konditional-ui/
├── build.gradle.kts                     # KMP JS library configuration
└── src/jsMain/kotlin/io/amichne/konditional/ui/
    ├── components/
    │   ├── ArrayFieldEditor.kt          # Array editing with up/down reordering
    │   ├── BindingsTable.kt             # Display JSON Pointer → FieldType bindings
    │   ├── DescriptorCatalog.kt         # Preview all field types with live editors
    │   ├── EditableFieldsPanel.kt       # Main panel with bound fields + validation
    │   ├── FieldEditor.kt               # Polymorphic editor (dispatches to specialized editors)
    │   ├── ObjectFieldEditor.kt         # Object editor with MUI Accordions
    │   ├── SnapshotJsonPanel.kt         # Pretty-printed JSON with copy-to-clipboard
    │   └── ValidationErrorSummary.kt    # Error summary with severity grouping
    ├── defaults/
    │   └── DefaultValueGenerator.kt     # Generate defaults from descriptors
    ├── external/
    │   └── DndKit.kt                    # External declarations for @dnd-kit (future drag-drop)
    ├── json/
    │   └── JsonPointer.kt               # RFC-6901 JSON Pointer utilities
    ├── model/
    │   └── ConfigurationStateDtos.kt    # Descriptor DTOs (serializable schema)
    ├── state/
    │   └── RemoteData.kt                # Async state ADT (Idle, Loading, Loaded, Failed)
    └── validation/
        └── FieldValidation.kt           # Validation logic with severity levels
```

## Installation

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":konditional-ui"))
}
```

The module transitively provides:
- `kotlinx-serialization-json` for JSON handling
- Kotlin React wrappers (`kotlin-wrappers-react`)
- MUI Material Design components (`kotlin-wrappers-mui-material`)
- Emotion styling (`kotlin-wrappers-emotion`)

## Usage Examples

### Basic Field Editor

```kotlin
import io.amichne.konditional.ui.components.FieldEditor
import io.amichne.konditional.ui.model.*

val FieldEditorExample: FC<Props> = FC {
    val descriptor = BooleanDescriptorDto(
        uiHints = UiHintsDto(
            label = "Dark Mode",
            hint = "Enable dark theme"
        ),
        defaultValue = false
    )

    val (value, setValue) = useState(JsonPrimitive(false))

    FieldEditor {
        fieldType = FieldTypeDto.FLAG_VALUE
        this.descriptor = descriptor
        this.value = value
        onChange = { setValue(it) }
        path = "/darkMode"
    }
}
```

### Editable Fields Panel with Validation

```kotlin
import io.amichne.konditional.ui.components.EditableFieldsPanel
import io.amichne.konditional.ui.model.*

val ConfigEditor: FC<Props> = FC {
    val supportedValues = SupportedValuesDto(
        byType = mapOf(
            "boolean" to BooleanDescriptorDto(...),
            "string" to StringDescriptorDto(...),
            "array" to ArrayDescriptorDto(...)
        ),
        bindings = mapOf(
            "/features/*/enabled" to FieldTypeDto.FLAG_VALUE,
            "/settings/theme" to FieldTypeDto.FLAG_VALUE
        )
    )

    val initialSnapshot = /* load from API */

    EditableFieldsPanel {
        this.initialSnapshot = initialSnapshot
        this.supportedValues = supportedValues
        onSnapshotChange = { newSnapshot ->
            // Save or sync newSnapshot
        }
    }
}
```

### Array Editor

```kotlin
import io.amichne.konditional.ui.components.ArrayFieldEditor
import io.amichne.konditional.ui.model.*

val TagsEditor: FC<Props> = FC {
    val descriptor = ArrayDescriptorDto(
        uiHints = UiHintsDto(label = "Tags"),
        itemDescriptor = StringDescriptorDto(
            uiHints = UiHintsDto(label = "Tag")
        ),
        minItems = 1,
        maxItems = 10
    )

    val (tags, setTags) = useState(JsonArray(listOf(JsonPrimitive("kotlin"))))

    ArrayFieldEditor {
        this.descriptor = descriptor
        value = tags
        onChange = { setTags(it as JsonArray) }
        path = "/tags"
    }
}
```

### Object Editor with Accordions

```kotlin
import io.amichne.konditional.ui.components.ObjectFieldEditor
import io.amichne.konditional.ui.model.*

val UserSettingsEditor: FC<Props> = FC {
    val descriptor = ObjectDescriptorDto(
        uiHints = UiHintsDto(label = "User Settings"),
        properties = mapOf(
            "theme" to PropertyDescriptorDto(
                descriptor = EnumOptionsDescriptorDto(...),
                order = 0
            ),
            "notifications" to PropertyDescriptorDto(
                descriptor = BooleanDescriptorDto(...),
                order = 1
            )
        ),
        required = setOf("theme")
    )

    val (settings, setSettings) = useState(JsonObject(...))

    ObjectFieldEditor {
        this.descriptor = descriptor
        value = settings
        onChange = { setSettings(it as JsonObject) }
        path = "/settings"
    }
}
```

### Validation Error Summary

```kotlin
import io.amichne.konditional.ui.components.ValidationErrorSummary
import io.amichne.konditional.ui.validation.*

val ValidationExample: FC<Props> = FC {
    val errors = listOf(
        ValidationError(
            path = "/features/darkMode/enabled",
            message = "Value is required",
            severity = Severity.ERROR
        ),
        ValidationError(
            path = "/settings/theme",
            message = "Consider using 'auto' for better UX",
            severity = Severity.INFO
        )
    )

    ValidationErrorSummary {
        this.errors = errors
        maxVisible = 5
    }
}
```

## Component Reference

### Core Components

| Component | Purpose | Key Props |
|-----------|---------|-----------|
| `FieldEditor` | Polymorphic editor for any field type | `descriptor`, `value`, `onChange`, `path` |
| `ArrayFieldEditor` | Array editing with add/remove/reorder | `descriptor: ArrayDescriptorDto`, `value: JsonArray` |
| `ObjectFieldEditor` | Object editing with accordions | `descriptor: ObjectDescriptorDto`, `value: JsonObject` |
| `EditableFieldsPanel` | Full panel with bindings + validation | `initialSnapshot`, `supportedValues`, `onSnapshotChange` |
| `ValidationErrorSummary` | Error summary grouped by severity | `errors`, `maxVisible` |
| `SnapshotJsonPanel` | Pretty JSON with copy button | `snapshot`, `title` |
| `DescriptorCatalog` | Preview all field types | `descriptors` |
| `BindingsTable` | Display bindings table | `bindings` |

### Descriptor Types

| Descriptor | Use Case | Key Properties |
|------------|----------|----------------|
| `BooleanDescriptorDto` | Checkbox, toggle | `defaultValue` |
| `StringDescriptorDto` | Text input, textarea | `minLength`, `maxLength`, `pattern` |
| `NumberRangeDescriptorDto` | Integer/double input | `minimum`, `maximum`, `step` |
| `EnumOptionsDescriptorDto` | Select, radio buttons | `options: List<OptionDto>` |
| `ArrayDescriptorDto` | Dynamic list | `itemDescriptor`, `minItems`, `maxItems` |
| `ObjectDescriptorDto` | Nested object | `properties`, `required`, `additionalProperties` |

### UI Control Types

```kotlin
enum class UiControlTypeDto {
    CHECKBOX,           // Boolean as checkbox
    TOGGLE,             // Boolean as toggle switch
    TEXT_INPUT,         // Single-line text
    TEXT_AREA,          // Multi-line text
    SELECT_DROPDOWN,    // Enum as dropdown
    RADIO_GROUP,        // Enum as radio buttons
    NUMBER_INPUT,       // Int/Double input
    ARRAY_EDITOR,       // Array with add/remove/reorder
    OBJECT_EDITOR,      // Object with accordions
    NESTED_OBJECT,      // Inline nested object (no accordion)
}
```

## Advanced Features

### Custom Validation Rules

Extend validation by creating custom validators:

```kotlin
fun validateCustom(value: JsonElement, descriptor: FieldDescriptorDto, path: String): ValidationResult {
    val baseValidation = validate(value, descriptor, path)
    if (!baseValidation.isValid()) return baseValidation

    // Add custom rules
    when (descriptor) {
        is StringDescriptorDto -> {
            val str = (value as? JsonPrimitive)?.content ?: return ValidationResult.Valid
            if (str.contains("profanity")) {
                return ValidationResult.Invalid(listOf(
                    ValidationError(path, "Contains inappropriate content", Severity.WARNING)
                ))
            }
        }
    }

    return ValidationResult.Valid
}
```

### Async Data Loading

Use `RemoteData` ADT for async state management:

```kotlin
import io.amichne.konditional.ui.state.RemoteData

val AsyncConfigEditor: FC<Props> = FC {
    val (configState, setConfigState) = useState<RemoteData<JsonElement>>(RemoteData.Idle)

    useEffect {
        setConfigState(RemoteData.Loading)
        GlobalScope.launch {
            try {
                val config = fetchConfig()
                setConfigState(RemoteData.Loaded(config))
            } catch (e: Exception) {
                setConfigState(RemoteData.Failed(e.message ?: "Unknown error"))
            }
        }
    }

    when (val state = configState) {
        is RemoteData.Idle -> +"Idle"
        is RemoteData.Loading -> +"Loading..."
        is RemoteData.Loaded -> EditableFieldsPanel { initialSnapshot = state.value }
        is RemoteData.Failed -> +"Error: ${state.message}"
    }
}
```

## Building

```bash
# Compile Kotlin/JS
./gradlew :konditional-ui:compileKotlinJs

# Run tests (if any)
./gradlew :konditional-ui:jsTest

# Clean build
./gradlew :konditional-ui:clean :konditional-ui:compileKotlinJs
```

## Example Apps

See [`ktor-demo/demo-client`](../ktor-demo/demo-client) for a full example using `konditional-ui`:
- **ConfigState Catalog App**: Interactive UI catalog at `/configstate/catalog`
- Live editing of configuration snapshots
- Real-time validation feedback
- JSON Pointer template expansion

## Dependencies

### Required
- `kotlinx-serialization-json` 1.8.0+
- `kotlin-wrappers` (React, ReactDOM, MUI, Emotion)
- `@dnd-kit/*` 6.1.0+ (npm, for future drag-and-drop features)

### Transitive
All React and MUI dependencies are provided transitively. Consumer projects only need to depend on `:konditional-ui`.

## Contributing

When adding new field types:
1. Add descriptor DTO in `model/ConfigurationStateDtos.kt`
2. Add FieldTypeDto enum case
3. Implement specialized editor in `components/`
4. Add case to `FieldEditor.kt` dispatcher
5. Add validation logic in `validation/FieldValidation.kt`
6. Add default generation in `defaults/DefaultValueGenerator.kt`

## License

MIT License - see [LICENSE](../LICENSE) file
