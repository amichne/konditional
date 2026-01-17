/**
 * Konditional Editor - Public API
 * 
 * This module exports the primary components and types needed to integrate
 * the Konditional configuration editor into a host application.
 */

// Main component
export { KonditionalEditor, type KonditionalEditorProps } from './components/KonditionalEditor';

// Types for integrators
export type {
  // Schema types
  Snapshot,
  Flag,
  Rule,
  FlagValue,
  FlagValueType,
  BooleanFlag,
  StringFlag,
  IntFlag,
  DoubleFlag,
  EnumFlag,
  DataClassFlag,
  VersionRange,
  Version,
  Platform,
  AppLocale,
  FeatureId,
  SchemaMetadata,
  DataClassSchema,
  SnapshotMetadata,
  Patch,
  generateSchemaFromSnapshot,
} from './types/schema';

// Validation types (for custom validation UI)
export type {
  ValidationResult,
  ValidationIssue,
  ValidationSeverity,
  ValidationCode,
} from './types/validation';

export {
  validateSnapshot,
  formatValidationPath,
  formatIssueForDisplay,
} from './types/validation';

// State types (for advanced integrations)
export type {
  EditorState,
  SnapshotDiff,
  FlagDiff,
  FieldChange,
} from './types/state';

// Hooks (for building custom UIs on top of the state management)
export {
  EditorProvider,
  useEditor,
  useFlag,
  useRule,
  useValidation,
  useDiff,
} from './types/context';

// Utility exports
export {
  parseFeatureId,
  formatFeatureId,
  APP_LOCALES,
  PLATFORMS,
} from './types/schema';

// Styles
import './styles/editor.css';
