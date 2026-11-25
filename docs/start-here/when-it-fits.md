# When it fits

Choose Konditional when a Kotlin application owns the feature decision and its callers need a specific return type. For example, a checkout rule can select self-service, manual review, or no access.

## Questions for the team

- Can the application run the [library's JVM target](../quickstart/install.md)?
- Who owns the namespace and reviews its rules?
- If users enter a gradual rollout, which stable ID identifies them?
- If operators supply snapshots, what delivers them to the application?

If several languages must evaluate one shared flag contract, consider a cross-language API. If operators need approvals, a console, or experiments, include those needs in the [platform comparison](comparison.md).
