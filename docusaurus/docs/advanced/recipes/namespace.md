# Namespace Isolation + Kill-Switch

Use separate namespaces for independent lifecycles, and a scoped kill-switch for emergencies.

```kotlin
sealed class AppDomain(id: String) : Namespace(id) {
    data object Payments : AppDomain("payments") {
        val applePay by boolean<Context>(default = false)
    }

    data object Search : AppDomain("search") {
        val reranker by boolean<Context>(default = false)
    }
}

fun disablePayments() {
    AppDomain.Payments.disableAll()
}
```

- **Guarantee**: Disabling a namespace only affects that namespace.
- **Mechanism**: Each `Namespace` has an isolated registry and kill-switch.
- **Boundary**: `disableAll()` returns defaults; it does not modify feature definitions or remote config state.

---
