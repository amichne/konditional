import { Flag, SchemaMetadata, Snapshot } from './schema';

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
export type ValidationCode = 'EMPTY_PLATFORM_SET' | 'EMPTY_LOCALE_SET' | 'VERSION_MIN_EXCEEDS_MAX' | 'UNKNOWN_ENUM_VALUE' | 'DATACLASS_MISSING_REQUIRED_FIELD' | 'DATACLASS_FIELD_TYPE_MISMATCH' | 'DATACLASS_UNKNOWN_CLASS' | 'ENUM_UNKNOWN_CLASS' | 'RAMPUP_OUT_OF_BOUNDS' | 'INVALID_HEX_STABLE_ID' | 'RAMPUP_ZERO_NO_ALLOWLIST' | 'DATACLASS_UNKNOWN_FIELD' | 'DUPLICATE_RULE' | 'SHADOWED_RULE' | 'FLAG_INACTIVE_WITH_RULES';
export declare function validateSnapshot(snapshot: Snapshot): ValidationResult;
/**
 * Validate a single flag in isolation.
 * Useful for immediate feedback during editing.
 */
export declare function validateFlag(flag: Flag, schema?: SchemaMetadata): ValidationResult;
export declare function formatValidationPath(path: (string | number)[]): string;
export declare function formatIssueForDisplay(issue: ValidationIssue): string;
