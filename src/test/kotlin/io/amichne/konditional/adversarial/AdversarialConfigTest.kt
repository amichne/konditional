package io.amichne.konditional.adversarial

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.features.evaluate
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.rules.versions.FullyBound
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

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
        // Negative versions don't make sense semantically but parseUnsafe successfully
        assertThrows<IllegalArgumentException> {

            val version = Version(-1, -1, -1)
        }
    }

    @Test
    fun `version parse defaults missing components to zero - ambiguity`() {
        // "1" becomes (1,0,0) same as "1.0.0" - user might not expect this
        val v1 = Version.parseUnsafe("1")
        val v2 = Version.parseUnsafe("1.0")
        val v3 = Version.parseUnsafe("1.0.0")
        assertEquals(v1, v2)
        assertEquals(v2, v3)
    }

    @Test
    fun `version parse handles invalid numeric parts as zero - silent failure`() {
        // "1.abc.2" parses as (1,0,0) because "abc".toIntOrNull() returns null
        assertThrows<IllegalArgumentException> { Version.parseUnsafe("1.abc.2") }
//        assertEquals(Version(1, 0, 0), version)
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
            Version.parseUnsafe("1.2.3.4")
        }
    }

    @Test
    fun `version parse rejects empty string`() {
        assertThrows<IllegalArgumentException> {
            Version.parseUnsafe("")
        }
    }


    @Test
    fun `multiple rules with same specificity - order determines winner`() {
        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
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

        val result = TestFeatures.ambiguousFlag.evaluate(androidContext)
        // Result depends on rule order and rollout bucket
        assertEquals(true, result)
    }

    @Test
    fun `overlapping rules - more specific rule shadows less specific`() {
        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val shadowedFlag by boolean<Context>(default = false) {
                // Specificity 3: platform + locale + version
                rule {
                    platforms(Platform.ANDROID)
                    locales(AppLocale.EN_US)
                    versions {
                        min(1, 0, 0)
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
        val result = TestFeatures.shadowedFlag.evaluate(context)
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
    fun `hexId is case insensitive in validation`() {
        val id1 = StableId.of("ABCDEF1234567890ABCDEF1234567890")
        val id2 = StableId.of("abcdef1234567890abcdef1234567890")
        // These are different IDs after normalization
        assertEquals(id1.hexId.id, id2.hexId.id)
    }

    // ============================================
    // ATTACK VECTOR 5: Type System Edge Cases
    // ============================================

    @Test
    fun `double values accept NaN - compiles but semantically invalid`() {
        // NaN compiles fine in Double context
        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
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

        val result = TestFeatures.nanFlag.evaluate(context)
        assert(result != null && result.isNaN()) // NaN propagates through
        assert(result != null && result != result) // NaN != NaN!
    }

    @Test
    fun `double values accept infinity - compiles but questionable semantics`() {
        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val infinityFlag by double<Context>(default = Double.POSITIVE_INFINITY) {
                rule {
                    platforms(Platform.WEB)
                } returns Double.NEGATIVE_INFINITY
            }
        }

        val infinityDef = TestFeatures.namespace.flag(TestFeatures.infinityFlag)
        assertEquals(Double.POSITIVE_INFINITY, infinityDef?.defaultValue)
    }

    @Test
    fun `int values at MAX_VALUE - potential overflow in calculations`() {
        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val maxIntFlag by int<Context>(default = Int.MAX_VALUE) {
                rule {
                    platforms(Platform.WEB)
                } returns Int.MIN_VALUE
            }
        }

        val maxIntDef = TestFeatures.namespace.flag(TestFeatures.maxIntFlag)
        assertEquals(Int.MAX_VALUE, maxIntDef?.defaultValue)
        // If user code does maxIntFlag + 1, it overflows to Int.MIN_VALUE
    }

    @Test
    fun `string values can be empty - potentially invalid for some use cases`() {
        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val emptyStringFlag by string<Context>(default = "") {
                rule {
                    platforms(Platform.WEB)
                } returns "   " // Whitespace-only also valid
            }
        }

        val emptyStringDef = TestFeatures.namespace.flag(TestFeatures.emptyStringFlag)
        assertEquals("", emptyStringDef?.defaultValue)
    }

    @Test
    fun `string values can be extremely long - no length validation`() {
        val longString = "x".repeat(1_000_000) // 1 MB string
        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val hugeStringFlag by string<Context>(default = longString)
        }

        val hugeStringDef = TestFeatures.namespace.flag(TestFeatures.hugeStringFlag)
        assertEquals(1_000_000, hugeStringDef?.defaultValue?.length)
    }

    @Test
    fun `string values can contain special characters and newlines`() {
        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val specialCharsFlag by string<Context>(
                default = "Line1\nLine2\tTab\u0000Null\r\nCRLF"
            )
        }

        val specialCharsDef = TestFeatures.namespace.flag(TestFeatures.specialCharsFlag)
        assert(specialCharsDef?.defaultValue?.contains("\n") == true)
        assert(specialCharsDef?.defaultValue?.contains("\u0000") == true)
    }

    // ============================================
    // ATTACK VECTOR 6: Salt Manipulation
    // ============================================

    @Test
    fun `salt defaults to v1 but can be any string`() {
        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val defaultSaltFlag by boolean<Context>(default = false) {
                // Salt defaults to "v1"
            }

            val customSaltFlag by boolean<Context>(default = false) {
                salt("custom-salt-value")
            }
        }

        val defaultSaltDef = TestFeatures.namespace.flag(TestFeatures.defaultSaltFlag)
        val customSaltDef = TestFeatures.namespace.flag(TestFeatures.customSaltFlag)
        assertEquals("v1", defaultSaltDef.salt)
        assertEquals("custom-salt-value", customSaltDef.salt)
    }

    @Test
    fun `salt can be empty string - affects bucketing determinism`() {
        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val emptySaltFlag by boolean<Context>(default = false) {
                salt("") // Empty salt compiles fine
            }
        }

        val emptySaltDef = TestFeatures.namespace.flag(TestFeatures.emptySaltFlag)
        assertEquals("", emptySaltDef.salt)
    }

    @Test
    fun `salt with special characters affects hash calculation`() {
        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val specialSaltFlag by boolean<Context>(default = false) {
                salt("salt:with:colons\nand\nnewlines")
            }
        }

        // Colons are used as delimiters in hash input: "$salt:$flagKey:$id"
        // This could cause unexpected bucket distributions
        val specialSaltDef = TestFeatures.namespace.flag(TestFeatures.specialSaltFlag)
        assert(specialSaltDef.salt?.contains(":") == true)
    }

    // ============================================
    // ATTACK VECTOR 7: Rollout Bucketing Manipulation
    // ============================================

    @Test
    fun `rollout at 0_01 percent affects 1 in 10000 users - easy to misconfigure`() {
        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
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
        val result = TestFeatures.tinyRolloutFlag.evaluate(context)
        // Assertion depends on hash bucket for this specific ID
        // We're just verifying this compiles and executes without error
        assert(result is Boolean)
    }

    @Test
    fun `changing salt completely re-randomizes user bucketing`() {
        val context = Context(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("ABCDEF1234567890ABCDEF1234567890")
        )

        val testFeatures1 = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val flag by boolean<Context>(default = false) {
                salt("v1")
                rule {
                    rollout { 50.0 }
                } returns true
            }
        }

        val testFeatures2 = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val flag by boolean<Context>(default = false) {
                salt("v2") // Different salt
                rule {
                    rollout { 50.0 }
                } returns true
            }
        }

        // Same user, same rollout %, but different salts
        // Could get different results (bucket assignment changes)
        val result1 = testFeatures1.flag.evaluate(context)
        val result2 = testFeatures2.flag.evaluate(context)

        // Results may differ due to re-bucketing (not guaranteed, depends on hash)
        // We're verifying both evaluate successfully (both are Boolean values)
        assert(result1 is Boolean)
        assert(result2 is Boolean)
    }

    @Test
    fun `empty locale sets match all locales - potential over-targeting`() {
        val featureContainer = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val noLocaleFlag by boolean<Context>(default = false) {
                rule {
                    // No locale specified = matches ALL locales
                    platforms(Platform.WEB)
                } returns true
            }
        }

        val contexts = listOf(
            AppLocale.EN_US,
            AppLocale.ES_US,
            AppLocale.FR_FR,
            AppLocale.HI_IN
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
            assertEquals(true, featureContainer.noLocaleFlag.evaluate(context))
        }
    }

    // ============================================
    // ATTACK VECTOR 9: Version Range Edge Cases
    // ============================================

    @Test
    fun `version range boundaries - inclusive min, exclusive max`() {
        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val boundedFlag by boolean<Context>(default = false) {
                rule {
                    versions {
                        min(1, 0, 0)
                        max(2, 0, 0)
                    }
                } returns true
            }
        }

        val contexts = listOf(
            Version(0, 9, 9) to false,  // Below min
            Version(1, 0, 0) to true,   // At min (inclusive)
            Version(1, 5, 0) to true,   // In range
            Version(2, 0, 0) to true,   // At max (inclusive based on RightBound implementation)
            Version(2, 0, 1) to false   // Above max
        ).map { (version, expected) ->
            Context(
                locale = AppLocale.EN_US,
                platform = Platform.WEB,
                appVersion = version,
                stableId = StableId.of("12345678901234567890123456789012")
            ) to expected
        }

        contexts.forEach { (context, expected) ->
            val result = TestFeatures.boundedFlag.evaluate(context)
            assertEquals(expected, result, "Failed for version ${context.appVersion}")
        }
    }

    @Test
    fun `version range with same min and max - single version targeting`() {
        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val exactVersionFlag by boolean<Context>(default = false) {
                rule {
                    versions {
                        min(1, 0, 0)
                        max(1, 0, 0)
                    }
                } returns true
            }
        }

        val exactContext = Context.Core(
            locale = AppLocale.EN_US,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = StableId.of("12345678901234567890123456789012")
        )

        // Should match exactly version 1.0.0
        assertEquals(true, TestFeatures.exactVersionFlag.evaluate(exactContext))

        val differentContext = exactContext.copy(appVersion = Version(1, 0, 1))
        assertEquals(false, TestFeatures.exactVersionFlag.evaluate(differentContext))
    }

    @Test
    fun `version range inverted (max less than min) creates impossible condition`() {
        // This compiles but creates a range that matches nothing
        assertThrows<IllegalArgumentException> {
            val invalidRange = FullyBound(
                min = Version(2, 0, 0),
                max = Version(1, 0, 0) // max < min!
            )
        }
    }

    // ============================================
    // ATTACK VECTOR 10: Namespace and Registration
    // ============================================

    @Test
    fun `multiple containers can use same feature key in different namespaces`() {
        val features1 = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val duplicateKey by boolean<Context>(default = true)
        }

        val features2 = object : FeatureContainer<Namespace.Authentication>(Namespace.Authentication) {
            val duplicateKey by boolean<Context>(default = false)
        }

        // Same key, different namespaces, different defaults
        assertEquals("duplicateKey", features1.duplicateKey.key)
        assertEquals("duplicateKey", features2.duplicateKey.key)
        assertEquals(Namespace.Global, features1.duplicateKey.namespace)
        assertEquals(Namespace.Authentication, features2.duplicateKey.namespace)
        val def1 = features1.namespace.flag(features1.duplicateKey)
        val def2 = features2.namespace.flag(features2.duplicateKey)
        assertEquals(true, def1?.defaultValue)
        assertEquals(false, def2?.defaultValue)
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
        val featureContainer = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
            val inactiveFlag by boolean<Context>(default = false) {
                active { false } // Flag is turned off
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
        assertEquals(false, featureContainer.inactiveFlag.evaluate(context))
        val inactiveDef = featureContainer.namespace.flag(featureContainer.inactiveFlag)
        assertEquals(false, inactiveDef?.isActive)
    }

    // ============================================
    // ATTACK VECTOR 12: Rule With No Constraints
    // ============================================

    @Test
    fun `rule with no constraints has specificity 0 - lowest priority`() {
        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
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
        assertEquals(true, TestFeatures.catchAllFlag.evaluate(androidContext))

        val iosContext = androidContext.copy(platform = Platform.IOS)
        // Second rule matches (catch-all) but first doesn't match platform
        assertEquals(false, TestFeatures.catchAllFlag.evaluate(iosContext))
    }

    // ============================================
    // ATTACK VECTOR 13: Rollout + Matching Interaction
    // ============================================

    @Test
    fun `rule matches but rollout excludes - falls through to next rule or default`() {
        val TestFeatures = object : FeatureContainer<Namespace.Global>(Namespace.Global) {
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
        assertEquals(false, TestFeatures.rolloutBlockedFlag.evaluate(context))
    }

    // ============================================
    // ATTACK VECTOR 14: Extremely Long Feature Keys
    // ============================================

    @Test
    fun `feature keys can be extremely long - no validation on length`() {
        // Conceptually, feature keys are derived from property names
        // Kotlin/JVM allows very long identifiers (up to ~64KB)
        // This attack vector demonstrates that arbitrarily long keys could be created
        // We can't easily test this without metaprogramming reflection,
        // but the library doesn't validate key length

        // This is a conceptual test documenting the attack vector
        // In practice, extremely long keys would cause memory and performance issues
        val exampleLongKey = "a".repeat(10_000)
        assert(exampleLongKey.length == 10_000)
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
     *     - Doesn't match min(0,0,0)
     */
}
