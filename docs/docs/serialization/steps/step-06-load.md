---
title: 'Step 6: Load into Runtime'
description: Load your snapshot into the Flags singleton for evaluation
---


## Overview

After deserializing JSON into a `Flags.Snapshot`, you need to load it into the `Flags` singleton so your application can evaluate feature flags.

::: tip
**Time estimate:** 5 minutes

**Goal:** Make flags available for evaluation throughout your application
:::

## Loading the Snapshot

The `Flags.load()` method loads a snapshot into the global state:

```kotlin
import io.amichne.konditional.core.Flags

// After deserializing
val snapshot = SnapshotSerializer.default.deserialize(json)

// Load into runtime
Flags.load(snapshot)

// Now flags are ready to use!
```

That's it! Once loaded, you can evaluate flags from anywhere in your application.

## When to Load

### Application Startup (Recommended)

Load flags as early as possible:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Register flags
        ConditionalRegistry.registerEnum<FeatureFlags>()

        // Load configuration
        loadFeatureFlags()

        // Continue initialization...
    }

    private fun loadFeatureFlags() {
        val json = loadConfigJson() // From assets, network, etc.
        val snapshot = SnapshotSerializer.default.deserialize(json)
        Flags.load(snapshot)

        logger.info("Feature flags loaded: ${snapshot.flags.size} flags")
    }
}
```

### Lazy Loading

If you need to defer loading:

```kotlin
object FlagManager {
    private var initialized = false

    fun ensureLoaded(context: Context) {
        if (initialized) return

        synchronized(this) {
            if (initialized) return

            ConditionalRegistry.registerEnum<FeatureFlags>()

            val json = context.assets.open("flags.json")
                .bufferedReader()
                .use { it.readText() }

            val snapshot = SnapshotSerializer.default.deserialize(json)
            Flags.load(snapshot)

            initialized = true
        }
    }
}

// Call before using flags
FlagManager.ensureLoaded(context)
```

## Evaluating Flags

Once loaded, evaluate flags using a `Context`:

```kotlin
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.StableId

// Create a context for the current user
val context = Context(
    locale = AppLocale.EN_US,
    platform = Platform.IOS,
    appVersion = Version.of(2, 1, 0),
    stableId = StableId.of(getUserId())
)

// Evaluate flags
with(Flags) {
    val isDarkModeEnabled = context.evaluate(FeatureFlags.DARK_MODE)
    val showNewOnboarding = context.evaluate(FeatureFlags.NEW_ONBOARDING)

    if (isDarkModeEnabled) {
        enableDarkMode()
    }

    if (showNewOnboarding) {
        showOnboardingScreen()
    }
}
```

## Reloading Flags

You can reload flags at runtime by calling `load()` again:

```kotlin
fun reloadFlags() {
    // Download new configuration
    val json = downloadLatestConfig()

    // Deserialize
    val snapshot = SnapshotSerializer.default.deserialize(json)

    // Reload (replaces current configuration)
    Flags.load(snapshot)

    logger.info("Flags reloaded successfully")

    // Notify listeners if needed
    notifyFlagChangeListeners()
}
```

::: note
Reloading flags is thread-safe. The `Flags` singleton uses atomic operations to ensure consistency.
:::

## Thread Safety

The `Flags` singleton is thread-safe and can be accessed from multiple threads:

```kotlin
// Thread 1: Loading
CoroutineScope(Dispatchers.IO).launch {
    val snapshot = loadRemoteConfiguration()
    Flags.load(snapshot)
}

