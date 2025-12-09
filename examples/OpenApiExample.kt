package io.amichne.konditional.examples

import io.amichne.konditional.core.dsl.jsonObject
import io.amichne.konditional.openapi.OpenApiGenerator
import io.amichne.konditional.openapi.SchemaRegistry
import io.amichne.konditional.openapi.buildOpenApiSpec
import java.io.File

/**
 * Complete example demonstrating OpenAPI generation from JSONSchema definitions.
 *
 * This example shows:
 * 1. Defining schemas using the JSONSchema DSL
 * 2. Registering schemas with the SchemaRegistry
 * 3. Generating OpenAPI specs using different methods
 * 4. Writing specs to files
 */
object OpenApiExample {

    // Define enums used in schemas
    enum class UserRole { ADMIN, USER, GUEST }
    enum class OrderStatus { PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED }
    enum class PaymentMethod { CREDIT_CARD, PAYPAL, BANK_TRANSFER }

    // ========== Schema Definitions ==========

    val userSchema = jsonObject {
        requiredField("id") { int() }
        requiredField("username") { string() }
        requiredField("email") { string() }
        requiredField("role") { enum<UserRole>() }
        optionalField("firstName") { string() }
        optionalField("lastName") { string() }
        optionalField("active") { boolean() }
        optionalField("createdAt") { string() }  // ISO 8601 timestamp
    }

    val addressSchema = jsonObject {
        requiredField("street") { string() }
        requiredField("city") { string() }
        requiredField("state") { string() }
        requiredField("zipCode") { string() }
        optionalField("country") { string() }
        optionalField("apartment") { string() }
    }

    val productSchema = jsonObject {
        requiredField("sku") { string() }
        requiredField("name") { string() }
        requiredField("price") { double() }
        requiredField("inStock") { boolean() }
        optionalField("description") { string() }
        optionalField("category") { string() }
        optionalField("tags") { array { string() } }
        optionalField("images") { array { string() } }
        optionalField("weight") { double() }
        optionalField("dimensions") {
            jsonObject {
                requiredField("length") { double() }
                requiredField("width") { double() }
                requiredField("height") { double() }
            }
        }
    }

    val orderItemSchema = jsonObject {
        requiredField("productSku") { string() }
        requiredField("quantity") { int() }
        requiredField("price") { double() }
        optionalField("discount") { double() }
    }

    val orderSchema = jsonObject {
        requiredField("orderId") { string() }
        requiredField("userId") { int() }
        requiredField("status") { enum<OrderStatus>() }
        requiredField("items") { array { orderItemSchema } }
        requiredField("total") { double() }
        optionalField("shippingAddress") { addressSchema }
        optionalField("billingAddress") { addressSchema }
        optionalField("paymentMethod") { enum<PaymentMethod>() }
        optionalField("notes") { string() }
        optionalField("createdAt") { string() }
        optionalField("updatedAt") { string() }
    }

    val configSchema = jsonObject {
        requiredField("enabled") { boolean() }
        requiredField("environment") { string() }
        optionalField("maxRetries") { int() }
        optionalField("timeout") { double() }
        optionalField("features") {
            jsonObject {
                optionalField("darkMode") { boolean() }
                optionalField("notifications") { boolean() }
                optionalField("analytics") { boolean() }
            }
        }
    }

    // ========== Registration ==========

    /**
     * Registers all schemas in the SchemaRegistry.
     * This should be called at application startup.
     */
    fun registerSchemas() {
        SchemaRegistry.register(
            "User",
            userSchema,
            "User account information with authentication and profile data"
        )
        SchemaRegistry.register(
            "Address",
            addressSchema,
            "Physical address for shipping or billing"
        )
        SchemaRegistry.register(
            "Product",
            productSchema,
            "Product catalog item with pricing and inventory"
        )
        SchemaRegistry.register(
            "OrderItem",
            orderItemSchema,
            "Individual item in an order"
        )
        SchemaRegistry.register(
            "Order",
            orderSchema,
            "Customer order with items, shipping, and payment information"
        )
        SchemaRegistry.register(
            "Config",
            configSchema,
            "Application configuration and feature flags"
        )
    }

