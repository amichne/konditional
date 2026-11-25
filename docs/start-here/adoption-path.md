# Adopt Konditional in one module

Pick a decision with one caller, such as whether to show a new checkout.
Keep its namespace in the Gradle module that owns the decision.
[Run your first flag](../quickstart/index.md) to try this pattern.

If you are replacing a string flag, move one caller at a time. Compare both
results while the old flag is still available.
[Migrate a caller from string flags](../guides/migrating-from-string-flags.md).

Add more flags to the namespace when they belong to the same team and change
together. If you need to roll out a flag gradually or update its rules between
releases, choose the matching guide in [Advanced usage](../guides/index.md).

