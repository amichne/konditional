package io.amichne.konditional.core.context

enum class Market(
    val region: String,
    val locales: Set<AppLocale>
) {
    US("US", setOf(AppLocale.EN_US, AppLocale.ES_US)),
    CA("CA", setOf(AppLocale.EN_CA)),
    IN("IN", setOf(AppLocale.HI_IN));
}
