package io.amichne.konditional

import io.amichne.konditional.core.Namespace

/**
 * Consumer-defined namespaces used by tests.
 *
 * Konditional intentionally only ships [Namespace.Global]. Any additional namespaces are defined by consumers.
 */
object TestDomains {
    object Payments : Namespace("payments")
    object Search : Namespace("search")
    object Messaging : Namespace("messaging")
    object Authentication : Namespace("auth")
}
