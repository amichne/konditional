file=konditional-core/src/main/kotlin/io/amichne/konditional/core/ops/MetricsCollector.kt
package=io.amichne.konditional.core.ops
type=io.amichne.konditional.core.ops.MetricsCollector|kind=interface|decl=interface MetricsCollector
type=io.amichne.konditional.core.ops.NoOp|kind=object|decl=data object NoOp : MetricsCollector
methods:
- fun recordEvaluation(event: Metrics.Evaluation) {}
- fun recordConfigLoad(event: Metrics.ConfigLoadMetric) {}
- fun recordConfigRollback(event: Metrics.ConfigRollbackMetric) {}
