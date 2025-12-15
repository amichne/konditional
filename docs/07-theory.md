# Theory & Guiding Principles

This page aims to outline the theory behind Konditional,
and especially to justify why the claims made regarding safety are guarantably true.

The core of this is built on the notion of "meta-schemas", or a schema which serves to describe another schema. 

We leverage a combination

Fundamentally, at the end of the theory, we are left with a few incredibly valuable traits:
* Compile-time structure owns all structural constraints
  * This includes any bounds on permitted values
* Runtime data is dynamic, but guaranteed to with inside the bounds defined by the compile-time structure
* 

---

## Metaschema Influence (and Metaschema as Type Safety)

Konditional treats your compiled flag definitions as a **metaschema**: a rigorous specification, which serves as the 
basis for enumerating all permitted states.

This is similar in spirit to:

- JSON Schema: a schema describing valid JSON documents
- RPC: an interface description language describing valid remote calls
  - But instead of valid calls, this describes valid states of a system
- Hyperschema-style thinking: a “superstructure” that describes not only shape, but how to interpret and act on data

In Konditional, that “superstructure” includes:

- Flag keys (derived from properties)
- Flag value types (Boolean/String/Int/Double/Enum)
- Context type parameterization per flag
- Rule structure (platform/locale/version/rollout + custom extensions)

Because the metaschema is compiled Kotlin, it acts as a proxy for type safety: **if the schema compiles, it can be
trusted as structurally valid**.

---

## Architecture Principles

- System divides responsibility between compiled code and runtime components
- Design follows a phasic transition from initialization to operational state
- Architecture balances compile-time guarantees with runtime flexibility
- Approach ensures both predictability and resilience

---

## Compiled Code Responsibilities

Compiled code is the source of truth for structure:

- Owns feature definitions (keys, types, defaults)
- Owns custom targeting extensions (your `extension { }` logic)
- Guarantees a valid initial state (defaults + definitions)
- Enforces that initial state is tightly coupled to compilability

Importantly: compiled code establishes the starting point, but does not need to “own” runtime updates forever.

---

## Runtime Handling (Operational Phase)

After initialization, responsibility transitions to runtime parsing and failure handling:

- Runtime loads remote configuration (JSON/UI-driven rule updates)
- Parsing failures are handled gracefully and do not enter the operational state
- Evaluation remains resilient because parsing is not on the critical path
- Operational evaluation stays deterministic and local

This separation maintains dynamicism (rules can change) without weakening the guarantees (structure cannot drift).

---

## Implementation Strategy

- Avoids indirection common in JVM solutions (no vague reflection mechanisms)
- Uses explicit parameter passing to constructors
- Implements initial state via direct invocation
  - That invocation is the single root for all resolved types, guaranteeing initial state validity
- Eager evaluation at initialization serves as a fall-back
  - Only relevant if something manages to slip past compilation (the intended surface area for this is very small)
