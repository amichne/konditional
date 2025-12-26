package io.amichne.konditional.demo.net

import kotlinx.serialization.Serializable

@Serializable
data class EvaluationRequest(
    val contextType: String,
    val locale: String,
    val platform: String,
    val version: String,
    val stableId: String,
    // Enterprise fields (optional)
    val tier: String? = null,
    val orgId: String? = null,
    val employeeCount: Int? = null,
)

@Serializable
data class EvaluationResponse(
    // Base features
    val darkMode: Boolean,
    val betaFeatures: Boolean,
    val analyticsEnabled: Boolean,
    val welcomeMessage: String,
    val themeColor: String,
    val maxItemsPerPage: Int,
    val cacheTtlSeconds: Int,
    val discountPercentage: Double,
    val apiRateLimit: Double,
    // Enterprise features (nullable for base context)
    val ssoEnabled: Boolean? = null,
    val advancedAnalytics: Boolean? = null,
    val customBranding: Boolean? = null,
    val dedicatedSupport: Boolean? = null,
)
