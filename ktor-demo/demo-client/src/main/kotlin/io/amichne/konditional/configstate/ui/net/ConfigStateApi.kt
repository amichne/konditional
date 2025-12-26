package io.amichne.konditional.configstate.ui.net

import io.amichne.konditional.configstate.ui.model.ConfigurationStateResponseDto
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import org.w3c.fetch.RequestInit
import kotlin.js.json

object ConfigStateApi {
    private val jsonCodec: Json =
        Json {
            ignoreUnknownKeys = true
            classDiscriminator = "kind"
            isLenient = true
        }

    suspend fun fetchConfigurationState(): Result<ConfigurationStateResponseDto> =
        runCatching {
            val requestInit =
                json(
                    "method" to "GET",
                    "headers" to
                        json(
                            "Accept" to "application/json",
                        ),
                ).unsafeCast<RequestInit>()

            val response = window.fetch("/api/configstate", requestInit).await()

            if (!response.ok) {
                val body = response.text().await()
                throw IllegalStateException("Configstate request failed: ${response.status} ${response.statusText}. Body: $body")
            }

            val body = response.text().await()
            jsonCodec.decodeFromString(ConfigurationStateResponseDto.serializer(), body)
        }
}
