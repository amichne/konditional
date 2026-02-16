package io.amichne.konditional.server.core.surface.spi

import io.amichne.konditional.server.core.surface.dto.CodecOutcome

internal interface SurfaceCodecSpi<in RequestPayload : Any, out ResponsePayload : Any> {
    fun decodeRequest(payload: RequestPayload): CodecOutcome

    fun encodeResponse(outcome: CodecOutcome): ResponsePayload
}
