# Runtime-Configurable Segments via Axes

Use axes for segment targeting you want to update via JSON (without redeploying predicates).

```kotlin
enum class Segment(override val id: String) : AxisValue<Segment> {
    CONSUMER("consumer"),
    SMB("smb"),
    ENTERPRISE("enterprise"),
}

object Axes {
    object SegmentAxis : Axis<Segment>("segment", Segment::class)
}

object SegmentFlags : Namespace("segment") {
    @Suppress("UnusedPrivateProperty")
    private val segmentAxis = Axes.SegmentAxis

    val premiumUi by boolean<Context>(default = false) {
        rule(true) { axis(Segment.ENTERPRISE) }
    }
}

fun isPremiumUiEnabled(): Boolean {
    val segmentContext =
        object : Context {
            override val locale = AppLocale.UNITED_STATES
            override val platform = Platform.IOS
            override val appVersion = Version.of(2, 1, 0)
            override val stableId = StableId.of("user-123")
            override val axisValues = axisValues { set(Axes.SegmentAxis, Segment.ENTERPRISE) }
        }

    return SegmentFlags.premiumUi.evaluate(segmentContext)
}
```

- **Guarantee**: Segment targeting is type-safe and serializable.
- **Mechanism**: Axis IDs are stored in JSON; `axis(...)` evaluates against `Context.axisValues`.
- **Boundary**: Axis IDs must remain stable across builds and obfuscation.

---
