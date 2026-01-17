/**
 * Konditional Editor - Validation System
 * 
 * Eagerly validates flag configurations against schema constraints.
 * Produces errors (block save) and warnings (allow save with confirmation).
 * 
 * DESIGN PRINCIPLE: If a condition is provably impossible or aberrant,
 * surface it immediately rather than allowing silent misconfiguration.
 */

import type {
  Flag,
  Rule,
  FlagValue,
  VersionRange,
  SchemaMetadata,
  Snapshot,
  EnumFlagValue,
  DataClassFlagValue,
  DataClassSchema,
  Version,
} from './schema';

// =============================================================================
// Validation Result Types
// =============================================================================

export type ValidationSeverity = 'error' | 'warning';

export interface ValidationIssue {
  severity: ValidationSeverity;
  code: ValidationCode;
  message: string;
  /** Path to the problematic field: ['flags', 0, 'rules', 1, 'platforms'] */
  path: (string | number)[];
}

export interface ValidationResult {
  valid: boolean;
  issues: ValidationIssue[];
  errorCount: number;
  warningCount: number;
}

// =============================================================================
// Validation Codes (exhaustive enumeration of detectable issues)
// =============================================================================

export type ValidationCode =
  // Errors (block save)
  | 'EMPTY_PLATFORM_SET'
  | 'EMPTY_LOCALE_SET'
  | 'VERSION_MIN_EXCEEDS_MAX'
  | 'UNKNOWN_ENUM_VALUE'
  | 'DATACLASS_MISSING_REQUIRED_FIELD'
  | 'DATACLASS_FIELD_TYPE_MISMATCH'
  | 'DATACLASS_UNKNOWN_CLASS'
  | 'ENUM_UNKNOWN_CLASS'
  | 'RAMPUP_OUT_OF_BOUNDS'
  | 'INVALID_HEX_STABLE_ID'
  // Warnings (allow save with confirmation)
  | 'RAMPUP_ZERO_NO_ALLOWLIST'
  | 'DATACLASS_UNKNOWN_FIELD'
  | 'DUPLICATE_RULE'
  | 'SHADOWED_RULE'
  | 'FLAG_INACTIVE_WITH_RULES';

// =============================================================================
// Validation Context
// =============================================================================

interface ValidationContext {
  schema: SchemaMetadata | undefined;
  issues: ValidationIssue[];
  currentPath: (string | number)[];
}

function pushIssue(
  ctx: ValidationContext,
  severity: ValidationSeverity,
  code: ValidationCode,
  message: string
): void {
  ctx.issues.push({
    severity,
    code,
    message,
    path: [...ctx.currentPath],
  });
}

function withPath<T>(
  ctx: ValidationContext,
  segment: string | number,
  fn: () => T
): T {
  ctx.currentPath.push(segment);
  try {
    return fn();
  } finally {
    ctx.currentPath.pop();
  }
}

// =============================================================================
// Version Comparison Utilities
// =============================================================================

function compareVersions(a: Version, b: Version): number {
  if (a.major !== b.major) return a.major - b.major;
  if (a.minor !== b.minor) return a.minor - b.minor;
  return a.patch - b.patch;
}

// =============================================================================
// Individual Validators
// =============================================================================

function validateVersionRange(
  ctx: ValidationContext,
  range: VersionRange
): void {
  if (range.type === 'MIN_AND_MAX_BOUND') {
    if (compareVersions(range.min, range.max) > 0) {
      pushIssue(
        ctx,
        'error',
        'VERSION_MIN_EXCEEDS_MAX',
        `Version minimum (${range.min.major}.${range.min.minor}.${range.min.patch}) ` +
        `exceeds maximum (${range.max.major}.${range.max.minor}.${range.max.patch})`
      );
    }
  }
}

function validateRampUp(ctx: ValidationContext, rampUp: number): void {
  if (rampUp < 0 || rampUp > 100) {
    pushIssue(
      ctx,
      'error',
      'RAMPUP_OUT_OF_BOUNDS',
      `Ramp-up percentage ${rampUp} is outside valid range [0, 100]`
    );
  }
}

function validateHexStableId(ctx: ValidationContext, id: string): void {
  // StableId should be a hex-encoded string
  if (!/^[0-9a-fA-F]+$/.test(id)) {
    pushIssue(
      ctx,
      'error',
      'INVALID_HEX_STABLE_ID',
      `Allowlist entry "${id}" is not valid hexadecimal`
    );
  }
}

