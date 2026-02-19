file=konditional-core/src/test/kotlin/io/amichne/konditional/context/ContextPolymorphismTest.kt
package=io.amichne.konditional.context
imports=io.amichne.konditional.api.evaluate,io.amichne.konditional.core.dsl.enable,io.amichne.konditional.core.id.StableId,io.amichne.konditional.fixtures.CompositeContext,io.amichne.konditional.fixtures.EnterpriseContext,io.amichne.konditional.fixtures.EnterpriseFeatures,io.amichne.konditional.fixtures.EnterpriseFeatures.advanced_analytics,io.amichne.konditional.fixtures.ExperimentContext,io.amichne.konditional.fixtures.ExperimentFeatures,io.amichne.konditional.fixtures.SubscriptionTier,io.amichne.konditional.fixtures.UserRole,io.amichne.konditional.fixtures.core.id.TestStableId,io.amichne.konditional.fixtures.enterpriseRule,io.amichne.konditional.fixtures.utilities.update,kotlin.test.Test,kotlin.test.assertEquals,kotlin.test.assertFalse,kotlin.test.assertTrue
type=io.amichne.konditional.context.ContextPolymorphismTest|kind=class|decl=class ContextPolymorphismTest
methods:
- fun `Given EnterpriseContext, When evaluating flags, Then context-specific properties are accessible`()
- fun `Given ExperimentContext, When evaluating flags, Then experiment-specific properties are accessible`()
- fun `Given multiple custom contexts, When using different flags, Then contexts are independent`()
- fun `Given custom EnterpriseRule, When matching with business logic, Then custom properties are enforced`()
- fun `Given CompositeContext, When evaluating flags, Then delegated properties are accessible`()
