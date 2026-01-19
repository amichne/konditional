import { Draft } from 'immer';
import { Snapshot, Flag, Rule, FlagValue, VersionRange, Platform, AppLocale, FeatureId } from './schema';
import { ValidationResult } from './validation';

export interface EditorState {
    /** The current working snapshot (modified by edits) */
    current: Snapshot;
    /** The original snapshot (for diff computation) */
    original: Snapshot;
    /** Validation result for current state */
    validation: ValidationResult;
    /** Set of modified flag keys */
    modifiedFlags: Set<FeatureId>;
    /** Whether any changes exist */
    isDirty: boolean;
}
export interface FlagDiff {
    key: FeatureId;
    type: 'modified';
    before: Flag;
    after: Flag;
    changes: FieldChange[];
}
export interface FieldChange {
    path: string;
    before: unknown;
    after: unknown;
}
export interface SnapshotDiff {
    flags: FlagDiff[];
    hasChanges: boolean;
}
export type EditorAction = {
    type: 'RESET';
    snapshot: Snapshot;
} | {
    type: 'UPDATE_FLAG';
    key: FeatureId;
    updater: (flag: Draft<Flag>) => void;
} | {
    type: 'UPDATE_RULE';
    flagKey: FeatureId;
    ruleIndex: number;
    updater: (rule: Draft<Rule>) => void;
} | {
    type: 'SET_FLAG_ACTIVE';
    key: FeatureId;
    isActive: boolean;
} | {
    type: 'SET_FLAG_SALT';
    key: FeatureId;
    salt: string;
} | {
    type: 'SET_DEFAULT_VALUE';
    key: FeatureId;
    value: FlagValue;
} | {
    type: 'SET_RULE_VALUE';
    flagKey: FeatureId;
    ruleIndex: number;
    value: FlagValue;
} | {
    type: 'SET_RULE_RAMPUP';
    flagKey: FeatureId;
    ruleIndex: number;
    rampUp: number;
} | {
    type: 'SET_RULE_PLATFORMS';
    flagKey: FeatureId;
    ruleIndex: number;
    platforms: Platform[];
} | {
    type: 'SET_RULE_LOCALES';
    flagKey: FeatureId;
    ruleIndex: number;
    locales: AppLocale[];
} | {
    type: 'SET_RULE_VERSION_RANGE';
    flagKey: FeatureId;
    ruleIndex: number;
    versionRange: VersionRange;
} | {
    type: 'SET_RULE_AXES';
    flagKey: FeatureId;
    ruleIndex: number;
    axes: Record<string, string[]>;
} | {
    type: 'SET_RULE_ALLOWLIST';
    flagKey: FeatureId;
    ruleIndex: number;
    allowlist: string[];
} | {
    type: 'SET_RULE_NOTE';
    flagKey: FeatureId;
    ruleIndex: number;
    note: string | undefined;
} | {
    type: 'SET_FLAG_ALLOWLIST';
    key: FeatureId;
    allowlist: string[];
} | {
    type: 'REVERT_FLAG';
    key: FeatureId;
} | {
    type: 'REVERT_ALL';
};
export declare function editorReducer(state: EditorState, action: EditorAction): EditorState;
export declare function createInitialState(snapshot: Snapshot): EditorState;
export declare function computeDiff(state: EditorState): SnapshotDiff;
export declare function selectFlag(state: EditorState, key: FeatureId): Flag | undefined;
export declare function selectOriginalFlag(state: EditorState, key: FeatureId): Flag | undefined;
export declare function selectIsFlagModified(state: EditorState, key: FeatureId): boolean;
export declare function selectFlagValidationIssues(state: EditorState, key: FeatureId): EditorState['validation']['issues'];
export declare function selectCanSave(state: EditorState): boolean;
