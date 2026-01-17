/**
 * Konditional Editor - Core Types
 * 
 * These types are mechanically derived from the OpenAPI schema (konditional-api.yaml).
 * They represent the wire format for Snapshot, Flag, Rule, and FlagValue structures.
 * 
 * INVARIANT: This file must stay in sync with the backend schema.
 * Any schema changes require regenerating these types.
 */

// =============================================================================
// Primitive Value Types
// =============================================================================

/** Semantic version representation */
export interface Version {
  major: number;
  minor: number;
  patch: number;
}

/** Encoded feature identifier: feature::(namespace)::(key) */
export type FeatureId = string;

// =============================================================================
// Version Range (discriminated union)
// =============================================================================

export interface VersionRangeUnbounded {
  type: 'UNBOUNDED';
}

export interface VersionRangeMinBound {
  type: 'MIN_BOUND';
  min: Version;
}

export interface VersionRangeMaxBound {
  type: 'MAX_BOUND';
  max: Version;
}

export interface VersionRangeMinAndMaxBound {
  type: 'MIN_AND_MAX_BOUND';
  min: Version;
  max: Version;
}

export type VersionRange =
  | VersionRangeUnbounded
  | VersionRangeMinBound
  | VersionRangeMaxBound
  | VersionRangeMinAndMaxBound;

// =============================================================================
// Flag Values (discriminated union by type)
// =============================================================================

export interface BooleanFlagValue {
  type: 'BOOLEAN';
  value: boolean;
}

export interface StringFlagValue {
  type: 'STRING';
  value: string;
}

export interface IntFlagValue {
  type: 'INT';
  value: number;
}

export interface DoubleFlagValue {
  type: 'DOUBLE';
  value: number;
}

export interface EnumFlagValue {
  type: 'ENUM';
  value: string;
  enumClassName: string;
}

export interface DataClassFlagValue {
  type: 'DATA_CLASS';
  dataClassName: string;
  value: Record<string, unknown>;
}

export type FlagValue =
  | BooleanFlagValue
  | StringFlagValue
  | IntFlagValue
  | DoubleFlagValue
  | EnumFlagValue
  | DataClassFlagValue;

export type FlagValueType = FlagValue['type'];

// =============================================================================
// Rules (type-specific, but structurally identical except for value shape)
// =============================================================================

/**
 * Base rule structure shared by all rule types.
 * The `value` field is typed per-flag-type to ensure type consistency.
 */
export interface BaseRule<V extends FlagValue> {
  value: V;
  rampUp: number;
  rampUpAllowlist: string[];
  note?: string;
  locales: AppLocale | AppLocale[];
  platforms: Platform[];
  versionRange: VersionRange;
  axes: Record<string, string[]>;
}

export type BooleanRule = BaseRule<BooleanFlagValue>;
export type StringRule = BaseRule<StringFlagValue>;
export type IntRule = BaseRule<IntFlagValue>;
export type DoubleRule = BaseRule<DoubleFlagValue>;
export type EnumRule = BaseRule<EnumFlagValue>;
export type DataClassRule = BaseRule<DataClassFlagValue>;

export type Rule = BaseRule<FlagValue>;

// =============================================================================
// Flags (discriminated union by type)
// =============================================================================

interface BaseFlag<V extends FlagValue, R extends BaseRule<V>> {
  type: V['type'];
  key: FeatureId;
  defaultValue: V;
  salt: string;
  isActive: boolean;
  rampUpAllowlist: string[];
  rules: R[];
}

export interface BooleanFlag extends BaseFlag<BooleanFlagValue, BooleanRule> {
  type: 'BOOLEAN';
}

export interface StringFlag extends BaseFlag<StringFlagValue, StringRule> {
  type: 'STRING';
}

export interface IntFlag extends BaseFlag<IntFlagValue, IntRule> {
  type: 'INT';
}

export interface DoubleFlag extends BaseFlag<DoubleFlagValue, DoubleRule> {
  type: 'DOUBLE';
}

export interface EnumFlag extends BaseFlag<EnumFlagValue, EnumRule> {
  type: 'ENUM';
}

export interface DataClassFlag extends BaseFlag<DataClassFlagValue, DataClassRule> {
  type: 'DATA_CLASS';
}

export type Flag =
  | BooleanFlag
  | StringFlag
  | IntFlag
  | DoubleFlag
  | EnumFlag
  | DataClassFlag;

// =============================================================================
// Snapshot Metadata
// =============================================================================

export interface SnapshotMetadata {
  version?: string;
  generatedAtEpochMillis?: number;
  source?: string;
}

// =============================================================================
// Schema Metadata (embedded enum/dataclass definitions)
// =============================================================================

/**
 * JSON Schema subset for DataClass field definitions.
 * Supports the types Konditional serializes: primitives, nested objects, arrays.
 */
