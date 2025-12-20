package io.amichne.konditional.context

/**
 * Platform
 *
 * @constructor Create empty Platform
 */
enum class Platform : PlatformTag {
    IOS,
    ANDROID,
    WEB;

    override val id: String = name
}
