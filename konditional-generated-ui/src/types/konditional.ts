/**
 * Konditional Configuration Types
 * Generated from OpenAPI schema for feature flags and configuration management
 */

// ============ Core Identifiers ============

/** Encoded feature identifier: feature::<namespace>::<key> */
export type FeatureId = string;

// ============ Version Types ============

/** Semantic version representation */
export interface Version {
  major: number;
  minor: number;
  patch: number;
}

/** Version range types for targeting */
export type VersionRangeType = 'UNBOUNDED' | 'MIN_BOUND' | 'MAX_BOUND' | 'MIN_AND_MAX_BOUND';

export interface UnboundedVersionRange {
  type: 'UNBOUNDED';
}

export interface MinBoundVersionRange {
  type: 'MIN_BOUND';
  min: Version;
}

export interface MaxBoundVersionRange {
  type: 'MAX_BOUND';
  max: Version;
}

export interface MinAndMaxBoundVersionRange {
  type: 'MIN_AND_MAX_BOUND';
  min: Version;
  max: Version;
}

export type VersionRange =
  | UnboundedVersionRange
  | MinBoundVersionRange
  | MaxBoundVersionRange
  | MinAndMaxBoundVersionRange;

// ============ Flag Value Types ============

export type FlagValueType = 'BOOLEAN' | 'STRING' | 'INT' | 'DOUBLE' | 'ENUM' | 'DATA_CLASS';

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

// ============ Targeting Constants ============