function validateEnumValue(
  ctx: ValidationContext,
  value: EnumFlagValue
): void {
  if (!ctx.schema) return;

  const validValues = ctx.schema.enums[value.enumClassName];
  if (!validValues) {
    pushIssue(
      ctx,
      'error',
      'ENUM_UNKNOWN_CLASS',
      `Unknown enum class "${value.enumClassName}"`
    );
    return;
  }

  if (!validValues.includes(value.value)) {
    pushIssue(
      ctx,
      'error',
      'UNKNOWN_ENUM_VALUE',
      `"${value.value}" is not a valid value for enum ${value.enumClassName}. ` +
      `Valid values: ${validValues.join(', ')}`
    );
  }
}

function validateDataClassValue(
  ctx: ValidationContext,
  value: DataClassFlagValue
): void {
  if (!ctx.schema) return;

  const schema = ctx.schema.dataClasses[value.dataClassName];
  if (!schema) {
    pushIssue(
      ctx,
      'error',
      'DATACLASS_UNKNOWN_CLASS',
      `Unknown data class "${value.dataClassName}"`
    );
    return;
  }

  validateDataClassAgainstSchema(ctx, value.value, schema);
}

function validateDataClassAgainstSchema(
  ctx: ValidationContext,
  data: Record<string, unknown>,
  schema: DataClassSchema
): void {
  // Check required fields
  for (const requiredField of schema.required) {
    if (!(requiredField in data)) {
      withPath(ctx, requiredField, () => {
        pushIssue(
          ctx,
          'error',
          'DATACLASS_MISSING_REQUIRED_FIELD',
          `Required field "${requiredField}" is missing`
        );
      });
    }
  }

  // Check each provided field
  for (const [fieldName, fieldValue] of Object.entries(data)) {
    withPath(ctx, fieldName, () => {
      const fieldSchema = schema.properties[fieldName];
      if (!fieldSchema) {
        pushIssue(
          ctx,
          'warning',
          'DATACLASS_UNKNOWN_FIELD',
          `Field "${fieldName}" is not defined in schema (will be ignored)`
        );
        return;
      }

      // Type checking
      const actualType = getJsonType(fieldValue);
      if (!isTypeCompatible(actualType, fieldSchema.type)) {
        pushIssue(
          ctx,
          'error',
          'DATACLASS_FIELD_TYPE_MISMATCH',
          `Field "${fieldName}" has type ${actualType}, expected ${fieldSchema.type}`
        );
      }
    });
  }
}

function getJsonType(value: unknown): string {
  if (value === null) return 'null';
  if (Array.isArray(value)) return 'array';
  if (typeof value === 'number') {
    return Number.isInteger(value) ? 'integer' : 'number';
  }
  return typeof value;
}

function isTypeCompatible(actual: string, expected: string): boolean {
  if (actual === expected) return true;
  // integer is compatible with number
  if (actual === 'integer' && expected === 'number') return true;
  return false;
}

function validateFlagValue(ctx: ValidationContext, value: FlagValue): void {
  switch (value.type) {
    case 'ENUM':
      validateEnumValue(ctx, value);
      break;
    case 'DATA_CLASS':
      validateDataClassValue(ctx, value);
      break;
    // Primitive types have no additional validation beyond type correctness
  }
}

function validateRule(ctx: ValidationContext, rule: Rule, ruleIndex: number): void {
  withPath(ctx, ruleIndex, () => {
    // Platform set validation
    withPath(ctx, 'platforms', () => {
      if (rule.platforms.length === 0) {
        pushIssue(
          ctx,
          'error',
          'EMPTY_PLATFORM_SET',
          'Rule has empty platform set and can never match'
        );
      }
    });

    // Locale validation - handle both single locale and array
    withPath(ctx, 'locales', () => {
      const locales = Array.isArray(rule.locales) ? rule.locales : [rule.locales];
      if (locales.length === 0) {
        pushIssue(
          ctx,
          'error',
          'EMPTY_LOCALE_SET',
          'Rule has empty locale set and can never match'
        );
      }
    });

    // Version range validation
    withPath(ctx, 'versionRange', () => {
      validateVersionRange(ctx, rule.versionRange);
    });

    // Ramp-up validation
    withPath(ctx, 'rampUp', () => {
      validateRampUp(ctx, rule.rampUp);
    });

    // Zero ramp-up with no allowlist warning
    if (rule.rampUp === 0 && rule.rampUpAllowlist.length === 0) {
      pushIssue(
        ctx,
        'warning',
        'RAMPUP_ZERO_NO_ALLOWLIST',
        'Rule has 0% ramp-up with no allowlist entries and is effectively disabled'
      );
    }

    // Allowlist validation
    withPath(ctx, 'rampUpAllowlist', () => {
      rule.rampUpAllowlist.forEach((id, idx) => {
        withPath(ctx, idx, () => {
          validateHexStableId(ctx, id);
        });
      });
    });

    // Value validation
    withPath(ctx, 'value', () => {
      validateFlagValue(ctx, rule.value);
    });
  });
}