export interface DataClassFieldSchema {
  type: 'string' | 'integer' | 'number' | 'boolean' | 'object' | 'array';
  minimum?: number;
  maximum?: number;
  minLength?: number;
  maxLength?: number;
  pattern?: string;
  enum?: string[];
  items?: DataClassFieldSchema;
  properties?: Record<string, DataClassFieldSchema>;
  required?: string[];
  additionalProperties?: boolean | DataClassFieldSchema;
}

export interface DataClassSchema {
  type: 'object';
  properties: Record<string, DataClassFieldSchema>;
  required: string[];
}

export interface SchemaMetadata {
  /** Map from fully-qualified enum class name to valid enum values */
  enums: Record<string, string[]>;
  /** Map from fully-qualified data class name to JSON Schema */
  dataClasses: Record<string, DataClassSchema>;
}

// =============================================================================
// Schema Generation (from snapshot instances)
// =============================================================================

interface DataClassAccumulator {
  properties: Record<string, DataClassFieldSchema>;
  fieldCounts: Map<string, number>;
  stringEnums: Map<string, Set<string>>;
  sampleCount: number;
}

export function generateSchemaFromSnapshot(snapshot: Snapshot): SchemaMetadata {
  const enums: Record<string, Set<string>> = {};
  const dataClasses = new Map<string, DataClassAccumulator>();

  const registerEnumValue = (className: string, value: string) => {
    if (!enums[className]) {
      enums[className] = new Set<string>();
    }
    enums[className].add(value);
  };

  const registerDataClassValue = (className: string, value: Record<string, unknown>) => {
    const existing = dataClasses.get(className) ?? {
      properties: {},
      fieldCounts: new Map<string, number>(),
      stringEnums: new Map<string, Set<string>>(),
      sampleCount: 0,
    };

    existing.sampleCount += 1;

    for (const [fieldName, fieldValue] of Object.entries(value)) {
      const fieldSchema = inferFieldSchema(fieldValue);
      existing.properties[fieldName] = mergeFieldSchema(
        existing.properties[fieldName],
        fieldSchema
      );
      existing.fieldCounts.set(
        fieldName,
        (existing.fieldCounts.get(fieldName) ?? 0) + 1
      );
      if (typeof fieldValue === 'string') {
        const enumValues = existing.stringEnums.get(fieldName) ?? new Set<string>();
        enumValues.add(fieldValue);
        existing.stringEnums.set(fieldName, enumValues);
      }
    }

    dataClasses.set(className, existing);
  };

  for (const flag of snapshot.flags) {
    if (flag.defaultValue.type === 'ENUM') {
      registerEnumValue(flag.defaultValue.enumClassName, flag.defaultValue.value);
    }
    if (flag.defaultValue.type === 'DATA_CLASS') {
      registerDataClassValue(flag.defaultValue.dataClassName, flag.defaultValue.value);
    }

    for (const rule of flag.rules) {
      if (rule.value.type === 'ENUM') {
        registerEnumValue(rule.value.enumClassName, rule.value.value);
      }
      if (rule.value.type === 'DATA_CLASS') {
        registerDataClassValue(rule.value.dataClassName, rule.value.value);
      }
    }
  }

  return {
    enums: Object.fromEntries(
      Object.entries(enums).map(([className, values]) => [
        className,
        Array.from(values).sort(),
      ])
    ),
    dataClasses: Object.fromEntries(
      Array.from(dataClasses.entries()).map(([className, accumulator]) => {
        const properties = Object.fromEntries(
          Object.entries(accumulator.properties).map(([fieldName, schema]) => {
            if (schema.type === 'string') {
              const enumValues = accumulator.stringEnums.get(fieldName);
              if (enumValues && enumValues.size > 0) {
                return [
                  fieldName,
                  {
                    ...schema,
                    enum: Array.from(enumValues).sort(),
                  },
                ];
              }
            }
            return [fieldName, schema];
          })
        );

        return [
          className,
          {
            type: 'object',
            properties,
            required: Array.from(accumulator.fieldCounts.entries())
              .filter(([, count]) => count === accumulator.sampleCount)
              .map(([fieldName]) => fieldName)
              .sort(),
          },
        ];
      })
    ),
  };
}

function inferFieldSchema(value: unknown): DataClassFieldSchema {
  if (Array.isArray(value)) {
    return {
      type: 'array',
      items: value.length > 0 ? inferFieldSchema(value[0]) : { type: 'string' },
    };
  }

  if (value && typeof value === 'object') {
    const entries = Object.entries(value as Record<string, unknown>);
    const properties = Object.fromEntries(
      entries.map(([key, fieldValue]) => [key, inferFieldSchema(fieldValue)])
    );

    return {
      type: 'object',
      properties,
      required: entries.map(([key]) => key).sort(),
    };
  }

  if (typeof value === 'number') {
    return { type: Number.isInteger(value) ? 'integer' : 'number' };
  }

  if (typeof value === 'boolean') {
    return { type: 'boolean' };
  }

  return { type: 'string' };
}

