# Konditional Demo - Full-Stack Kotlin Application

An interactive web application demonstrating the Konditional feature flags library with type-safe Kotlin on both client and server.

This demo includes **two applications**:
1. **Feature Evaluation Demo** - Interactive feature flag evaluation at `/`
2. **ConfigState Catalog** - Rich UI component catalog at `/configstate/catalog`

## Architecture

This demo uses **Kotlin Multiplatform** with separate modules for client and server:

```
ktor-demo/
├── src/main/kotlin/          # JVM server code (Ktor)
│   └── io/amichne/konditional/demo/
│       ├── Application.kt    # Server routes & HTML generation
│       ├── DemoFeatures.kt   # Feature definitions
│       └── DemoContexts.kt   # Context types
└── demo-client/              # Kotlin/JS client module
    └── src/main/kotlin/io/amichne/konditional/demo/
        ├── client/
        │   └── DemoClient.kt      # Feature evaluation demo
        ├── catalog/
        │   ├── ConfigStateCatalogApp.kt    # ConfigState UI catalog (React)
        │   └── ConfigStateCatalogClient.kt # API client for catalog
        └── net/
            └── ConfigStateApi.kt  # Shared API types
```

The demo-client depends on the `konditional-ui` module (see [`../konditional-ui/README.md`](../konditional-ui/README.md)) for reusable React components.

## Key Features

### ✨ Type-Safe Client Code with Kotlin/JS
Instead of embedding raw JavaScript strings in HTML, the demo uses **Kotlin/JS** for maintainable, type-safe client code:

**Before**:
```kotlin
script {
    unsafe {
        raw("""
            let hotReloadEnabled = false;
            // ... hundreds of lines of JavaScript
            // No type safety, hard to maintain
        """)
    }
}
```

**After**:
```kotlin
// In DemoClient.kt - pure Kotlin!
object DemoClient {
    private var hotReloadEnabled = false

    fun evaluate() {
        GlobalScope.launch {
            val response = window.fetch("/api/evaluate", ...).await()
            val data = response.json().await()
            renderResults(data)
        }
    }
}
```

Benefits:
- ✅ Compile-time type safety
- ✅ IDE autocomplete and refactoring
- ✅ Kotlin coroutines (compile to JS Promises)
- ✅ Testable, maintainable code

### 🔒 HexId-Compliant Values
All stable IDs use valid hexadecimal strings compliant with `HexId`:
- Predefined test users (User Alpha, Beta, Gamma, Delta, Epsilon)
- No arbitrary strings that fail at runtime
- Example: `a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6`

### 🎯 Kotlin-Generated UI
All dropdown options are generated from Kotlin enum types:
```kotlin
AppLocale.entries.forEach { locale ->
    option {
        value = locale.name
        +locale.displayName  // Extension property
    }
}
```

No hardcoded HTML - everything is type-safe and refactorable!

### Feature Types Demonstrated
- **Boolean**: Dark Mode, Beta Features, Analytics
- **String**: Welcome Messages (localized), Theme Colors
- **Integer**: Max Items Per Page, Cache TTL
- **Double**: Discount Percentages, API Rate Limits

### Hot Reload Mode
- Auto-evaluation on parameter changes
- Debounced text input (500ms)
- Type-safe event handling in Kotlin

## Prerequisites

- JDK 17 or higher
- Gradle 8.0+ (wrapper included)

## Building

The build automatically compiles both server and client:

```bash
# Build everything (JVM + JS compilation)
./gradlew :ktor-demo:build

# Just compile the Kotlin/JS client
./gradlew :ktor-demo:demo-client:browserProductionWebpack

# Clean build
./gradlew clean :ktor-demo:build
```

The build process:
1. Compiles Kotlin/JS client to JavaScript
2. Packages with webpack
3. Copies `demo-client.js` to server resources
4. Serves at `/static/demo-client.js`

## Running

```bash
# Run the server (auto-builds client first)
./gradlew :ktor-demo:run

# Or from ktor-demo directory
cd ktor-demo
../gradlew run
```

Then visit:
- **http://localhost:8080** - Feature Evaluation Demo
- **http://localhost:8080/configstate/catalog** - ConfigState UI Catalog