function validateFlagInternal(ctx: ValidationContext, flag: Flag, flagIndex: number): void {
  withPath(ctx, flagIndex, () => {
    // Inactive flag with rules warning
    if (!flag.isActive && flag.rules.length > 0) {
      pushIssue(
        ctx,
        'warning',
        'FLAG_INACTIVE_WITH_RULES',
        `Flag "${flag.key}" is inactive but has ${flag.rules.length} rule(s) that will never evaluate`
      );
    }

    // Default value validation
    withPath(ctx, 'defaultValue', () => {
      validateFlagValue(ctx, flag.defaultValue);
    });

    // Flag-level allowlist validation
    withPath(ctx, 'rampUpAllowlist', () => {
      flag.rampUpAllowlist.forEach((id, idx) => {
        withPath(ctx, idx, () => {
          validateHexStableId(ctx, id);
        });
      });
    });

    // Rules validation
    withPath(ctx, 'rules', () => {
      flag.rules.forEach((rule, idx) => {
        validateRule(ctx, rule, idx);
      });

      // Check for duplicate rules
      detectDuplicateRules(ctx, flag.rules);
    });
  });
}

function detectDuplicateRules(ctx: ValidationContext, rules: Rule[]): void {
  // Simplified duplicate detection: rules with identical targeting and value
  const seen = new Map<string, number>();

  rules.forEach((rule, idx) => {
    const signature = computeRuleSignature(rule);
    const previousIdx = seen.get(signature);

    if (previousIdx !== undefined) {
      withPath(ctx, idx, () => {
        pushIssue(
          ctx,
          'warning',
          'DUPLICATE_RULE',
          `Rule is identical to rule at index ${previousIdx}`
        );
      });
    } else {
      seen.set(signature, idx);
    }
  });
}

function computeRuleSignature(rule: Rule): string {
  // Create a deterministic string representation of rule targeting + value
  const targeting = {
    platforms: [...rule.platforms].sort(),
    locales: Array.isArray(rule.locales) 
      ? [...rule.locales].sort() 
      : [rule.locales],
    versionRange: rule.versionRange,
    axes: Object.fromEntries(
      Object.entries(rule.axes)
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([k, v]) => [k, [...v].sort()])
    ),
  };
  return JSON.stringify({ targeting, value: rule.value });
}

// =============================================================================
// Main Validation Entry Point
// =============================================================================

export function validateSnapshot(snapshot: Snapshot): ValidationResult {
  const ctx: ValidationContext = {
    schema: snapshot.schema,
    issues: [],
    currentPath: ['flags'],
  };

  snapshot.flags.forEach((flag, idx) => {
    validateFlagInternal(ctx, flag, idx);
  });

  const errorCount = ctx.issues.filter(i => i.severity === 'error').length;
  const warningCount = ctx.issues.filter(i => i.severity === 'warning').length;

  return {
    valid: errorCount === 0,
    issues: ctx.issues,
    errorCount,
    warningCount,
  };
}

/**
 * Validate a single flag in isolation.
 * Useful for immediate feedback during editing.
 */
export function validateFlag(
  flag: Flag,
  schema?: SchemaMetadata
): ValidationResult {
  const ctx: ValidationContext = {
    schema,
    issues: [],
    currentPath: ['flag'],
  };

  validateFlagInternal(ctx, flag, 0);

  const errorCount = ctx.issues.filter(i => i.severity === 'error').length;
  const warningCount = ctx.issues.filter(i => i.severity === 'warning').length;

  return {
    valid: errorCount === 0,
    issues: ctx.issues,
    errorCount,
    warningCount,
  };
}

// =============================================================================
// Human-Readable Issue Formatting
// =============================================================================

export function formatValidationPath(path: (string | number)[]): string {
  return path
    .map((segment, idx) => {
      if (typeof segment === 'number') {
        return `[${segment}]`;
      }
      return idx === 0 ? segment : `.${segment}`;
    })
    .join('');
}

export function formatIssueForDisplay(issue: ValidationIssue): string {
  const prefix = issue.severity === 'error' ? '✗' : '⚠';
  const path = formatValidationPath(issue.path);
  return `${prefix} ${path}: ${issue.message}`;
}
