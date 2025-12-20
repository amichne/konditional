package io.amichne.konditional.context

import io.amichne.konditional.values.PlatformTagId

/**
 * Platform
 *
 * @constructor Create empty Platform
 */
enum class Platform : PlatformTag {
    IOS,
    ANDROID,
    WEB;

    override val id: PlatformTagId = PlatformTagId.of(name)
}
