## Axis refactor

- Simplified Axis handles to explicit factory registration only (`Axis.of(...)`).
- Removed implicit axis registration and alias resolution; axis type lookup now requires prior declaration.
- Removed delegate-based `AxisDefinition` extension path to reduce API surface and ambiguity.
- Added explicit rule targeting overloads (`axis(axisHandle, values...)`) and documented the explicit-first model.
