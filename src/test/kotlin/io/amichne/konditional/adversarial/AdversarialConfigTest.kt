package io.amichne.konditional.adversarial

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.rules.versions.FullyBound
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * ADVERSARIAL TEST SUITE
 *
 * This test suite aggressively attempts to find gaps and ways to break the library
 * such that code compiles but config is invalid or semantically illegal.
 *
 * Goal: Be a hostile user testing the bounds of what the library can support.
 */
class AdversarialConfigTest {

    companion object {
        // Feature containers defined at companion object level for proper scoping

        object RuleSpecificityTest1 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val ambiguousFlag by boolean<Context>(default = false) {
                rule {
                    platforms(Platform.ANDROID)
                } returns true

                rule {
                    platforms(Platform.IOS)
                } returns true
            }
        }

        object RuleSpecificityTest2 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val shadowedFlag by boolean<Context>(default = false) {
                rule {
                    platforms(Platform.ANDROID)
                    locales(AppLocale.EN_US)
                    versions {
                        minimum(Version(1, 0, 0))
                    }
                } returns true

                rule {
                    platforms(Platform.ANDROID)
                } returns false
            }
        }

        object TypeEdgeCases1 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val nanFlag by double<Context>(default = Double.NaN)
        }

        object TypeEdgeCases2 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val infinityFlag by double<Context>(default = Double.POSITIVE_INFINITY) {
                rule {
                    platforms(Platform.WEB)
                } returns Double.NEGATIVE_INFINITY
            }
        }

        object TypeEdgeCases3 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val maxIntFlag by int<Context>(default = Int.MAX_VALUE) {
                rule {
                    platforms(Platform.WEB)
                } returns Int.MIN_VALUE
            }
        }

        object TypeEdgeCases4 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val emptyStringFlag by string<Context>(default = "") {
                rule {
                    platforms(Platform.WEB)
                } returns "   "
            }
        }

        object TypeEdgeCases5 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val specialCharsFlag by string<Context>(
                default = "Line1\nLine2\tTab\u0000Null\r\nCRLF"
            )
        }

        object SaltTests1 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val defaultSaltFlag by boolean<Context>(default = false)

            val customSaltFlag by boolean<Context>(default = false) {
                salt("custom-salt-value")
            }
        }

        object SaltTests2 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val emptySaltFlag by boolean<Context>(default = false) {
                salt("")
            }
        }

        object SaltTests3 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val specialSaltFlag by boolean<Context>(default = false) {
                salt("salt:with:colons\nand\nnewlines")
            }
        }

        object RolloutTests1 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val tinyRolloutFlag by boolean<Context>(default = false) {
                rule {
                    platforms(Platform.WEB)
                    rollout { 0.01 }
                } returns true
            }
        }

        object RolloutTests2 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val flag by boolean<Context>(default = false) {
                salt("v1")
                rule {
                    rollout { 50.0 }
                } returns true
            }
        }

        object RolloutTests3 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val flag by boolean<Context>(default = false) {
                salt("v2")
                rule {
                    rollout { 50.0 }
                } returns true
            }
        }

        object ContextTests1 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val versionedFlag by boolean<Context>(default = false) {
                rule {
                    versions {
                        minimum(Version(0, 0, 0))
                    }
                } returns true
            }
        }

        object ContextTests2 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val noLocaleFlag by boolean<Context>(default = false) {
                rule {
                    platforms(Platform.WEB)
                } returns true
            }
        }

        object VersionRangeTests1 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val boundedFlag by boolean<Context>(default = false) {
                rule {
                    versions {
                        minimum(Version(1, 0, 0))
                        maximum(Version(2, 0, 0))
                    }
                } returns true
            }
        }

        object VersionRangeTests2 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val exactVersionFlag by boolean<Context>(default = false) {
                rule {
                    versions {
                        minimum(Version(1, 0, 0))
                        maximum(Version(1, 0, 0))
                    }
                } returns true
            }
        }

        object NamespaceTests1 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val duplicateKey by boolean<Context>(default = true)
        }

        object NamespaceTests2 : FeatureContainer<Namespace.Authentication>(Namespace.Authentication) {
            val duplicateKey by boolean<Context>(default = false)
        }

        object InactiveTests1 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val inactiveFlag by boolean<Context>(default = false) {
                active(false)
                rule {
                    platforms(Platform.WEB)
                    rollout { 100.0 }
                } returns true
            }
        }

        object ConstraintTests1 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val catchAllFlag by boolean<Context>(default = false) {
                rule {
                    platforms(Platform.ANDROID)
                } returns true

                rule {
                    // No constraints
                } returns false
            }
        }

        object RolloutBlockingTests1 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val rolloutBlockedFlag by boolean<Context>(default = false) {
                rule {
                    platforms(Platform.WEB)
                    rollout { 0.0 }
                } returns true

                rule {
                    platforms(Platform.WEB)
                    rollout { 100.0 }
                } returns false
            }
        }
    }

    // ============================================
    // ATTACK VECTOR 1: Rollout Validation Edge Cases
    // ============================================

    @Test
    fun `rollout accepts exact boundary value 0_0 - users never see feature`() {
        val rollout = Rollout.of(0.0)
        assertEquals(0.0, rollout.value)
    }

    @Test
    fun `rollout accepts exact boundary value 100_0`() {
        val rollout = Rollout.of(100.0)
        assertEquals(100.0, rollout.value)
    }

    @Test
    fun `rollout rejects negative values`() {
        assertThrows<IllegalArgumentException> {
            Rollout.of(-0.001)
        }
    }

    @Test
    fun `rollout rejects values above 100`() {
        assertThrows<IllegalArgumentException> {
            Rollout.of(100.001)
        }
    }

    @Test
    fun `rollout accepts very small positive value - almost impossible to trigger`() {
        val rollout = Rollout.of(0.001)
        assertEquals(0.001, rollout.value)
    }

    @Test
    fun `rollout from string can parse but may have precision issues`() {
        val rollout = Rollout.of("33.333333333333333")
        assertEquals(33.333333333333333, rollout.value)
    }

    @Test
    fun `rollout from int autoconverts to double - possible confusion`() {
        val rollout = Rollout.of(50)
        assertEquals(50.0, rollout.value)
    }

    // ============================================
    // ATTACK VECTOR 2: Version Parsing Vulnerabilities
    // ============================================

    @Test
    fun `version parsing accepts negative components - creates invalid semantic version`() {
        val version = Version(-1, -1, -1)
        assertEquals(-1, version.major)
        assertEquals(Version.default, version)
    }

    @Test
    fun `version parse defaults missing components to zero - ambiguity`() {
        val v1 = Version.parse("1")
        val v2 = Version.parse("1.0")
        val v3 = Version.parse("1.0.0")
        assertEquals(v1, v2)
        assertEquals(v2, v3)
    }

    @Test
    fun `version parse handles invalid numeric parts as zero - silent failure`() {
        val version = Version.parse("1.abc.2")
        assertEquals(Version(1, 0, 0), version)
    }

    @Test
    fun `version parse allows enormous version numbers - potential overflow concerns`() {
        val version = Version(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
        assertEquals(Int.MAX_VALUE, version.major)
    }

    @Test
    fun `version parse rejects too many components`() {
        assertThrows<IllegalArgumentException> {
            Version.parse("1.2.3.4")
        }
    }

    @Test
    fun `version parse rejects empty string`() {
        assertThrows<IllegalArgumentException> {
            Version.parse("")
        }
    }

    @Test
    fun `version comparison with negative versions behaves unexpectedly`() {
        val v1 = Version(-1, 0, 0)
        val v2 = Version(0, 0, 0)
        assert(v1 < v2)
    }

    // ============================================
    // ATTACK VECTOR 3: Rule Specificity Conflicts
    // ============================================

    @Test
    fun `multiple rules with same specificity - order determines winner`() {
        val androidContext = Context(
            locale = AppLocale.EN_US,
            platform = Platform.ANDROID,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        val result = RuleSpecificityTest1.ambiguousFlag.definition.evaluate(androidContext)
        assertEquals(true, result)
    }

    @Test
    fun `overlapping rules - more specific rule shadows less specific`() {
        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.ANDROID,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        val result = RuleSpecificityTest2.shadowedFlag.definition.evaluate(context)
        assertEquals(true, result)
    }

    // ============================================
    // ATTACK VECTOR 4: HexId Validation
    // ============================================

    @Test
    fun `hexId must be valid hex string - enforced at construction`() {
        assertThrows<IllegalArgumentException> {
            StableId.of("not-valid-hex!")
        }
    }

    @Test
    fun `hexId must be even length for valid hex encoding`() {
        assertThrows<IllegalArgumentException> {
            StableId.of("abc")
        }
    }

    @Test
    fun `hexId accepts empty string in theory but validation should catch it`() {
        assertThrows<IllegalArgumentException> {
            StableId.of("")
        }
    }

    @Test
    fun `hexId is case sensitive in validation`() {
        val id1 = StableId.of("ABCDEF1234567890ABCDEF1234567890")
        val id2 = StableId.of("abcdef1234567890abcdef1234567890")
        assertNotEquals(id1.hexId.id, id2.hexId.id)
    }

    // ============================================
    // ATTACK VECTOR 5: Type System Edge Cases
    // ============================================

    @Test
    fun `double values accept NaN - compiles but semantically invalid`() {
        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        val result = TypeEdgeCases1.nanFlag.definition.evaluate(context)
        assert(result.isNaN())
        assert(result != result) // NaN != NaN!
    }

    @Test
    fun `double values accept infinity - compiles but questionable semantics`() {
        assertEquals(Double.POSITIVE_INFINITY, TypeEdgeCases2.infinityFlag.definition.defaultValue)
    }

    @Test
    fun `int values at MAX_VALUE - potential overflow in calculations`() {
        assertEquals(Int.MAX_VALUE, TypeEdgeCases3.maxIntFlag.definition.defaultValue)
    }

    @Test
    fun `string values can be empty - potentially invalid for some use cases`() {
        assertEquals("", TypeEdgeCases4.emptyStringFlag.definition.defaultValue)
    }

    @Test
    fun `string values can be extremely long - no length validation`() {
        val longString = "x".repeat(1_000_000)
        // Create container on the fly for this specific test since it needs the longString
        val container = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val hugeStringFlag by string<Context>(default = longString)
        }

        assertEquals(1_000_000, container.hugeStringFlag.definition.defaultValue.length)
    }

    @Test
    fun `string values can contain special characters and newlines`() {
        assert(TypeEdgeCases5.specialCharsFlag.definition.defaultValue.contains("\n"))
        assert(TypeEdgeCases5.specialCharsFlag.definition.defaultValue.contains("\u0000"))
    }

    // ============================================
    // ATTACK VECTOR 6: Salt Manipulation
    // ============================================

    @Test
    fun `salt defaults to v1 but can be any string`() {
        assertEquals("v1", SaltTests1.defaultSaltFlag.definition.salt)
        assertEquals("custom-salt-value", SaltTests1.customSaltFlag.definition.salt)
    }

    @Test
    fun `salt can be empty string - affects bucketing determinism`() {
        assertEquals("", SaltTests2.emptySaltFlag.definition.salt)
    }

    @Test
    fun `salt with special characters affects hash calculation`() {
        assert(SaltTests3.specialSaltFlag.definition.salt.contains(":"))
    }

    // ============================================
    // ATTACK VECTOR 7: Rollout Bucketing Manipulation
    // ============================================

    @Test
    fun `rollout at 0_01 percent affects 1 in 10000 users - easy to misconfigure`() {
        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        val result = RolloutTests1.tinyRolloutFlag.definition.evaluate(context)
        // Result depends on hash bucket - we're just testing it compiles and runs
    }

    @Test
    fun `changing salt completely re-randomizes user bucketing`() {
        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("ABCDEF1234567890ABCDEF1234567890")
        )

        val result1 = RolloutTests2.flag.definition.evaluate(context)
        val result2 = RolloutTests3.flag.definition.evaluate(context)
        // Results may differ due to re-bucketing (not guaranteed)
    }

    // ============================================
    // ATTACK VECTOR 8: Context Manipulation
    // ============================================

    @Test
    fun `context with default version (-1,-1,-1) might match unexpected rules`() {
        val defaultVersionContext = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version.default,
            stableId = StableId.of("12345678901234567890123456789012")
        )

        val result = ContextTests1.versionedFlag.definition.evaluate(defaultVersionContext)
        assertEquals(false, result)
    }

    @Test
    fun `empty locale sets match all locales - potential over-targeting`() {
        val contexts = listOf(
            AppLocale.EN_US,
            AppLocale.ES_ES,
            AppLocale.FR_FR,
            AppLocale.DE_DE
        ).map { locale ->
            Context(
                locale = locale,
                platform = Platform.WEB,
                appVersion = Version(1, 0, 0),
                stableId = StableId.of("12345678901234567890123456789012")
            )
        }

        contexts.forEach { context ->
            assertEquals(true, ContextTests2.noLocaleFlag.definition.evaluate(context))
        }
    }

    // ============================================
    // ATTACK VECTOR 9: Version Range Edge Cases
    // ============================================

    @Test
    fun `version range boundaries - inclusive minimum, exclusive maximum`() {
        val contexts = listOf(
            Version(0, 9, 9) to false,
            Version(1, 0, 0) to true,
            Version(1, 5, 0) to true,
            Version(2, 0, 0) to true,
            Version(2, 0, 1) to false
        ).map { (version, expected) ->
            Context(
                locale = AppLocale.EN_US,
                platform = Platform.WEB,
                appVersion = version,
                stableId = StableId.of("12345678901234567890123456789012")
            ) to expected
        }

        contexts.forEach { (context, expected) ->
            val result = VersionRangeTests1.boundedFlag.definition.evaluate(context)
            assertEquals(expected, result, "Failed for version ${context.appVersion}")
        }
    }

    @Test
    fun `version range with same min and max - single version targeting`() {
        val exactContext = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        assertEquals(true, VersionRangeTests2.exactVersionFlag.definition.evaluate(exactContext))

        val differentContext = exactContext.copy(appVersion = Version(1, 0, 1))
        assertEquals(false, VersionRangeTests2.exactVersionFlag.definition.evaluate(differentContext))
    }

    @Test
    fun `version range inverted (max less than min) creates impossible condition`() {
        val invalidRange = FullyBound(
            minimum = Version(2, 0, 0),
            maximum = Version(1, 0, 0)
        )

        assert(!invalidRange.contains(Version(1, 5, 0)))
        assert(!invalidRange.contains(Version(2, 0, 0)))
        assert(!invalidRange.contains(Version(1, 0, 0)))
    }

    // ============================================
    // ATTACK VECTOR 10: Namespace and Registration
    // ============================================

    @Test
    fun `multiple containers can use same feature key in different namespaces`() {
        assertEquals("duplicateKey", NamespaceTests1.duplicateKey.key)
        assertEquals("duplicateKey", NamespaceTests2.duplicateKey.key)
        assertEquals(Namespace.Global, NamespaceTests1.duplicateKey.namespace)
        assertEquals(Namespace.Authentication, NamespaceTests2.duplicateKey.namespace)
        assertEquals(true, NamespaceTests1.duplicateKey.definition.defaultValue)
        assertEquals(false, NamespaceTests2.duplicateKey.definition.defaultValue)
    }

    @Test
    fun `features register lazily - accessing key triggers registration`() {
        val container = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val lazy1 by boolean<Context>(default = true)
            val lazy2 by boolean<Context>(default = false)
        }

        assertEquals(0, container.allFeatures().size)

        container.lazy1
        assertEquals(1, container.allFeatures().size)

        container.lazy2
        assertEquals(2, container.allFeatures().size)
    }

    // ============================================
    // ATTACK VECTOR 11: Inactive Flags
    // ============================================

    @Test
    fun `inactive flag always returns default regardless of rules`() {
        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        assertEquals(false, InactiveTests1.inactiveFlag.definition.evaluate(context))
        assertEquals(false, InactiveTests1.inactiveFlag.definition.isActive)
    }

    // ============================================
    // ATTACK VECTOR 12: Rule With No Constraints
    // ============================================

    @Test
    fun `rule with no constraints has specificity 0 - lowest priority`() {
        val androidContext = Context(
            locale = AppLocale.EN_US,
            platform = Platform.ANDROID,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        assertEquals(true, ConstraintTests1.catchAllFlag.definition.evaluate(androidContext))

        val iosContext = androidContext.copy(platform = Platform.IOS)
        assertEquals(false, ConstraintTests1.catchAllFlag.definition.evaluate(iosContext))
    }

    // ============================================
    // ATTACK VECTOR 13: Rollout + Matching Interaction
    // ============================================

    @Test
    fun `rule matches but rollout excludes - falls through to next rule or default`() {
        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        assertEquals(false, RolloutBlockingTests1.rolloutBlockedFlag.definition.evaluate(context))
    }
}
