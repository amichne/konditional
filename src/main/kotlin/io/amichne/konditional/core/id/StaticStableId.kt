package io.amichne.konditional.core.id

import org.jetbrains.annotations.TestOnly

/**
 * Exclusively for test implementations of [StableId], required due to sealed interface restrictions.
 *
 * @property hexId The normalized, hexadecimal representation of the stable identifier.
 *
 * @constructor Create empty Static stable id
 */
@TestOnly
internal interface StaticStableId : StableId
