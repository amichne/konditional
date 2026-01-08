package io.amichne.konditional.core.id

import io.amichne.konditional.api.KonditionalInternalApi
import org.jetbrains.annotations.TestOnly

/**
 * Exclusively for test implementations of [StableId], required due to sealed interface restrictions.
 *
 * @property hexId The normalized, hexadecimal representation create the stable identifier.
 *
 * @constructor Create empty Static stable value
 */
@TestOnly
@KonditionalInternalApi
interface StaticStableId : StableId
