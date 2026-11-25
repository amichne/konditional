package io.amichne.konditional.engine

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.internal.bindRule
import io.amichne.konditional.internal.serialize
import io.amichne.konditional.internal.toSerializedRules
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.targeting.Targeting
import io.amichne.konditional.rules.targeting.TargetingSchema
import io.amichne.konditional.rules.versions.Unbounded
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TargetingSnapshotTest {
    @Test
    fun repeatedPlatformClausesRemainAnIntersectionAndKeepTheirPrecedence() {
        val flags = object : Namespace("portable-platform") {
            val checkout by boolean<Context>(default = false) {
                enable {
                    platforms(Platform.IOS, Platform.ANDROID)
                    platforms(Platform.IOS)
                }
            }
        }
        val rules = flags.flag(flags.checkout).toSerializedRules()
        val tree = requireNotNull(rules.single().targeting)
        val rule = assertIs<ParseResult.Success<Targeting.All<Context>>>(tree.bindRule(TargetingSchema<Context>())).value
        assertEquals(2, rule.specificity())
        assertTrue(rule.matches(object : Context.PlatformContext { override val platform = Platform.IOS }))
        assertFalse(rule.matches(object : Context.PlatformContext { override val platform = Platform.ANDROID }))
    }

    @Test
    fun anUnboundedVersionStillNeedsTheVersionCapability() {
        val flags = object : Namespace("portable-version") {
            val checkout by boolean<Context>(default = false)
        }
        val versioned = Rule<Context>(targeting = Targeting.All(listOf(Targeting.version(Unbounded))))
        val definition = FlagDefinition(feature = flags.checkout, defaultValue = false, values = listOf(versioned.targetedBy(true)))
        val tree = requireNotNull(definition.toSerializedRules().single().targeting)
        val rule = assertIs<ParseResult.Success<Targeting.All<Context>>>(tree.bindRule(TargetingSchema<Context>())).value
        assertTrue(rule.matches(object : Context.VersionContext { override val appVersion = Version.default }))
        assertFalse(rule.matches(object : Context {}))
    }

    @Test
    fun exportRejectsAnApplicationGuardEvenAroundASupportedLeaf() {
        val guard = Targeting.Guarded<Context, Context.PlatformContext>(Targeting.Platform(setOf(Platform.IOS.id))) { null }
        val failure = assertFailsWith<KonditionalBoundaryFailure> { guard.serialize() }
        assertIs<ParseError.InvalidSnapshot>(failure.parseError)
    }

    @Test
    fun aContextualValueCannotExportAMarkerThatDropsDisjunction() {
        val flags = object : Namespace("local-resolver") {
            val checkout by boolean<Context>(default = false) {
                rule { anyOf { platforms(Platform.IOS) } } yields { true }
            }
        }
        val failure = assertFailsWith<KonditionalBoundaryFailure> { flags.flag(flags.checkout).toSerializedRules() }
        assertIs<ParseError.InvalidSnapshot>(failure.parseError)
    }
}