function mergeFieldSchema(
  existing: DataClassFieldSchema | undefined,
  incoming: DataClassFieldSchema
): DataClassFieldSchema {
  if (!existing) return incoming;
  if (existing.type === incoming.type) return existing;
  if (
    (existing.type === 'integer' && incoming.type === 'number') ||
    (existing.type === 'number' && incoming.type === 'integer')
  ) {
    return { type: 'number' };
  }
  return { type: 'object' };
}

// =============================================================================
// Snapshot (top-level container)
// =============================================================================

export interface Snapshot {
  meta?: SnapshotMetadata;
  flags: Flag[];
  schema?: SchemaMetadata;
}

// =============================================================================
// Patch (incremental update)
// =============================================================================

export interface Patch {
  meta?: SnapshotMetadata;
  flags: Flag[];
  removeKeys: FeatureId[];
}

// =============================================================================
// Enums (from schema)
// =============================================================================

/**
 * Supported locales.
 * These values come from the OpenAPI schema and must match the backend.
 */
export type AppLocale =
  | 'AUSTRALIA'
  | 'AUSTRIA'
  | 'BELGIUM_DUTCH'
  | 'BELGIUM_FRENCH'
  | 'CANADA'
  | 'CANADA_FRENCH'
  | 'FINLAND'
  | 'FRANCE'
  | 'GERMANY'
  | 'HONG_KONG'
  | 'HONG_KONG_ENGLISH'
  | 'INDIA'
  | 'ITALY'
  | 'JAPAN'
  | 'MEXICO'
  | 'NETHERLANDS'
  | 'NEW_ZEALAND'
  | 'NORWAY'
  | 'SINGAPORE'
  | 'SPAIN'
  | 'SWEDEN'
  | 'TAIWAN'
  | 'UNITED_KINGDOM'
  | 'UNITED_STATES'
  | 'ICC_EN_EU'
  | 'ICC_EN_EI';

export const APP_LOCALES: readonly AppLocale[] = [
  'AUSTRALIA',
  'AUSTRIA',
  'BELGIUM_DUTCH',
  'BELGIUM_FRENCH',
  'CANADA',
  'CANADA_FRENCH',
  'FINLAND',
  'FRANCE',
  'GERMANY',
  'HONG_KONG',
  'HONG_KONG_ENGLISH',
  'INDIA',
  'ITALY',
  'JAPAN',
  'MEXICO',
  'NETHERLANDS',
  'NEW_ZEALAND',
  'NORWAY',
  'SINGAPORE',
  'SPAIN',
  'SWEDEN',
  'TAIWAN',
  'UNITED_KINGDOM',
  'UNITED_STATES',
  'ICC_EN_EU',
  'ICC_EN_EI',
] as const;

/**
 * Supported platforms.
 */
export type Platform = 'IOS' | 'ANDROID' | 'WEB';

export const PLATFORMS: readonly Platform[] = ['IOS', 'ANDROID', 'WEB'] as const;

// =============================================================================
// Type Guards
// =============================================================================

export function isBooleanFlag(flag: Flag): flag is BooleanFlag {
  return flag.type === 'BOOLEAN';
}

export function isStringFlag(flag: Flag): flag is StringFlag {
  return flag.type === 'STRING';
}

export function isIntFlag(flag: Flag): flag is IntFlag {
  return flag.type === 'INT';
}

export function isDoubleFlag(flag: Flag): flag is DoubleFlag {
  return flag.type === 'DOUBLE';
}

export function isEnumFlag(flag: Flag): flag is EnumFlag {
  return flag.type === 'ENUM';
}

export function isDataClassFlag(flag: Flag): flag is DataClassFlag {
  return flag.type === 'DATA_CLASS';
}

// =============================================================================
// Utility Types
// =============================================================================

/** Extract the value type from a flag type */
export type FlagValueOf<F extends Flag> = F['defaultValue'];

/** Extract the rule type from a flag type */
export type RuleOf<F extends Flag> = F['rules'][number];

/** Parse a FeatureId into its components */
export interface ParsedFeatureId {
  prefix: 'feature';
  namespace: string;
  key: string;
}

export function parseFeatureId(id: FeatureId): ParsedFeatureId | null {
  const parts = id.split('::');
  if (parts.length !== 3 || parts[0] !== 'feature') {
    return null;
  }
  return {
    prefix: 'feature',
    namespace: parts[1],
    key: parts[2],
  };
}

export function formatFeatureId(namespace: string, key: string): FeatureId {
  return `feature::${namespace}::${key}`;
}
