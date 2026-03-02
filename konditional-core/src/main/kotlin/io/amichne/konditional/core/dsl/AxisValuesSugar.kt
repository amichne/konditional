package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue

/**
 * Legacy explicit axis setter for one or more values.
 *
 * Use `variant { axis { include(...) } }` instead.
 */
@Deprecated(
    message = "Use variant { axis { include(...) } } to express axis values.",
    replaceWith =
        ReplaceWith(
            "variant { axis { include(values.first(), *values.drop(1).toTypedArray()) } }",
        ),
    level = DeprecationLevel.ERROR,
)
fun <T> AxisValuesScope.axis(
    axis: Axis<T>,
    vararg values: T,
) where T : AxisValue<T>, T : Enum<T> {
    if (values.isEmpty()) return

    variant {
        axis {
            include(values[0])
            for (idx in 1 until values.size) {
                include(values[idx])
            }
        }
    }
}
