# Konditional Ktor Demo

An interactive web application demonstrating the Konditional feature flags library with a full-featured UI.

## Features

🎨 **Interactive Web UI** - Built with Ktor and kotlinx.html
- Visual representation of enabled/disabled features
- Real-time feature evaluation
- JSON configuration viewer

🔧 **Dynamic Context Parameters** - Modify evaluation context on the fly:
- Locale (EN_US, FR_FR, DE_DE, ES_ES, JA_JP, EN_GB)
- Platform (Web, iOS, Android, Desktop)
- App Version (semantic versioning)
- Stable ID (user identifier for bucketing)

📊 **Multiple Context Types**:
- **Base Context**: Standard feature flags with locale, platform, version, and user ID
- **Enterprise Context**: Extended context with subscription tier, organization ID, and employee count

🎯 **Rich Feature Types Demonstrated**:
- **Boolean Features**: Dark Mode, Beta Features, Analytics
- **String Features**: Welcome Messages (localized), Theme Colors
- **Integer Features**: Max Items Per Page, Cache TTL
- **Double Features**: Discount Percentages, API Rate Limits

🔥 **Hot Reload Mode**:
- Toggle to enable automatic re-evaluation on parameter changes
- Disables manual evaluate button when enabled
- Debounced updates for text inputs (500ms)

## Quick Start

### Prerequisites

- JDK 17 or higher
- Gradle 8.0+

### Running the Demo

```bash
# From the project root
gradle :ktor-demo:run

# Or from the ktor-demo directory
cd ktor-demo
gradle run
```

The server will start on `http://localhost:8080`

### Using the Demo

1. **Open your browser** to `http://localhost:8080`

2. **Select Context Type**:
   - Choose "Base Context" for standard features
   - Choose "Enterprise Context" to see extended features (SSO, Advanced Analytics, etc.)

3. **Modify Context Parameters**:
   - Change locale to see localized welcome messages
   - Switch platforms to see platform-specific rules
   - Adjust app version to trigger version-based rollouts
   - Change user ID to see different bucketing results (due to SHA-256 hashing)

4. **For Enterprise Context**:
   - Select subscription tier (Free, Starter, Professional, Enterprise)
   - Set organization ID
   - Adjust employee count to see enterprise feature targeting

5. **Evaluation Modes**:
   - **Manual**: Click "Evaluate Features" button to see results
   - **Hot Reload**: Toggle "Hot Reload Mode" for automatic updates on every change

6. **View JSON Configuration**:
   - Scroll to bottom to see the complete Konfig JSON snapshot
   - Updates automatically when page loads

## Example Configurations

### Dark Mode with Rollout

```kotlin
DemoFeatures.DARK_MODE with {
    default(false)
    rule {
        platforms(Platform.IOS, Platform.ANDROID)
        rollout = Rollout.of(50.0)
    } implies true
    rule {
        platforms(Platform.WEB)
        rollout = Rollout.of(75.0)
    } implies true
}
```

- **iOS/Android**: 50% rollout
- **Web**: 75% rollout
- **Desktop**: Default (false)

### Localized Welcome Messages

```kotlin
DemoFeatures.WELCOME_MESSAGE with {
    default("Welcome!")
    rule { locales(AppLocale.EN_US, AppLocale.EN_GB) } implies "Welcome to Konditional Demo!"
    rule { locales(AppLocale.FR_FR) } implies "Bienvenue dans Konditional Demo!"
    rule { locales(AppLocale.DE_DE) } implies "Willkommen bei Konditional Demo!"
    rule { locales(AppLocale.ES_ES) } implies "¡Bienvenido a Konditional Demo!"
}
```

### Enterprise Features with Custom Rules

```kotlin
EnterpriseFeatures.ADVANCED_ANALYTICS with {
    default(false)
    rule {
        custom<EnterpriseContext> { ctx ->
            ctx.subscriptionTier == SubscriptionTier.ENTERPRISE &&
                ctx.employeeCount > 100
        }
    } implies true
    rule {
        custom<EnterpriseContext> { ctx ->
            ctx.subscriptionTier == SubscriptionTier.PROFESSIONAL
        }
        rollout = Rollout.of(50.0)
    } implies true
}
```

- **Enterprise tier + 100+ employees**: Always enabled
- **Professional tier**: 50% rollout

## Architecture

### Project Structure

```
ktor-demo/
├── src/main/kotlin/io/amichne/konditional/demo/
│   ├── Application.kt          # Ktor server and routing
│   ├── DemoContexts.kt         # Context type definitions
│   ├── DemoFeatures.kt         # Feature flag definitions
│   └── DemoConfiguration.kt    # Feature flag rules and configs
├── build.gradle.kts            # Build configuration
└── README.md                   # This file
```

### Key Components

