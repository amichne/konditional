# Konditional

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Codebase](https://github.com/amichne/konditional/actions/workflows/ci.yml/badge.svg)](https://github.com/amichne/konditional/actions/workflows/ci.yml)

Konditional lets you declare feature flags as Kotlin properties. A caller passes
the input for a decision and gets back the Kotlin type the flag declares.

```kotlin
val showNewCheckout: Boolean = CheckoutFlags.newCheckout.evaluate(account)
```

[Run your first flag](docs/quickstart.mdx) to enable a new checkout for pilot accounts.

For rollouts, remote configuration, and custom flag values, see
[Advanced usage](docs/guides.mdx).

## Project

[Contributing](CONTRIBUTING.md) · [Code of conduct](CODE_OF_CONDUCT.md) ·
[Changelog](CHANGELOG.md) · [License](LICENSE)
