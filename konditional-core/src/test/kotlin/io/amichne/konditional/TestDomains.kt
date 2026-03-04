package io.amichne.konditional

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.values.NamespaceId

/**
 * Consumer-defined namespaces used by tests.
 */
object TestDomains {
    object Payments : Namespace(NamespaceId("payments"))
    object Search : Namespace(NamespaceId("search"))
    object Messaging : Namespace(NamespaceId("messaging"))
    object Authentication : Namespace(NamespaceId("auth"))
}
