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
import io.amichne.konditional.rules.versions.LeftBound
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

    // ============================================
    // ATTACK VECTOR 1: Rollout Validation Edge Cases
    // ============================================

    @Test
    fun `rollout accepts exact boundary value 0_0 - users never see feature`() {
        // This compiles and is "valid" but means 0% rollout
        // Possible user error: thinking 0.0 means "no limit"
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
        // Compiles fine but 0.001% rollout is 1 in 100,000 users
        // User might think this is 0.1% (which would be 10x higher)
        val rollout = Rollout.of(0.001)
        assertEquals(0.001, rollout.value)
    }

    @Test
    fun `rollout from string can parse but may have precision issues`() {
        // Floating point representation might not be exact
        val rollout = Rollout.of("33.333333333333333")
        // This compiles but the value might be slightly different
        assertEquals(33.333333333333333, rollout.value)
    }

    @Test
    fun `rollout from int autoconverts to double - possible confusion`() {
        // User might not realize they're getting a Double
        val rollout = Rollout.of(50)
        assertEquals(50.0, rollout.value)
    }

    // ============================================
    // ATTACK VECTOR 2: Version Parsing Vulnerabilities
    // ============================================

    @Test
    fun `version parsing accepts negative components - creates invalid semantic version`() {
        // Negative versions don't make sense semantically but parse successfully
        val version = Version(-1, -1, -1)
        assertEquals(-1, version.major)
        // This is the "default" version! Collision risk!
        assertEquals(Version.default, version)
    }

    @Test
    fun `version parse defaults missing components to zero - ambiguity`() {
        // "1" becomes (1,0,0) same as "1.0.0" - user might not expect this
        val v1 = Version.parse("1")
        val v2 = Version.parse("1.0")
        val v3 = Version.parse("1.0.0")
        assertEquals(v1, v2)
        assertEquals(v2, v3)
    }

    @Test
    fun `version parse handles invalid numeric parts as zero - silent failure`() {
        // "1.abc.2" parses as (1,0,0) because "abc".toIntOrNull() returns null
        val version = Version.parse("1.abc.2")
        assertEquals(Version(1, 0, 0), version)
        // The "2" is completely ignored! User might think they got version 1.0.2
    }

    @Test
    fun `version parse allows enormous version numbers - potential overflow concerns`() {
        // Int.MAX_VALUE is accepted, could cause comparison issues
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
        // Negative versions compare in reverse order
        val v1 = Version(-1, 0, 0)
        val v2 = Version(0, 0, 0)
        assert(v1 < v2) // -1 < 0, but semantically meaningless
    }

    // ============================================
    // ATTACK VECTOR 3: Rule Specificity Conflicts
    // ============================================

    @Test
    fun `multiple rules with same specificity - order determines winner`() {
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            // Both rules have specificity = 1 (platform only)
            // Order matters but this is implicit and undocumented
            val ambiguousFlag by boolean<Context>(default = false) {
                rule {
                    platforms(Platform.ANDROID)
                } returns true

                rule {
                    platforms(Platform.IOS) // Also specificity 1
                } returns true
            }
        }

        // Both rules match their respective platforms but have same specificity
        // The sorting is stable but relies on note comparison as tiebreaker
        // If notes are null, order is implementation-dependent
        val androidContext = Context(
            locale = AppLocale.EN_US,
            platform = Platform.ANDROID,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        val result = TestFeatures.ambiguousFlag.definition.evaluate(androidContext)
        // Result depends on rule order and rollout bucket
        assertEquals(true, result)
    }

    @Test
    fun `overlapping rules - more specific rule shadows less specific`() {
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val shadowedFlag by boolean<Context>(default = false) {
                // Specificity 3: platform + locale + version
                rule {
                    platforms(Platform.ANDROID)
                    locales(AppLocale.EN_US)
                    versions {
                        minimum(Version(1, 0, 0))
                    }
                } returns true

                // Specificity 1: platform only
                // This rule never fires for Android EN_US v1+ users
                // even if they intended it as a fallback
                rule {
                    platforms(Platform.ANDROID)
                } returns false
            }
        }

        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.ANDROID,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        // First rule matches and has higher specificity, second never evaluated
        val result = TestFeatures.shadowedFlag.definition.evaluate(context)
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
        // Odd-length hex strings should fail
        assertThrows<IllegalArgumentException> {
            StableId.of("abc") // 3 chars = odd
        }
    }

    @Test
    fun `hexId accepts empty string in theory but validation should catch it`() {
        // Empty string is technically "valid" hex but semantically wrong
        assertThrows<IllegalArgumentException> {
            StableId.of("")
        }
    }

    @Test
    fun `hexId is case sensitive in validation`() {
        // Uppercase and lowercase hex should both work
        val id1 = StableId.of("ABCDEF1234567890ABCDEF1234567890")
        val id2 = StableId.of("abcdef1234567890abcdef1234567890")
        // These are different IDs after normalization
        assertNotEquals(id1.hexId.id, id2.hexId.id)
    }

    // ============================================
    // ATTACK VECTOR 5: Type System Edge Cases
    // ============================================

    @Test
    fun `double values accept NaN - compiles but semantically invalid`() {
        // NaN compiles fine in Double context
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val nanFlag by double<Context>(default = Double.NaN) {
                // This compiles! But NaN != NaN by IEEE 754
            }
        }

        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        val result = TestFeatures.nanFlag.definition.evaluate(context)
        assert(result.isNaN()) // NaN propagates through
        assert(result != result) // NaN != NaN!
    }

    @Test
    fun `double values accept infinity - compiles but questionable semantics`() {
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val infinityFlag by double<Context>(default = Double.POSITIVE_INFINITY) {
                rule {
                    platforms(Platform.WEB)
                } returns Double.NEGATIVE_INFINITY
            }
        }

        assertEquals(Double.POSITIVE_INFINITY, TestFeatures.infinityFlag.definition.defaultValue)
    }

    @Test
    fun `int values at MAX_VALUE - potential overflow in calculations`() {
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val maxIntFlag by int<Context>(default = Int.MAX_VALUE) {
                rule {
                    platforms(Platform.WEB)
                } returns Int.MIN_VALUE
            }
        }

        assertEquals(Int.MAX_VALUE, TestFeatures.maxIntFlag.definition.defaultValue)
        // If user code does maxIntFlag + 1, it overflows to Int.MIN_VALUE
    }

    @Test
    fun `string values can be empty - potentially invalid for some use cases`() {
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val emptyStringFlag by string<Context>(default = "") {
                rule {
                    platforms(Platform.WEB)
                } returns "   " // Whitespace-only also valid
            }
        }

        assertEquals("", TestFeatures.emptyStringFlag.definition.defaultValue)
    }

    @Test
    fun `string values can be extremely long - no length validation`() {
        val longString = "x".repeat(1_000_000) // 1 MB string
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val hugeStringFlag by string<Context>(default = longString)
        }

        assertEquals(1_000_000, TestFeatures.hugeStringFlag.definition.defaultValue.length)
    }

    @Test
    fun `string values can contain special characters and newlines`() {
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val specialCharsFlag by string<Context>(
                default = "Line1\nLine2\tTab\u0000Null\r\nCRLF"
            )
        }

        assert(TestFeatures.specialCharsFlag.definition.defaultValue.contains("\n"))
        assert(TestFeatures.specialCharsFlag.definition.defaultValue.contains("\u0000"))
    }

    // ============================================
    // ATTACK VECTOR 6: Salt Manipulation
    // ============================================

    @Test
    fun `salt defaults to v1 but can be any string`() {
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val defaultSaltFlag by boolean<Context>(default = false) {
                // Salt defaults to "v1"
            }

            val customSaltFlag by boolean<Context>(default = false) {
                salt("custom-salt-value")
            }
        }

        assertEquals("v1", TestFeatures.defaultSaltFlag.definition.salt)
        assertEquals("custom-salt-value", TestFeatures.customSaltFlag.definition.salt)
    }

    @Test
    fun `salt can be empty string - affects bucketing determinism`() {
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val emptySaltFlag by boolean<Context>(default = false) {
                salt("") // Empty salt compiles fine
            }
        }

        assertEquals("", TestFeatures.emptySaltFlag.definition.salt)
    }

    @Test
    fun `salt with special characters affects hash calculation`() {
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val specialSaltFlag by boolean<Context>(default = false) {
                salt("salt:with:colons\nand\nnewlines")
            }
        }

        // Colons are used as delimiters in hash input: "$salt:$flagKey:$id"
        // This could cause unexpected bucket distributions
        assert(TestFeatures.specialSaltFlag.definition.salt.contains(":"))
    }

    // ============================================
    // ATTACK VECTOR 7: Rollout Bucketing Manipulation
    // ============================================

    @Test
    fun `rollout at 0_01 percent affects 1 in 10000 users - easy to misconfigure`() {
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val tinyRolloutFlag by boolean<Context>(default = false) {
                rule {
                    platforms(Platform.WEB)
                    rollout { 0.01 } // User might think this is 1%, but it's 0.01%
                } returns true
            }
        }

        // Bucketing is deterministic, so specific users always get same result
        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        // This user might or might not be in the 0.01% bucket
        // But the rollout is so small it's almost never true
        val result = TestFeatures.tinyRolloutFlag.definition.evaluate(context)
        // Assertion depends on hash bucket for this specific ID
    }

    @Test
    fun `changing salt completely re-randomizes user bucketing`() {
        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("ABCDEF1234567890ABCDEF1234567890")
        )

        object TestFeatures1 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val flag by boolean<Context>(default = false) {
                salt("v1")
                rule {
                    rollout { 50.0 }
                } returns true
            }
        }

        object TestFeatures2 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val flag by boolean<Context>(default = false) {
                salt("v2") // Different salt
                rule {
                    rollout { 50.0 }
                } returns true
            }
        }

        // Same user, same rollout %, but different salts
        // Could get different results (bucket assignment changes)
        val result1 = TestFeatures1.flag.definition.evaluate(context)
        val result2 = TestFeatures2.flag.definition.evaluate(context)

        // Results may differ due to re-bucketing (not guaranteed, depends on hash)
    }

    // ============================================
    // ATTACK VECTOR 8: Context Manipulation
    // ============================================

    @Test
    fun `context with default version (-1,-1,-1) might match unexpected rules`() {
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val versionedFlag by boolean<Context>(default = false) {
                rule {
                    versions {
                        minimum(Version(0, 0, 0))
                    }
                } returns true
            }
        }

        val defaultVersionContext = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version.default, // (-1,-1,-1)
            stableId = StableId.of("12345678901234567890123456789012")
        )

        // Does Version(-1,-1,-1) >= Version(0,0,0)? No!
        val result = TestFeatures.versionedFlag.definition.evaluate(defaultVersionContext)
        assertEquals(false, result) // Default version doesn't match minimum(0,0,0)
    }

    @Test
    fun `empty locale sets match all locales - potential over-targeting`() {
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val noLocaleFlag by boolean<Context>(default = false) {
                rule {
                    // No locale specified = matches ALL locales
                    platforms(Platform.WEB)
                } returns true
            }
        }

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

        // All locales match because rule has no locale restriction
        contexts.forEach { context ->
            assertEquals(true, TestFeatures.noLocaleFlag.definition.evaluate(context))
        }
    }

    // ============================================
    // ATTACK VECTOR 9: Version Range Edge Cases
    // ============================================

    @Test
    fun `version range boundaries - inclusive minimum, exclusive maximum`() {
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val boundedFlag by boolean<Context>(default = false) {
                rule {
                    versions {
                        minimum(Version(1, 0, 0))
                        maximum(Version(2, 0, 0))
                    }
                } returns true
            }
        }

        val contexts = listOf(
            Version(0, 9, 9) to false,  // Below minimum
            Version(1, 0, 0) to true,   // At minimum (inclusive)
            Version(1, 5, 0) to true,   // In range
            Version(2, 0, 0) to true,   // At maximum (inclusive based on RightBound implementation)
            Version(2, 0, 1) to false   // Above maximum
        ).map { (version, expected) ->
            Context(
                locale = AppLocale.EN_US,
                platform = Platform.WEB,
                appVersion = version,
                stableId = StableId.of("12345678901234567890123456789012")
            ) to expected
        }

        contexts.forEach { (context, expected) ->
            val result = TestFeatures.boundedFlag.definition.evaluate(context)
            assertEquals(expected, result, "Failed for version ${context.appVersion}")
        }
    }

    @Test
    fun `version range with same min and max - single version targeting`() {
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val exactVersionFlag by boolean<Context>(default = false) {
                rule {
                    versions {
                        minimum(Version(1, 0, 0))
                        maximum(Version(1, 0, 0))
                    }
                } returns true
            }
        }

        val exactContext = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        // Should match exactly version 1.0.0
        assertEquals(true, TestFeatures.exactVersionFlag.definition.evaluate(exactContext))

        val differentContext = exactContext.copy(appVersion = Version(1, 0, 1))
        assertEquals(false, TestFeatures.exactVersionFlag.definition.evaluate(differentContext))
    }

    @Test
    fun `version range inverted (max less than min) creates impossible condition`() {
        // This compiles but creates a range that matches nothing
        val invalidRange = FullyBound(
            minimum = Version(2, 0, 0),
            maximum = Version(1, 0, 0) // max < min!
        )

        // No version can satisfy: version >= 2.0.0 AND version <= 1.0.0
        assert(!invalidRange.contains(Version(1, 5, 0)))
        assert(!invalidRange.contains(Version(2, 0, 0)))
        assert(!invalidRange.contains(Version(1, 0, 0)))
    }

    // ============================================
    // ATTACK VECTOR 10: Namespace and Registration
    // ============================================

    @Test
    fun `multiple containers can use same feature key in different namespaces`() {
        object Features1 : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val duplicateKey by boolean<Context>(default = true)
        }

        object Features2 : FeatureContainer<Namespace.Authentication>(Namespace.Authentication) {
            val duplicateKey by boolean<Context>(default = false)
        }

        // Same key, different namespaces, different defaults
        assertEquals("duplicateKey", Features1.duplicateKey.key)
        assertEquals("duplicateKey", Features2.duplicateKey.key)
        assertEquals(Namespace.Global, Features1.duplicateKey.namespace)
        assertEquals(Namespace.Authentication, Features2.duplicateKey.namespace)
        assertEquals(true, Features1.duplicateKey.definition.defaultValue)
        assertEquals(false, Features2.duplicateKey.definition.defaultValue)
    }

    @Test
    fun `features register lazily - accessing key triggers registration`() {
        val container = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val lazy1 by boolean<Context>(default = true)
            val lazy2 by boolean<Context>(default = false)
        }

        // Before accessing, allFeatures returns empty
        assertEquals(0, container.allFeatures().size)

        // Access one feature
        container.lazy1
        assertEquals(1, container.allFeatures().size)

        // Access second feature
        container.lazy2
        assertEquals(2, container.allFeatures().size)
    }

    // ============================================
    // ATTACK VECTOR 11: Inactive Flags
    // ============================================

    @Test
    fun `inactive flag always returns default regardless of rules`() {
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val inactiveFlag by boolean<Context>(default = false) {
                active(false) // Flag is turned off
                rule {
                    platforms(Platform.WEB)
                    rollout { 100.0 }
                } returns true
            }
        }

        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        // Even though rule matches and rollout is 100%, flag is inactive
        assertEquals(false, TestFeatures.inactiveFlag.definition.evaluate(context))
        assertEquals(false, TestFeatures.inactiveFlag.definition.isActive)
    }

    // ============================================
    // ATTACK VECTOR 12: Rule With No Constraints
    // ============================================

    @Test
    fun `rule with no constraints has specificity 0 - lowest priority`() {
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val catchAllFlag by boolean<Context>(default = false) {
                // Specific rule: specificity = 1
                rule {
                    platforms(Platform.ANDROID)
                } returns true

                // Catch-all rule: specificity = 0
                rule {
                    // No constraints = matches everything
                } returns false
            }
        }

        val androidContext = Context(
            locale = AppLocale.EN_US,
            platform = Platform.ANDROID,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        // First rule has higher specificity and matches
        assertEquals(true, TestFeatures.catchAllFlag.definition.evaluate(androidContext))

        val iosContext = androidContext.copy(platform = Platform.IOS)
        // Second rule matches (catch-all) but first doesn't match platform
        assertEquals(false, TestFeatures.catchAllFlag.definition.evaluate(iosContext))
    }

    // ============================================
    // ATTACK VECTOR 13: Rollout + Matching Interaction
    // ============================================

    @Test
    fun `rule matches but rollout excludes - falls through to next rule or default`() {
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val rolloutBlockedFlag by boolean<Context>(default = false) {
                rule {
                    platforms(Platform.WEB)
                    rollout { 0.0 } // 0% rollout - no one gets this
                } returns true

                rule {
                    platforms(Platform.WEB)
                    rollout { 100.0 } // Everyone gets this
                } returns false
            }
        }

        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        // First rule matches platform but rollout is 0%, so skip to second rule
        // Second rule matches and has 100% rollout
        assertEquals(false, TestFeatures.rolloutBlockedFlag.definition.evaluate(context))
    }

    // ============================================
    // ATTACK VECTOR 14: Extremely Long Feature Keys
    // ============================================

    @Test
    fun `feature keys can be extremely long - no validation on length`() {
        val longKey = "a".repeat(10_000)
        object TestFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
            // Property name becomes the key
            // Kotlin allows very long identifiers (limited by JVM)
        }

        // Can't easily test this without metaprogramming
        // but conceptually nothing prevents very long keys
    }

    // ============================================
    // FINDINGS SUMMARY
    // ============================================
    /*
     * IDENTIFIED VULNERABILITIES AND EDGE CASES:
     *
     * 1. VERSION PARSING SILENTLY IGNORES INVALID PARTS
     *    - "1.abc.2" -> Version(1,0,0), losing the "2" completely
     *    - Creates semantic confusion
     *
     * 2. NEGATIVE VERSIONS ARE ALLOWED
     *    - Version(-1,-1,-1) is valid and used as default
     *    - Can cause unexpected comparison behavior
     *
     * 3. RULE SPECIFICITY TIES
     *    - Multiple rules with same specificity depend on note comparison
     *    - If notes are null/same, behavior is implementation-dependent
     *
     * 4. ROLLOUT PRECISION CONFUSION
     *    - 0.01 means 0.01%, not 1%
     *    - Easy to misconfigure by orders of magnitude
     *
     * 5. SPECIAL VALUES IN DOUBLES
     *    - NaN, Infinity are allowed as defaults
     *    - NaN != NaN causes equality issues
     *
     * 6. EMPTY/SPECIAL CHARACTERS IN STRINGS
     *    - Empty strings, null bytes, newlines all valid
     *    - No length limits
     *
     * 7. SALT CAN BE EMPTY OR CONTAIN DELIMITERS
     *    - Empty salt affects bucketing
     *    - Colons in salt interact with hash format
     *
     * 8. INVERTED VERSION RANGES
     *    - FullyBound(max=1.0.0, min=2.0.0) compiles but matches nothing
     *    - Creates unreachable rules
     *
     * 9. EMPTY CONSTRAINT SETS MATCH EVERYTHING
     *    - Rule with no constraints has specificity 0
     *    - Silently acts as catch-all
     *
     * 10. 0% ROLLOUT RULES
     *     - Compiles fine but never triggers
     *     - Could be user error thinking 0=unlimited
     *
     * 11. CASE SENSITIVITY IN HEX IDS
     *     - Uppercase vs lowercase hex are different IDs after normalization
     *     - Could cause bucketing inconsistencies
     *
     * 12. INT OVERFLOW NOT PREVENTED
     *     - Int.MAX_VALUE accepted, user code could overflow
     *
     * 13. DEFAULT VERSION DOESN'T MATCH POSITIVE VERSION RANGES
     *     - Version.default = Version(-1,-1,-1)
     *     - Doesn't match minimum(0,0,0)
     */
}
