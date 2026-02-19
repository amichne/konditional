file=konditional-core/src/main/kotlin/io/amichne/konditional/core/ops/KonditionalLogger.kt
package=io.amichne.konditional.core.ops
type=io.amichne.konditional.core.ops.KonditionalLogger|kind=interface|decl=interface KonditionalLogger
type=io.amichne.konditional.core.ops.NoOp|kind=object|decl=data object NoOp : KonditionalLogger
methods:
- fun debug(message: () -> String) {}
- fun info(message: () -> String) {}
- fun warn(message: () -> String, throwable: Throwable? = null) {}
- fun error(message: () -> String, throwable: Throwable? = null) {}
