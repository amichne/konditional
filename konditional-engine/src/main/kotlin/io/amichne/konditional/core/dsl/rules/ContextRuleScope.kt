package io.amichne.konditional.core.dsl.rules

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.dsl.rules.targeting.scopes.ExtensionTargetingScope
import io.amichne.konditional.core.dsl.rules.targeting.scopes.FactTargetingScope

/**
 * Base, user-agnostic rule scope that can be composed for configuration-centric targeting.
 */
@KonditionalDsl
@KonditionalInternalApi
interface ContextRuleScope<C : Context> :
    RuleScopeBase<C>,
    FactTargetingScope,
    ExtensionTargetingScope<C>,
    NoteScope<C>