export const ALL_LOCALES = [
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

export type Locale = typeof ALL_LOCALES[number];

export const ALL_PLATFORMS = ['IOS', 'ANDROID', 'WEB'] as const;
export type Platform = typeof ALL_PLATFORMS[number];

// ============ Rule Types ============

/** Targeting axes - custom key-value targeting dimensions */
export type Axes = Record<string, string[]>;

/** A rule that can override the default flag value based on targeting criteria */
export interface SerializableRule<T extends FlagValue = FlagValue> {
  /** The value to use when this rule matches */
  value: T;
  /** Percentage of users to ramp up to (0-100) */
  rampUp: number;
  /** List of user IDs to always include in ramp-up */
  rampUpAllowlist: string[];
  /** Optional note/description for this rule */
  note?: string;
  /** Locales this rule applies to */
  locales: Locale[];
  /** Platforms this rule applies to */
  platforms: Platform[];
  /** App version range this rule applies to */
  versionRange: VersionRange;
  /** Custom axes for additional targeting */
  axes: Axes;
}

/** Untyped rule for UI components that work with any flag type */
export interface AnySerializableRule {
  value: FlagValue;
  rampUp: number;
  rampUpAllowlist: string[];
  note?: string;
  locales: Locale[];
  platforms: Platform[];
  versionRange: VersionRange;
  axes: Axes;
}

// ============ Flag Types ============

/** Base flag structure shared by all flag types */
export interface SerializableFlagBase<T extends FlagValue = FlagValue> {
  /** Encoded feature identifier */
  key: FeatureId;
  /** Default value when no rules match */
  defaultValue: T;
  /** Salt for consistent hashing (default: 'v1') */
  salt: string;
  /** Whether this flag is active */
  isActive: boolean;
  /** Global ramp-up allowlist */
  rampUpAllowlist: string[];
  /** Rules that can override the default value */
  rules: SerializableRule<T>[];
}

export interface SerializableBooleanFlag extends SerializableFlagBase<BooleanFlagValue> {}
export interface SerializableStringFlag extends SerializableFlagBase<StringFlagValue> {}
export interface SerializableIntFlag extends SerializableFlagBase<IntFlagValue> {}
export interface SerializableDoubleFlag extends SerializableFlagBase<DoubleFlagValue> {}
export interface SerializableEnumFlag extends SerializableFlagBase<EnumFlagValue> {}
export interface SerializableDataClassFlag extends SerializableFlagBase<DataClassFlagValue> {}

export type SerializableFlag =
  | SerializableBooleanFlag
  | SerializableStringFlag
  | SerializableIntFlag
  | SerializableDoubleFlag
  | SerializableEnumFlag
  | SerializableDataClassFlag;

/** Untyped flag for UI components that work with any flag type */
export interface AnySerializableFlag {
  key: FeatureId;
  defaultValue: FlagValue;
  salt: string;
  isActive: boolean;
  rampUpAllowlist: string[];
  rules: AnySerializableRule[];
}

// ============ Snapshot & Patch Types ============

export interface SnapshotMeta {
  version: string;
  generatedAtEpochMillis: number;
  source?: string;
}

export interface SerializableSnapshot {
  meta?: SnapshotMeta;
  flags: SerializableFlag[];
}

export interface SerializablePatch {
  meta?: SnapshotMeta;
  flags: SerializableFlag[];
  removeKeys: FeatureId[];
}

// ============ UI Helper Types ============

/** Human-readable labels for locales */
export const LOCALE_LABELS: Record<Locale, string> = {
  AUSTRALIA: 'Australia',
  AUSTRIA: 'Austria',
  BELGIUM_DUTCH: 'Belgium (Dutch)',
  BELGIUM_FRENCH: 'Belgium (French)',
  CANADA: 'Canada',
  CANADA_FRENCH: 'Canada (French)',
  FINLAND: 'Finland',
  FRANCE: 'France',
  GERMANY: 'Germany',
  HONG_KONG: 'Hong Kong',
  HONG_KONG_ENGLISH: 'Hong Kong (English)',
  INDIA: 'India',
  ITALY: 'Italy',
  JAPAN: 'Japan',
  MEXICO: 'Mexico',
  NETHERLANDS: 'Netherlands',
  NEW_ZEALAND: 'New Zealand',
  NORWAY: 'Norway',
  SINGAPORE: 'Singapore',
  SPAIN: 'Spain',
  SWEDEN: 'Sweden',
  TAIWAN: 'Taiwan',
  UNITED_KINGDOM: 'United Kingdom',
  UNITED_STATES: 'United States',
  ICC_EN_EU: 'ICC (EU English)',
  ICC_EN_EI: 'ICC (IE English)',
};

export const PLATFORM_LABELS: Record<Platform, string> = {
  IOS: 'iOS',
  ANDROID: 'Android',
  WEB: 'Web',
};

/** Get the display type name for a flag value type */
export function getFlagValueTypeLabel(type: FlagValueType): string {
  switch (type) {
    case 'BOOLEAN': return 'Boolean';
    case 'STRING': return 'String';
    case 'INT': return 'Integer';
    case 'DOUBLE': return 'Decimal';
    case 'ENUM': return 'Enum';
    case 'DATA_CLASS': return 'Object';
  }
}

/** Create default rule with all targeting enabled */
export function createDefaultRule<T extends FlagValue>(value: T): SerializableRule<T> {
  return {
    value,
    rampUp: 100,
    rampUpAllowlist: [],
    locales: [...ALL_LOCALES],
    platforms: [...ALL_PLATFORMS],
    versionRange: { type: 'UNBOUNDED' },
    axes: {},
  };
}

/** Create a default flag value based on type */
export function createDefaultFlagValue(type: FlagValueType): FlagValue {
  switch (type) {
    case 'BOOLEAN':
      return { type: 'BOOLEAN', value: false };
    case 'STRING':
      return { type: 'STRING', value: '' };
    case 'INT':
      return { type: 'INT', value: 0 };
    case 'DOUBLE':
      return { type: 'DOUBLE', value: 0.0 };
    case 'ENUM':
      return { type: 'ENUM', value: '', enumClassName: '' };
    case 'DATA_CLASS':
      return { type: 'DATA_CLASS', dataClassName: '', value: {} };
  }
}

/** Check if all locales are selected */
export function hasAllLocales(locales: Locale[]): boolean {
  return locales.length === ALL_LOCALES.length;
}

/** Check if all platforms are selected */
export function hasAllPlatforms(platforms: Platform[]): boolean {
  return platforms.length === ALL_PLATFORMS.length;
}

/** Format version for display */
export function formatVersion(v: Version): string {
  return `${v.major}.${v.minor}.${v.patch}`;
}

/** Format version range for display */
export function formatVersionRange(vr: VersionRange): string {
  switch (vr.type) {
    case 'UNBOUNDED':
      return 'All versions';
    case 'MIN_BOUND':
      return `≥ ${formatVersion(vr.min)}`;
    case 'MAX_BOUND':
      return `≤ ${formatVersion(vr.max)}`;
    case 'MIN_AND_MAX_BOUND':
      return `${formatVersion(vr.min)} – ${formatVersion(vr.max)}`;
  }
}

/** Parse a feature ID into namespace and key */
export function parseFeatureId(id: FeatureId): { namespace: string; key: string } | null {
  const match = id.match(/^feature::([^:]+)::(.+)$/);
  if (!match) return null;
  return { namespace: match[1], key: match[2] };
}

/** Create a feature ID from namespace and key */
export function createFeatureId(namespace: string, key: string): FeatureId {
  return `feature::${namespace}::${key}`;
}
