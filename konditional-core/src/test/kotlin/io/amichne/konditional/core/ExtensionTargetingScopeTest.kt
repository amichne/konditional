@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.dsl.rules.targeting.scopes.whenContext
import io.amichne.konditional.core.features.BooleanFeature
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.fixtures.EnterpriseContext
import io.amichne.konditional.fixtures.SubscriptionTier
import io.amichne.konditional.fixtures.UserRole
import io.amichne.konditional.fixtures.utilities.update
import io.amichne.konditional.rules.predicate.and
import io.amichne.konditional.rules.predicate.allOf
import io.amichne.konditional.rules.predicate.anyOf
import io.amichne.konditional.rules.predicate.not
import io.amichne.konditional.rules.predicate.predicateOf
import io.amichne.konditional.rules.predicate.or
import io.amichne.konditional.rules.predicate.where
import io.amichne.konditional.rules.predicate.PredicateRef
import io.amichne.konditional.rules.targeting.Targeting
import io.amichne.konditional.values.PredicateId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ExtensionTargetingScopeTest {
    private data class EvalCase(
        val idHex: String,
        val subscriptionTier: SubscriptionTier,
        val userRole: UserRole,
        val organizationId: String = "org-123",
    )

    private object Features : Namespace.TestNamespaceFacade("extension-targeting-scope-test") {
        val extensionComposition by boolean<EnterpriseContext>(default = false)
        val specificityOrdering by string<EnterpriseContext>(default = "default")
        val enterpriseOnlyOnBaseContext by boolean<Context>(default = false)
    }

    private fun baseContext(idHex: String): Context = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(1, 0, 0),
        stableId = StableId.of(idHex),
    )

    private fun enterpriseContext(
        idHex: String,
        subscriptionTier: SubscriptionTier,
        userRole: UserRole,
        organizationId: String = "org-123",
    ): EnterpriseContext = EnterpriseContext(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(1, 0, 0),
        stableId = StableId.of(idHex),
        organizationId = organizationId,
        subscriptionTier = subscriptionTier,
        userRole = userRole,
    )

    private fun assertFeatureValue(
        feature: BooleanFeature<EnterpriseContext, *>,
        expected: Boolean,
        evalCase: EvalCase,
    ) {
        assertEquals(
            expected,
            feature.evaluate(
                enterpriseContext(
                    idHex = evalCase.idHex,
                    subscriptionTier = evalCase.subscriptionTier,
                    userRole = evalCase.userRole,
                    organizationId = evalCase.organizationId,
                ),
            ),
        )
    }

    @Test
    fun `multiple extension blocks compose with AND semantics`() {
        Features.extensionComposition.update(default = false) {
            rule(true) {
                extension { subscriptionTier == SubscriptionTier.PREMIUM }
                extension { userRole == UserRole.OWNER }
            }
        }

        val basicOwner = enterpriseContext(
            idHex = "11111111111111111111111111111111",
            subscriptionTier = SubscriptionTier.BASIC,
            userRole = UserRole.OWNER,
        )
        val premiumOwner = enterpriseContext(
            idHex = "22222222222222222222222222222222",
            subscriptionTier = SubscriptionTier.PREMIUM,
            userRole = UserRole.OWNER,
        )

        assertEquals(false, Features.extensionComposition.evaluate(basicOwner))
        assertEquals(true, Features.extensionComposition.evaluate(premiumOwner))
    }

    @Test
    fun `multiple extension blocks increase specificity cumulatively`() {
        Features.specificityOrdering.update(default = "default") {
            rule("single-extension") {
                extension { userRole == UserRole.OWNER }
            }
            rule("double-extension") {
                extension { userRole == UserRole.OWNER }
                extension {
                    subscriptionTier == SubscriptionTier.PREMIUM ||
                        subscriptionTier == SubscriptionTier.ENTERPRISE
                }
            }
        }

        val premiumOwner = enterpriseContext(
            idHex = "33333333333333333333333333333333",
            subscriptionTier = SubscriptionTier.PREMIUM,
            userRole = UserRole.OWNER,
        )

        assertEquals("double-extension", Features.specificityOrdering.evaluate(premiumOwner))
    }

    @Test
    fun `whenContext evaluates only when runtime context supports the requested capability`() {
        Features.enterpriseOnlyOnBaseContext.update(default = false) {
            rule(true) {
                whenContext<EnterpriseContext> {
                    subscriptionTier == SubscriptionTier.ENTERPRISE
                }
            }
        }

        val base = baseContext(idHex = "44444444444444444444444444444444")
        val enterprise = enterpriseContext(
            idHex = "55555555555555555555555555555555",
            subscriptionTier = SubscriptionTier.ENTERPRISE,
            userRole = UserRole.ADMIN,
        )
        val basicEnterprise = enterpriseContext(
            idHex = "66666666666666666666666666666666",
            subscriptionTier = SubscriptionTier.BASIC,
            userRole = UserRole.ADMIN,
        )

        assertEquals(false, Features.enterpriseOnlyOnBaseContext.evaluate(base))
        assertEquals(true, Features.enterpriseOnlyOnBaseContext.evaluate(enterprise))
        assertEquals(false, Features.enterpriseOnlyOnBaseContext.evaluate(basicEnterprise))
    }

    @Test
    fun `predicate ref resolves through namespace registry`() {
        val ref = PredicateRef.Registered(Features.id, PredicateId("is-owner-ref"))
        Features.predicates<EnterpriseContext>().register(
            ref,
            Targeting.Custom(block = { it.userRole == UserRole.OWNER }),
        )

        Features.extensionComposition.update(default = false) {
            rule(true) {
                predicate(ref)
            }
        }

        val admin = enterpriseContext(
            idHex = "77777777777777777777777777777777",
            subscriptionTier = SubscriptionTier.PREMIUM,
            userRole = UserRole.ADMIN,
        )
        val owner = enterpriseContext(
            idHex = "88888888888888888888888888888888",
            subscriptionTier = SubscriptionTier.PREMIUM,
            userRole = UserRole.OWNER,
        )

        assertEquals(false, Features.extensionComposition.evaluate(admin))
        assertEquals(true, Features.extensionComposition.evaluate(owner))
    }

    @Test
    fun `predicate ref fails fast when unregistered`() {
        val missing = PredicateRef.Registered(Features.id, PredicateId("missing-owner-ref"))

        val error = assertFailsWith<KonditionalBoundaryFailure> {
            Features.extensionComposition.update(default = false) {
                rule(true) {
                    predicate(missing)
                }
            }
        }

        assertIs<ParseError.UnknownPredicate>(error.parseError)
    }

    @Test
    fun `anonymous require blocks compose with AND semantics`() {
        Features.extensionComposition.update(default = false) {
            rule(true) {
                require { subscriptionTier == SubscriptionTier.PREMIUM }
                require { userRole == UserRole.OWNER }
            }
        }

        val premiumAdmin = enterpriseContext(
            idHex = "99999999999999999999999999999999",
            subscriptionTier = SubscriptionTier.PREMIUM,
            userRole = UserRole.ADMIN,
        )
        val premiumOwner = enterpriseContext(
            idHex = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            subscriptionTier = SubscriptionTier.PREMIUM,
            userRole = UserRole.OWNER,
        )

        assertEquals(false, Features.extensionComposition.evaluate(premiumAdmin))
        assertEquals(true, Features.extensionComposition.evaluate(premiumOwner))
    }

    @Test
    @Suppress("LongMethod")
    fun `predicate expressions support grouped conjunction disjunction and negation`() {
        val namespace = object : Namespace("predicate-expression-${System.nanoTime()}") {
            val isPremium by predicate<EnterpriseContext> {
                subscriptionTier == SubscriptionTier.PREMIUM
            }
            val isOwner by predicate<EnterpriseContext> {
                userRole == UserRole.OWNER
            }

            private val eligible =
                isPremium and (isOwner or !where<EnterpriseContext> { organizationId == "blocked-org" })

            val composed by boolean<EnterpriseContext>(default = false) {
                rule(true) {
                    require(eligible)
                }
            }
        }

        assertFeatureValue(
            namespace.composed,
            true,
            EvalCase(
                idHex = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                subscriptionTier = SubscriptionTier.PREMIUM,
                userRole = UserRole.ADMIN,
            ),
        )
        assertFeatureValue(
            namespace.composed,
            true,
            EvalCase(
                idHex = "cccccccccccccccccccccccccccccccc",
                subscriptionTier = SubscriptionTier.PREMIUM,
                userRole = UserRole.OWNER,
                organizationId = "blocked-org",
            ),
        )
        assertFeatureValue(
            namespace.composed,
            false,
            EvalCase(
                idHex = "dddddddddddddddddddddddddddddddd",
                subscriptionTier = SubscriptionTier.PREMIUM,
                userRole = UserRole.ADMIN,
                organizationId = "blocked-org",
            ),
        )
        assertFeatureValue(
            namespace.composed,
            false,
            EvalCase(
                idHex = "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee",
                subscriptionTier = SubscriptionTier.BASIC,
                userRole = UserRole.OWNER,
            ),
        )
    }

    @Test
    fun `predicate expressions normalize nested negation and explicit grouping`() {
        val namespace = object : Namespace("predicate-negation-${System.nanoTime()}") {
            val isPremium by predicate<EnterpriseContext> {
                subscriptionTier == SubscriptionTier.PREMIUM
            }
            val isOwner by predicate<EnterpriseContext> {
                userRole == UserRole.OWNER
            }

            private val allowed = allOf(
                !(!isPremium),
                !anyOf(
                    predicateOf<EnterpriseContext> { organizationId == "blocked-org" },
                    allOf(
                        predicateOf<EnterpriseContext> { userRole == UserRole.ADMIN },
                        !isOwner,
                    ),
                ),
            )

            val composed by boolean<EnterpriseContext>(default = false) {
                rule(true) {
                    require(allowed)
                }
            }
        }

        assertFeatureValue(
            namespace.composed,
            true,
            EvalCase(
                idHex = "ffffffffffffffffffffffffffffffff",
                subscriptionTier = SubscriptionTier.PREMIUM,
                userRole = UserRole.OWNER,
            ),
        )
        assertFeatureValue(
            namespace.composed,
            false,
            EvalCase(
                idHex = "12121212121212121212121212121212",
                subscriptionTier = SubscriptionTier.PREMIUM,
                userRole = UserRole.OWNER,
                organizationId = "blocked-org",
            ),
        )
        assertFeatureValue(
            namespace.composed,
            false,
            EvalCase(
                idHex = "13131313131313131313131313131313",
                subscriptionTier = SubscriptionTier.PREMIUM,
                userRole = UserRole.ADMIN,
            ),
        )
    }
}
