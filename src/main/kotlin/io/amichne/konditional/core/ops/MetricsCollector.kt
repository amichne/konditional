package io.amichne.konditional.core.ops

interface MetricsCollector {
    fun recordEvaluation(event: EvaluationMetric) {}
    fun recordConfigLoad(event: ConfigLoadMetric) {}
    fun recordConfigRollback(event: ConfigRollbackMetric) {}

    data object NoOp : MetricsCollector
}