**Application.kt**:
- Ktor server setup (Netty engine on port 8080)
- Routes:
  - `GET /` - Main UI page
  - `POST /api/evaluate` - Evaluate features with context
  - `GET /api/snapshot` - Get Konfig JSON snapshot
- HTML rendering with kotlinx.html
- JavaScript for interactivity

**DemoContexts.kt**:
- `DemoContext`: Base context with required fields
- `EnterpriseContext`: Extended context with business fields
- `SubscriptionTier`: Enum for subscription levels

**DemoFeatures.kt**:
- 9 demo features across all value types (Boolean, String, Int, Double)
- 4 enterprise-specific features
- Type-safe feature references

**DemoConfiguration.kt**:
- `initializeDemoConfig()`: Configures base features
- `initializeEnterpriseConfig()`: Configures enterprise features
- Rich examples of rules, rollouts, and targeting

## API Endpoints

### POST /api/evaluate

Evaluates all features with the provided context.

**Request (application/x-www-form-urlencoded)**:
```
contextType=base
locale=EN_US
platform=WEB
version=1.0.0
stableId=user-001
```

**Enterprise Context Additional Parameters**:
```
subscriptionTier=ENTERPRISE
organizationId=org-001
employeeCount=150
```

**Response (application/json)**:
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

### GET /api/snapshot

Returns the complete Konfig JSON snapshot.

**Response (application/json)**:
```json
{
  "dark_mode": {
    "default": false,
    "values": [
      {
        "value": true,
        "rollout": 50,
        "platforms": ["IOS", "ANDROID"]
      }
    ]
  },
  ...
}
```

## Technical Highlights

### Type Safety

All features are type-safe at compile time:

```kotlin
// ✅ Compiles - correct type
val darkMode: Boolean = Features.DARK_MODE.evaluate(context)

// ❌ Won't compile - type mismatch
val darkMode: String = Features.DARK_MODE.evaluate(context)
```

### Deterministic Bucketing

Rollout bucketing is deterministic and uses SHA-256 hashing:

```kotlin
rollout = Rollout.of(50.0)  // 50% of users
```

- Same user always gets the same experience for a flag
- Different flags can bucket the same user differently
- Hash: `SHA-256(salt + flag_key + stable_id) % 10000`

### Context Polymorphism

Enterprise context extends base context:

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier,
    val organizationId: String,
    val employeeCount: Int,
) : Context
```

Enterprise features can only be evaluated with `EnterpriseContext`, enforced at compile time.

### Hot Reload Implementation

```javascript
// Automatic evaluation on change
form.querySelectorAll('input, select').forEach(input => {
    input.addEventListener('change', () => {
        if (hotReloadEnabled) evaluate();
    });
    input.addEventListener('input', () => {
        if (hotReloadEnabled && input.type === 'text') {
            clearTimeout(input.timeout);
            input.timeout = setTimeout(() => evaluate(), 500);
        }
    });
});
```

- Immediate evaluation on dropdown/checkbox changes
- Debounced (500ms) evaluation on text input changes
- Prevents excessive API calls while typing

## Customization

### Adding New Features

1. **Define the feature** in `DemoFeatures.kt`:
```kotlin
enum class DemoFeatures(override val key: String) : Feature<*, *, Context, FeatureModule.Core> {
    NEW_FEATURE("new_feature");
    // ...
}
```

2. **Configure the feature** in `DemoConfiguration.kt`:
```kotlin
DemoFeatures.NEW_FEATURE with {
    default(someValue)
    rule {
        // targeting conditions
    } implies someOtherValue
}
```

3. **Update evaluation** in `Application.kt`:
```kotlin
results["newFeature"] = Features.NEW_FEATURE.evaluate(context)
```

4. **Update UI rendering** in JavaScript:
```javascript
html += renderFeature('New Feature', data.newFeature, 'boolean');
```

### Modifying Rules

Edit `DemoConfiguration.kt` to change rollout percentages, targeting conditions, or default values.

Server automatically picks up changes after restart.

## Troubleshooting

**Server won't start**:
- Ensure port 8080 is available
- Check JDK version (must be 17+)

**Features not evaluating**:
- Check browser console for errors
- Verify context parameters are valid
- Ensure feature configuration is initialized

**JSON snapshot not loading**:
- Check server logs for serialization errors
- Ensure all features have valid configurations

**Hot reload not working**:
- Verify toggle is enabled (switch should be blue)
- Check that evaluate button is disabled
- Open browser console to see API calls

## Learn More

- [Konditional Documentation](../../docs/)
- [Konditional GitHub](https://github.com/amichne/konditional)
- [Ktor Documentation](https://ktor.io/)
- [kotlinx.html](https://github.com/Kotlin/kotlinx.html)

## License

MIT License - see [LICENSE](../LICENSE) file