    // ========== Generation Methods ==========

    /**
     * Method 1: Generate using SchemaRegistry
     */
    fun generateFromRegistry(outputFile: File) {
        println("Method 1: Generating from SchemaRegistry...")

        val generator = OpenApiGenerator(
            title = "E-Commerce API",
            version = "1.0.0",
            description = "Complete e-commerce API with users, products, and orders",
            baseUrl = "https://api.ecommerce.example.com/v1"
        )

        // Export all registered schemas
        SchemaRegistry.exportTo(generator)

        generator.writeToFile(outputFile)
    }

    /**
     * Method 2: Generate using manual registration
     */
    fun generateManually(outputFile: File) {
        println("Method 2: Generating manually...")

        val generator = OpenApiGenerator(
            title = "Product Catalog API",
            version = "2.0.0",
            description = "Product management API",
            baseUrl = "https://api.products.example.com/v2"
        )

        // Register specific schemas
        generator.registerSchema("Product", productSchema)
        generator.registerSchema("Config", configSchema)

        generator.writeToFile(outputFile)
    }

    /**
     * Method 3: Generate using DSL helper
     */
    fun generateWithDsl(): String {
        println("Method 3: Generating with DSL...")

        return buildOpenApiSpec(
            title = "User Management API",
            version = "1.0.0"
        ) {
            registerSchema("User", userSchema)
            registerSchema("Address", addressSchema)
        }
    }

    /**
     * Method 4: Custom generator with all features
     */
    fun generateCustom(outputFile: File) {
        println("Method 4: Generating custom spec...")

        val generator = OpenApiGenerator(
            title = "Konditional Complete API",
            version = "3.0.0",
            description = """
                Complete API demonstrating all schema types:
                - Primitive types (string, int, double, boolean)
                - Enums
                - Nested objects
                - Arrays
                - Complex compositions
            """.trimIndent(),
            baseUrl = "https://api.konditional.example.com/v3"
        )

        // Register all schemas
        generator.registerSchema("User", userSchema)
        generator.registerSchema("Address", addressSchema)
        generator.registerSchema("Product", productSchema)
        generator.registerSchema("OrderItem", orderItemSchema)
        generator.registerSchema("Order", orderSchema)
        generator.registerSchema("Config", configSchema)

        generator.writeToFile(outputFile)
    }

    // ========== Main Example ==========

    @JvmStatic
    fun main(args: Array<String>) {
        println("=" .repeat(70))
        println("OpenAPI Generation Example")
        println("=".repeat(70))
        println()

        // Register all schemas first
        registerSchemas()
        println("✓ Registered ${SchemaRegistry.size()} schemas")
        println()

        // Create output directory
        val outputDir = File("build/openapi-examples")
        outputDir.mkdirs()

        // Method 1: From registry
        val registryFile = File(outputDir, "from-registry.json")
        generateFromRegistry(registryFile)
        println("  → ${registryFile.name} (${registryFile.length()} bytes)")
        println()

        // Method 2: Manual
        val manualFile = File(outputDir, "manual.json")
        generateManually(manualFile)
        println("  → ${manualFile.name} (${manualFile.length()} bytes)")
        println()

        // Method 3: DSL
        val dslSpec = generateWithDsl()
        val dslFile = File(outputDir, "dsl.json")
        dslFile.writeText(dslSpec)
        println("  → ${dslFile.name} (${dslFile.length()} bytes)")
        println()

        // Method 4: Custom
        val customFile = File(outputDir, "custom.json")
        generateCustom(customFile)
        println("  → ${customFile.name} (${customFile.length()} bytes)")
        println()

        println("=".repeat(70))
        println("✓ All examples generated successfully!")
        println("=".repeat(70))
        println()
        println("Output directory: ${outputDir.absolutePath}")
        println()
        println("Next steps:")
        println("  1. View the generated specs in ${outputDir.name}/")
        println("  2. Import into Swagger UI or Postman")
        println("  3. Use OpenAPI Generator to create client SDKs")
        println("  4. Validate against OpenAPI 3.0 specification")
        println()
    }
}
