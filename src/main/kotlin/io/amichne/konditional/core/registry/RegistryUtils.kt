package io.amichne.konditional.core.registry

/**
 * Executes a block with a specific registry in scope.
 *
 * This is useful for testing or when you need to evaluate flags against a specific
 * registry instance instead of the default taxonomy registry.
 *
 * Example:
 * ```kotlin
 * val testRegistry = ModuleRegistry.create()
 * testRegistry.load(testConfig)
 *
 * withRegistry(testRegistry) {
 *     val value = MyFeature.FLAG.evaluate(context)
 *     // Evaluation uses testRegistry instead of default
 * }
 * ```
 *
 * @param registry The registry to use within the block
 * @param block The code to execute with the scoped registry
 * @return The result of the block
 */
fun <T> withRegistry(registry: ModuleRegistry = InMemoryModuleRegistry(), block: () -> T): T =
    RegistryScope.usingRegistry(registry, block)
