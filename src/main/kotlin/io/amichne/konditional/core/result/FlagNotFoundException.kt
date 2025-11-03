package io.amichne.konditional.core.result

/**
 * Exception thrown when a flag is not found in the registry.
 */
class FlagNotFoundException(val key: String) : NoSuchElementException("Flag not found: $key")
