# Trade-offs and non-goals

Kotlin code defines the namespace, context type, and feature value type. Changes to those declarations go through the application's code review and release process.

A snapshot can update declared features at runtime. The application owns transport, refresh timing, and error handling. Konditional loads a complete snapshot into one namespace at a time.

Teams that need a hosted control plane, a GUI, cross-language evaluation, or experiment analytics must supply those capabilities separately.