The server will display:
```
Initializing Konditional Demo Client (Kotlin/JS)
```

## ConfigState UI Catalog

The **ConfigState Catalog** (`/configstate/catalog`) is a **React-based UI component showcase** built with `konditional-ui`:

### Features
- **Descriptor Catalog**: Preview all field type editors (Boolean, String, Number, Enum, Array, Object)
- **Bindings Table**: Shows JSON Pointer templates → Field Type mappings
- **Editable Fields Panel**: Live editing of configuration snapshot with validation
- **Validation Summary**: Real-time error/warning/info feedback grouped by severity
- **JSON Viewer**: Pretty-printed snapshot with copy-to-clipboard

### Key Components Demonstrated
1. **ArrayFieldEditor** - Add/remove/reorder array items with up/down buttons
2. **ObjectFieldEditor** - Collapsible MUI Accordions for object properties
3. **ValidationErrorSummary** - Error counts by severity with expandable details
4. **FieldEditor** - Polymorphic editor routing to specialized field types
5. **JsonPointer utilities** - Template expansion, nested value access

### Architecture
The catalog uses:
- **React Functional Components** from `konditional-ui`
- **MUI Material Design** components (Paper, Stack, Accordion, TextField, etc.)
- **kotlinx-serialization** for JSON snapshot handling
- **Fetch API** for async config loading

See [`../konditional-ui/README.md`](../konditional-ui/README.md) for component documentation.

## Development Workflow

For faster iteration:

```bash
# Terminal 1: Watch and auto-rebuild client
./gradlew :ktor-demo:demo-client:browserDevelopmentRun --continuous

# Terminal 2: Run server
./gradlew :ktor-demo:run
```

Or use the Gradle daemon:
```bash
./gradlew :ktor-demo:run --continuous
```

## API Endpoints

### Server Routes
- `GET /` - Feature Evaluation Demo (server-rendered HTML)
- `GET /configstate/catalog` - ConfigState UI Catalog (React SPA)
- `GET /static/*` - Static resources (compiled Kotlin/JS)

### REST API

#### Feature Evaluation API
- `POST /api/evaluate` - Evaluate features for a context
- `GET /api/snapshot` - Get Konfig JSON snapshot
- `GET /api/rules` - Get rules metadata (feature types, defaults, rule counts)

#### ConfigState API
- `GET /api/configstate` - Get configuration state with supported values, bindings, and current snapshot

### Example Request

```bash
curl -X POST http://localhost:8080/api/evaluate \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "contextType=base&locale=UNITED_STATES&platform=WEB&version=1.0.0&stableId=a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
```

Response:
```json
{
  "darkMode": false,
  "betaFeatures": false,
  "analyticsEnabled": true,
  "welcomeMessage": "Welcome to Konditional Demo!",
  "themeColor": "#F59E0B",
  "maxItemsPerPage": 25,
  "cacheTtlSeconds": 900,
  "discountPercentage": 0.0,
  "apiRateLimit": 200.0
}
```

## Using the Demo

### Feature Evaluation Demo (`/`)

1. **Open** http://localhost:8080

2. **Choose Context Type**:
   - **Base Context**: Standard features
   - **Enterprise Context**: Extended features (SSO, Advanced Analytics, etc.)

3. **Configure Parameters**:
   - **Locale**: UNITED_STATES, UNITED_STATES, CANADA, INDIA
   - **Platform**: Web, iOS, Android
   - **App Version**: Semantic versioning (e.g., "2.5.0")
   - **Stable ID**: Select from predefined test users

4. **Enterprise Options** (if Enterprise Context selected):
   - **Subscription Tier**: Free, Starter, Professional, Enterprise
   - **Organization ID**: Custom org identifier
   - **Employee Count**: Number of employees

5. **Evaluate**:
   - Click "Evaluate Features" button
   - Or toggle "Hot Reload Mode" for automatic updates

6. **View Results**:
   - **Middle Panel**: Feature evaluation results
   - **Right Panel**: Rules configuration (types, defaults, rule counts)
   - **Bottom Panel**: Complete Konfig JSON snapshot

### ConfigState UI Catalog (`/configstate/catalog`)

1. **Open** http://localhost:8080/configstate/catalog

