# Custom axes

Use an axis when rules select a product, environment, tenant, or region.

## Define the axis values

Each entry in an enum that implements `AxisValue<T>` is an axis value.

```kotlin
import io.amichne.konditional.context.axis.AxisValue

enum class Environment : AxisValue<Environment> {
    DEV,
    PROD,
}
```

You can derive the axis handle later with `Axis.of<Environment>()` when you
need typed access to the grouped values.

## Add the axis to the context

Expose the selection through the context's `axes` property.

```kotlin
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.axis.axes

data class RuntimeContext(
    val environment: Environment,
) : Context {
    override val axes = axes(environment)
}
```

## Target the axis in a rule

Use `constrain(...)` in the rule DSL to target one or more axis values.

```kotlin
object RuntimeFlags : Namespace("runtime") {
    val diagnostics by boolean<RuntimeContext>(default = false) {
        enable {
            constrain(Environment.PROD)
        }
    }
}
```

That rule only matches contexts whose environment axis includes `PROD`.
