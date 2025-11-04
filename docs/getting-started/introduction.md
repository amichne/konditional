---
title: Introduction
description: Learn about Konditional and how it can help manage feature flags in your Kotlin projects
---

# Introduction to Konditional

Konditional is a type-safe, deterministic feature flag library for Kotlin that provides compile-time safety and runtime flexibility for managing feature flags in your applications.

## What are Feature Flags?

Feature flags (also known as feature toggles) are a software development technique that allows you to enable or disable features without deploying new code. This enables:

- **Progressive rollouts**: Deploy features to a subset of users
- **A/B testing**: Test different implementations with different user groups
- **Emergency kill switches**: Quickly disable problematic features
- **Development flexibility**: Work on features behind flags before they're ready

## Why Konditional?

### Type-Safe

Konditional leverages Kotlin's powerful type system to provide compile-time safety. Your feature flags are strongly typed, preventing runtime type errors.

### Deterministic

Flag evaluation is predictable and based on clear, testable rules. No surprises in production.

### Serializable

Export and import flag configurations using built-in serialization support. Perfect for:
- Remote configuration management
- A/B testing platforms
- Configuration as code

### Flexible

Support for:
- Version ranges
- Custom value types
- Complex conditional logic
- Rule composition

## Core Concepts

### Flags

A flag is a typed configuration value that can change based on conditions. Each flag has:
- A unique identifier
- A value type (Boolean, String, Int, etc.)
- A default value
- Optional rules for conditional evaluation

### Rules

Rules define when and how flag values should change. Rules can be based on:
- Application version
- User attributes
- Custom conditions
- Time-based criteria

### Conditions

Conditions are the building blocks of rules. They evaluate to true or false based on runtime context.

## Next Steps

Continue to the [Quick Start](/getting-started/quick-start/) guide to begin using Konditional in your project.