2. **View Sections**:
   - **Top**: **Descriptor Catalog** - Preview all field type editors with live interaction
   - **Middle**: **Bindings Table** - JSON Pointer templates mapped to field types
   - **Bottom**: **Editable Fields Panel** - Live configuration editing with validation

3. **Interact with Field Editors**:
   - **Booleans**: Click checkboxes/toggles
   - **Strings**: Type in text fields
   - **Numbers**: Use number inputs with constraints
   - **Enums**: Select from dropdowns
   - **Arrays**: Add/remove items, reorder with ↑↓ buttons
   - **Objects**: Expand accordions, edit nested properties

4. **See Validation in Action**:
   - **Real-time errors**: Red borders on invalid fields
   - **Error summary**: Top panel shows counts by severity (ERROR/WARNING/INFO)
   - **Inline feedback**: Helper text under each field

5. **View Live JSON**:
   - Bottom panel shows pretty-printed snapshot
   - Click copy button to copy JSON to clipboard
   - Updates in real-time as you edit fields

## Technical Highlights

### Type-Safe JavaScript
All client logic is written in Kotlin and compiled to JavaScript:

```kotlin
// Type-safe DOM access
private fun getSelectElement(id: String): HTMLSelectElement =
    document.getElementById(id) as HTMLSelectElement

// Kotlin coroutines → JS Promises
GlobalScope.launch {
    val response = window.fetch("/api/evaluate").await()
    val data = response.json().await()
    renderResults(data)
}

// Type-safe enums
enum class FeatureType(val cssClass: String) {
    BOOLEAN("boolean"),
    STRING("string"),
    NUMBER("number")
}
```

### Server-Side Type Safety
HTML generation uses kotlinx-html-jvm:

```kotlin
// Type-safe HTML DSL
div("panel") {
    h2 { +"📊 Feature Evaluation Results" }
    div {
        id = "results"
        div("loading") { +"Click 'Evaluate Features' to see results" }
    }
}

// Enum-driven dropdowns
Platform.entries.forEach { platform ->
    option {
        value = platform.name
        if (platform == Platform.WEB) selected = true
        +platform.displayName
    }
}
```

### Shared Type Safety
Both client and server use the same Kotlin types (through JSON):
- Feature keys
- Value types (Boolean, String, Int, Double)
- Context structure

## Dependencies

### Server (JVM)
```kotlin
// Ktor server
implementation("io.ktor:ktor-server-core-jvm:3.0.1")
implementation("io.ktor:ktor-server-netty-jvm:3.0.1")
implementation("io.ktor:ktor-server-html-builder-jvm:3.0.1")

// kotlinx.html for server-side rendering
implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")

// JSON serialization
implementation("com.squareup.moshi:moshi:1.15.0")
```

### Client (JS)
```kotlin
// Reusable UI components from konditional-ui module
implementation(project(":konditional-ui"))

// kotlinx libraries for Kotlin/JS
implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.11.0")
implementation("org.jetbrains.kotlinx:kotlinx-browser:0.2")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.10.1")

// React + MUI (transitively from konditional-ui, but explicit for clarity)
implementation(kotlinWrappers.react)
implementation(kotlinWrappers.reactDom)
implementation(kotlinWrappers.mui.material)
```

## Project Structure

### Server Code (JVM)

**Application.kt**:
- Ktor server setup (Netty on port 8080)
- Static resource serving
- REST API endpoints
- HTML rendering with kotlinx-html
- Extension properties for display names

**DemoFeatures.kt**:
- 9 demo features using `FeatureContainer` delegation
- Mix of Boolean, String, Int, Double types
- Rules with targeting (platforms, locales, versions, rollouts)

**DemoContexts.kt**:
- `DemoContext`: Base context (locale, platform, version, stableId)
- `EnterpriseContext`: Extended with subscriptionTier, organizationId, employeeCount
- `SubscriptionTier`: Enum (FREE, STARTER, PROFESSIONAL, ENTERPRISE)

### Client Code (JS)

**DemoClient.kt** (Kotlin/JS - Feature Evaluation):
- Main client application object
- Event listeners (type-safe)
- Async API calls (coroutines → Promises)
- DOM manipulation (kotlinx-browser)
- Feature rendering logic
- External JS API declarations (URLSearchParams, FormData, JSON)