// Thread 2: Evaluating (safe even during reload)
CoroutineScope(Dispatchers.Main).launch {
    with(Flags) {
        val value = context.evaluate(FeatureFlags.DARK_MODE)
        updateUI(value)
    }
}
```

## Complete Integration Example

Here's a complete example integrating loading and evaluation:

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure flags are loaded
        ensureFlagsLoaded()

        // Use flags to control UI
        setupUI()
    }

    private fun ensureFlagsLoaded() {
        if (!FlagManager.isLoaded) {
            FlagManager.load(this)
        }
    }

    private fun setupUI() {
        val context = createUserContext()

        with(Flags) {
            // Check dark mode flag
            if (context.evaluate(FeatureFlags.DARK_MODE)) {
                setTheme(R.style.DarkTheme)
            }

            // Check UI variant flags
            if (context.evaluate(FeatureFlags.COMPACT_CARDS)) {
                setContentView(R.layout.activity_main_compact)
            } else {
                setContentView(R.layout.activity_main_standard)
            }

            // Check feature availability
            if (context.evaluate(FeatureFlags.NEW_FEATURE)) {
                showNewFeatureBadge()
            }
        }
    }

    private fun createUserContext(): Context {
        return Context(
            locale = getCurrentLocale(),
            platform = Platform.ANDROID,
            appVersion = Version.of(BuildConfig.VERSION_CODE),
            stableId = StableId.of(getUserId())
        )
    }
}
```

## Observing Flag Changes

If you need to react to flag changes, implement an observer pattern:

```kotlin
object FlagObserver {
    private val listeners = mutableSetOf<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun notifyChange() {
        listeners.forEach { it() }
    }
}

// When reloading flags
fun reloadFlags() {
    val snapshot = loadNewConfiguration()
    Flags.load(snapshot)

    // Notify observers
    FlagObserver.notifyChange()
}

// In your UI
class MyFragment : Fragment() {
    private val flagChangeListener = {
        refreshUI()
    }

    override fun onResume() {
        super.onResume()
        FlagObserver.addListener(flagChangeListener)
    }

    override fun onPause() {
        super.onPause()
        FlagObserver.removeListener(flagChangeListener)
    }

    private fun refreshUI() {
        val context = createUserContext()
        with (Flags) {
            updateDarkMode(context.evaluate(FeatureFlags.DARK_MODE))
            updateCompactMode(context.evaluate(FeatureFlags.COMPACT_CARDS))
        }
    }
}
```

## Best Practices

### 1. Load Early

Load flags as early as possible to avoid race conditions:

```kotlin
// ✅ Good: Load in Application.onCreate()
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        loadFlags() // First thing
        initializeLibraries() // After flags
    }
}

// ❌ Bad: Load lazily when first needed
fun someActivity() {
    loadFlagsIfNeeded() // Too late, might miss flags
    if (isDarkModeEnabled()) { ... }
}
```

### 2. Handle Missing Flags Gracefully

Always provide defaults:

```kotlin
fun isDarkModeEnabled(context: Context): Boolean {
    return try {
        with(Flags) {
            context.evaluate(FeatureFlags.DARK_MODE)
        }
    } catch (e: IllegalStateException) {
        // Flag not found - return safe default
        logger.warn("Dark mode flag not found, defaulting to false", e)
        false
    }
}
```

### 3. Cache User Context

Don't create contexts repeatedly:

```kotlin
// ✅ Good: Create once, reuse
class UserSession {
    val flagContext by lazy {
        Context(
            locale = getUserLocale(),
            platform = Platform.ANDROID,
            appVersion = Version.of(BuildConfig.VERSION_CODE),
            stableId = StableId.of(getUserId())
        )
    }
}

// ❌ Bad: Create every time
fun checkFlag(): Boolean {
    val context = Context(...) // Recreated unnecessarily
    return with(Flags) { context.evaluate(flag) }
}
```

## What's Next?

With flags loaded and working, you should add tests to ensure everything works correctly.

<div style="display: flex; justify-content: space-between; margin-top: 2rem;">
  <a href="/serialization/steps/step-05-deserialize/" style="text-decoration: none;">
    <strong>← Previous: Step 5 - Deserialize</strong>
  </a>
  <a href="/serialization/steps/step-07-testing/" style="text-decoration: none;">
    <strong>Next: Step 7 - Testing →</strong>
  </a>
</div>
