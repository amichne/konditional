/**
 * Konditional Editor - Public API
 *
 * This module exports the primary components and types needed to integrate
 * the Konditional configuration editor into a host application.
 */
export { KonditionalEditor, type KonditionalEditorProps } from './components/KonditionalEditor';
export type { Snapshot, Flag, Rule, FlagValue, FlagValueType, BooleanFlag, StringFlag, IntFlag, DoubleFlag, EnumFlag, DataClassFlag, VersionRange, Version, Platform, AppLocale, FeatureId, SchemaMetadata, DataClassSchema, SnapshotMetadata, Patch, generateSchemaFromSnapshot, } from './types/schema';
export type { ValidationResult, ValidationIssue, ValidationSeverity, ValidationCode, } from './types/validation';
export { validateSnapshot, formatValidationPath, formatIssueForDisplay, } from './types/validation';
export type { EditorState, SnapshotDiff, FlagDiff, FieldChange, } from './types/state';
export { EditorProvider, useEditor, useFlag, useRule, useValidation, useDiff, } from './types/context';
export { parseFeatureId, formatFeatureId, APP_LOCALES, PLATFORMS, } from './types/schema';
