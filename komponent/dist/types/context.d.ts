import { ReactNode } from 'react';
import { Snapshot, FlagValue, VersionRange, Platform, AppLocale, FeatureId, SchemaMetadata } from './schema';
import { SnapshotDiff } from './state';

interface EditorProviderProps {
    snapshot: Snapshot;
    children: ReactNode;
}
export declare function EditorProvider({ snapshot, children }: EditorProviderProps): JSX.Element;
/**
 * Access the full editor state and dispatch.
 * Use more specific hooks when possible for better encapsulation.
 */
export declare function useEditor(): {
    snapshot: Snapshot;
    originalSnapshot: Snapshot;
    flags: import('./schema').Flag[];
    validation: import('./validation').ValidationResult;
    isDirty: boolean;
    canSave: boolean;
    diff: SnapshotDiff;
    schema: SchemaMetadata | undefined;
    reset: (snapshot: Snapshot) => void;
    revertAll: () => void;
};
/**
 * Hook for working with a single flag.
 * Provides flag-specific state and update functions.
 */
export declare function useFlag(key: FeatureId): {
    flag: import('./schema').Flag | undefined;
    originalFlag: import('./schema').Flag | undefined;
    isModified: boolean;
    validationIssues: import('./validation').ValidationIssue[];
    hasErrors: boolean;
    hasWarnings: boolean;
    schema: SchemaMetadata | undefined;
    setActive: (isActive: boolean) => void;
    setSalt: (salt: string) => void;
    setDefaultValue: (value: FlagValue) => void;
    setAllowlist: (allowlist: string[]) => void;
    revert: () => void;
};
/**
 * Hook for working with a single rule within a flag.
 */
export declare function useRule(flagKey: FeatureId, ruleIndex: number): {
    rule: import('./schema').BooleanRule | import('./schema').StringRule | import('./schema').IntRule | import('./schema').DoubleRule | import('./schema').EnumRule | import('./schema').DataClassRule | undefined;
    flagType: "BOOLEAN" | "STRING" | "INT" | "DOUBLE" | "ENUM" | "DATA_CLASS" | undefined;
    validationIssues: import('./validation').ValidationIssue[];
    hasErrors: boolean;
    schema: SchemaMetadata | undefined;
    setValue: (value: FlagValue) => void;
    setRampUp: (rampUp: number) => void;
    setPlatforms: (platforms: Platform[]) => void;
    setLocales: (locales: AppLocale[]) => void;
    setVersionRange: (versionRange: VersionRange) => void;
    setAxes: (axes: Record<string, string[]>) => void;
    setAllowlist: (allowlist: string[]) => void;
    setNote: (note: string | undefined) => void;
};
/**
 * Hook for validation state only.
 */
export declare function useValidation(): {
    validation: import('./validation').ValidationResult;
    isValid: boolean;
    errorCount: number;
    warningCount: number;
    issues: import('./validation').ValidationIssue[];
};
/**
 * Hook for diff computation.
 */
export declare function useDiff(): SnapshotDiff;
export {};
