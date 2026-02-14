file=detekt-rules/src/main/kotlin/io/amichne/konditional/detekt/UnclosedCriteriaFirstRule.kt
package=io.amichne.konditional.detekt
imports=io.gitlab.arturbosch.detekt.api.CodeSmell,io.gitlab.arturbosch.detekt.api.Config,io.gitlab.arturbosch.detekt.api.Debt,io.gitlab.arturbosch.detekt.api.Entity,io.gitlab.arturbosch.detekt.api.Issue,io.gitlab.arturbosch.detekt.api.Rule,io.gitlab.arturbosch.detekt.api.Severity,org.jetbrains.kotlin.psi.KtBinaryExpression,org.jetbrains.kotlin.psi.KtCallExpression,org.jetbrains.kotlin.psi.KtDotQualifiedExpression,org.jetbrains.kotlin.psi.KtExpression,org.jetbrains.kotlin.psi.KtParenthesizedExpression
type=io.amichne.konditional.detekt.UnclosedCriteriaFirstRule|kind=class|decl=class UnclosedCriteriaFirstRule(config: Config) : Rule(config)
fields:
- override val issue: Issue
methods:
- override fun visitCallExpression(expression: KtCallExpression)
