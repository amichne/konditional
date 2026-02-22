# How-to: test feature behavior end to end

Use this page to prove that feature evaluation is deterministic, type-safe at
boundaries, and stable under concurrent refresh.

## Read this page when

- You are adding or changing feature rules.
- You are adding remote configuration support.
- You are preparing a rollout or migration in production.

## Test suite matrix

| Test class | Purpose | Canonical guide |
| --- | --- | --- |
| Rule evaluation unit tests | Validate targeting semantics | [Roll out gradually](/how-to-guides/rolling-out-gradually) |
| Determinism tests | Prove same input yields same output | [Debugging determinism](/how-to-guides/debugging-determinism) |
| Parse-boundary tests | Validate typed load failures | [Safe remote config](/how-to-guides/safe-remote-config) |
| Atomicity tests | Ensure no partial reads during refresh | [Thread safety](/production-operations/thread-safety) |
| Isolation tests | Prevent cross-namespace blast radius | [Namespace isolation](/how-to-guides/namespace-isolation) |

## Deterministic steps

1. Add unit tests for explicit rule expectations.

```kotlin
@Test
fun `enterprise users get premium UI`() {
    val result = BillingFlags.premiumUi.evaluate(enterpriseContext)
    assertTrue(result)
}
```

2. Add determinism replay tests.

```kotlin
@Test
fun `same tuple yields same rollout decision`() {
    val first = CheckoutFlags.newCheckout.evaluate(ctx)
    val second = CheckoutFlags.newCheckout.evaluate(ctx)
    assertEquals(first, second)
}
```

3. Add parse-boundary failure tests.

```kotlin
@Test
fun `invalid snapshot returns typed parse error`() {
    val result = NamespaceSnapshotLoader(AppFeatures).load(invalidJson)
    assertTrue(result.isFailure)
}
```

4. Add atomicity smoke tests for concurrent refresh.

- Run concurrent reader and writer loops.
- Assert each read observes only old or new snapshots, never mixed state.

5. Add namespace isolation tests.

- Refresh namespace A while evaluating namespace B.
- Assert namespace B results are unchanged.

## Coverage checklist

- [ ] Unit tests cover all critical rule branches.
- [ ] Determinism tests pin expected behavior for stable tuples.
- [ ] Parse failures are asserted by typed error class.
- [ ] Atomic refresh behavior is tested under concurrency.
- [ ] Namespace isolation is tested with independent refresh cycles.

## Next steps

- [Operational debugging](/production-operations/debugging)
- [Failure modes](/production-operations/failure-modes)
- [Troubleshooting](/troubleshooting/)