**ConfigStateCatalogApp.kt** (Kotlin/JS - React):
- React SPA for UI component catalog
- Uses `konditional-ui` components (FieldEditor, ArrayFieldEditor, ObjectFieldEditor, etc.)
- Live configuration editing with validation
- MUI Material Design UI

**ConfigStateCatalogClient.kt**:
- API client for fetching configuration state
- Fetches `/api/configstate` endpoint
- Returns `ConfigurationStateResponse` with supported values, bindings, snapshot

**ConfigStateApi.kt**:
- Shared API types for ConfigState endpoints
- DTO definitions for configuration state responses

## Example Feature Configuration

### Dark Mode with Platform-Specific Rollouts

```kotlin
val DARK_MODE by boolean<Context>(false) {
    rule {
        platforms(Platform.IOS, Platform.ANDROID)
        rollout { 50.0 }
    } returns true
    rule {
        platforms(Platform.WEB)
        rollout { 75.0 }
    } returns true
}
```

### Localized Welcome Messages

```kotlin
val WELCOME_MESSAGE by string<Context>("Hello!") {
    default("Welcome!")
    rule {
        locales(AppLocale.UNITED_STATES, AppLocale.CANADA)
    } returns "Welcome to Konditional Demo!"
    rule {
        locales(AppLocale.UNITED_STATES)
    } returns "¡Bienvenido a Konditional Demo!"
}
```

### Enterprise Feature with Custom Logic

```kotlin
val SSO_ENABLED by boolean<EnterpriseContext>(true) {
    rule {
        extension {
            factory { ctx ->
                ctx.subscriptionTier == SubscriptionTier.ENTERPRISE ||
                ctx.subscriptionTier == SubscriptionTier.PROFESSIONAL
            }
        }
    } returns true
}
```

## Troubleshooting

### Client JavaScript not loading
**Symptom**: Page loads but no interactivity, console errors

**Solution**:
```bash
# Verify compiled JS exists
ls ktor-demo/build/resources/main/static/demo-client.js

# Rebuild client
./gradlew clean :ktor-demo:demo-client:browserProductionWebpack

# Check browser console for: "Initializing Konditional Demo Client (Kotlin/JS)"
```

### Build errors
**Symptom**: Gradle build fails

**Solution**:
```bash
# Ensure Java 17+
java -version

# Clean and rebuild
./gradlew clean build

# Check Kotlin version (should be 2.2.0)
./gradlew :ktor-demo:demo-client:dependencies
```

### Client changes not reflected
**Symptom**: Modified DemoClient.kt but no changes in browser

**Solution**:
```bash
# Clear webpack cache
./gradlew clean

# Force rebuild
./gradlew :ktor-demo:demo-client:browserProductionWebpack --rerun-tasks

# Hard refresh browser (Ctrl+Shift+R or Cmd+Shift+R)
```

### Port 8080 in use
**Symptom**: "Address already in use"

**Solution**:
```bash
# Find process using port 8080
lsof -i :8080  # Mac/Linux
netstat -ano | findstr :8080  # Windows

# Kill process or change port in Application.kt:
embeddedServer(Netty, port = 8081, ...)
```

## Why Kotlin/JS?

Traditional web apps split client (JavaScript) and server (Kotlin/JVM) with:
- ❌ Two different languages
- ❌ Duplicated logic
- ❌ No shared types
- ❌ Error-prone string templates

Kotlin/JS enables:
- ✅ Single language (Kotlin) everywhere
- ✅ Shared types and logic
- ✅ Compile-time safety
- ✅ Modern IDE support

Result: **Type-safe full-stack Kotlin** from database to browser!

## Learn More

- [Konditional Documentation](../../docs/)
- [Konditional GitHub](https://github.com/amichne/konditional)
- [Kotlin/JS Documentation](https://kotlinlang.org/docs/js-overview.html)
- [kotlinx.html](https://github.com/Kotlin/kotlinx.html)
- [kotlinx-browser](https://github.com/Kotlin/kotlinx-browser)
- [Ktor Documentation](https://ktor.io/)

## License

MIT License - see [LICENSE](../LICENSE) file
