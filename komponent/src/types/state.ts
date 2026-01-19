/**
 * Konditional Editor - State Management
 * 
 * Manages editor state with Immer for immutable updates.
 * Tracks original snapshot for diff computation.
 * Provides reducer pattern for predictable state transitions.
 */

import { produce, Draft, enableMapSet } from 'immer';
import type {
  Snapshot,
  Flag,
  Rule,
  FlagValue,
  VersionRange,
  Platform,
  AppLocale,
  FeatureId,
} from './schema';
import { validateSnapshot, ValidationResult } from './validation';

enableMapSet();

// =============================================================================
// Editor State Shape
// =============================================================================

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

// =============================================================================
// Diff Types
// =============================================================================

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

// =============================================================================
// Actions
// =============================================================================

export type EditorAction =
  | { type: 'RESET'; snapshot: Snapshot }
  | { type: 'UPDATE_FLAG'; key: FeatureId; updater: (flag: Draft<Flag>) => void }
  | { type: 'UPDATE_RULE'; flagKey: FeatureId; ruleIndex: number; updater: (rule: Draft<Rule>) => void }
  | { type: 'SET_FLAG_ACTIVE'; key: FeatureId; isActive: boolean }
  | { type: 'SET_FLAG_SALT'; key: FeatureId; salt: string }
  | { type: 'SET_DEFAULT_VALUE'; key: FeatureId; value: FlagValue }
  | { type: 'SET_RULE_VALUE'; flagKey: FeatureId; ruleIndex: number; value: FlagValue }
  | { type: 'SET_RULE_RAMPUP'; flagKey: FeatureId; ruleIndex: number; rampUp: number }
  | { type: 'SET_RULE_PLATFORMS'; flagKey: FeatureId; ruleIndex: number; platforms: Platform[] }
  | { type: 'SET_RULE_LOCALES'; flagKey: FeatureId; ruleIndex: number; locales: AppLocale[] }
  | { type: 'SET_RULE_VERSION_RANGE'; flagKey: FeatureId; ruleIndex: number; versionRange: VersionRange }
  | { type: 'SET_RULE_AXES'; flagKey: FeatureId; ruleIndex: number; axes: Record<string, string[]> }
  | { type: 'SET_RULE_ALLOWLIST'; flagKey: FeatureId; ruleIndex: number; allowlist: string[] }
  | { type: 'SET_RULE_NOTE'; flagKey: FeatureId; ruleIndex: number; note: string | undefined }
  | { type: 'SET_FLAG_ALLOWLIST'; key: FeatureId; allowlist: string[] }
  | { type: 'REVERT_FLAG'; key: FeatureId }
  | { type: 'REVERT_ALL' };

// =============================================================================
// Reducer
// =============================================================================

export function editorReducer(state: EditorState, action: EditorAction): EditorState {
  switch (action.type) {
    case 'RESET': {
      return createInitialState(action.snapshot);
    }

    case 'UPDATE_FLAG': {
      return produceState(state, draft => {
        const flag = draft.current.flags.find(f => f.key === action.key);
        if (flag) {
          action.updater(flag);
          draft.modifiedFlags.add(action.key);
        }
      });
    }

    case 'UPDATE_RULE': {
      return produceState(state, draft => {
        const flag = draft.current.flags.find(f => f.key === action.flagKey);
        if (flag && flag.rules[action.ruleIndex]) {
          action.updater(flag.rules[action.ruleIndex]);
          draft.modifiedFlags.add(action.flagKey);
        }
      });
    }

    case 'SET_FLAG_ACTIVE': {
      return produceState(state, draft => {
        const flag = draft.current.flags.find(f => f.key === action.key);
        if (flag) {
          flag.isActive = action.isActive;
          draft.modifiedFlags.add(action.key);
        }
      });
    }

    case 'SET_FLAG_SALT': {
      return produceState(state, draft => {
        const flag = draft.current.flags.find(f => f.key === action.key);
        if (flag) {
          flag.salt = action.salt;
          draft.modifiedFlags.add(action.key);
        }
      });
    }

    case 'SET_DEFAULT_VALUE': {
      return produceState(state, draft => {
        const flag = draft.current.flags.find(f => f.key === action.key);
        if (flag && flag.type === action.value.type) {
          (flag as any).defaultValue = action.value;
          draft.modifiedFlags.add(action.key);
        }
      });
    }

    case 'SET_RULE_VALUE': {
      return produceState(state, draft => {
        const flag = draft.current.flags.find(f => f.key === action.flagKey);
        const rule = flag?.rules[action.ruleIndex];
        if (rule && rule.value.type === action.value.type) {
          (rule as any).value = action.value;
          draft.modifiedFlags.add(action.flagKey);
        }
      });
    }

    case 'SET_RULE_RAMPUP': {
      return produceState(state, draft => {
        const flag = draft.current.flags.find(f => f.key === action.flagKey);
        const rule = flag?.rules[action.ruleIndex];
        if (rule) {
          rule.rampUp = Math.max(0, Math.min(100, action.rampUp));
          draft.modifiedFlags.add(action.flagKey);
        }
      });
    }

    case 'SET_RULE_PLATFORMS': {
      return produceState(state, draft => {
        const flag = draft.current.flags.find(f => f.key === action.flagKey);
        const rule = flag?.rules[action.ruleIndex];
        if (rule) {
          rule.platforms = action.platforms;
          draft.modifiedFlags.add(action.flagKey);
        }
      });
    }

    case 'SET_RULE_LOCALES': {
      return produceState(state, draft => {
        const flag = draft.current.flags.find(f => f.key === action.flagKey);
        const rule = flag?.rules[action.ruleIndex];
        if (rule) {
          rule.locales = action.locales;
          draft.modifiedFlags.add(action.flagKey);
        }
      });
    }

    case 'SET_RULE_VERSION_RANGE': {
      return produceState(state, draft => {
        const flag = draft.current.flags.find(f => f.key === action.flagKey);
        const rule = flag?.rules[action.ruleIndex];
        if (rule) {
          rule.versionRange = action.versionRange;
          draft.modifiedFlags.add(action.flagKey);
        }
      });
    }

    case 'SET_RULE_AXES': {
      return produceState(state, draft => {
        const flag = draft.current.flags.find(f => f.key === action.flagKey);
        const rule = flag?.rules[action.ruleIndex];
        if (rule) {
          rule.axes = action.axes;
          draft.modifiedFlags.add(action.flagKey);
        }
      });
    }

    case 'SET_RULE_ALLOWLIST': {
      return produceState(state, draft => {
        const flag = draft.current.flags.find(f => f.key === action.flagKey);
        const rule = flag?.rules[action.ruleIndex];
        if (rule) {
          rule.rampUpAllowlist = action.allowlist;
          draft.modifiedFlags.add(action.flagKey);
        }
      });
    }

    case 'SET_RULE_NOTE': {
      return produceState(state, draft => {
        const flag = draft.current.flags.find(f => f.key === action.flagKey);
        const rule = flag?.rules[action.ruleIndex];
        if (rule) {
          rule.note = action.note;
          draft.modifiedFlags.add(action.flagKey);
        }
      });
    }

    case 'SET_FLAG_ALLOWLIST': {
      return produceState(state, draft => {
        const flag = draft.current.flags.find(f => f.key === action.key);
        if (flag) {
          flag.rampUpAllowlist = action.allowlist;
          draft.modifiedFlags.add(action.key);
        }
      });
    }

    case 'REVERT_FLAG': {
      return produceState(state, draft => {
        const originalFlag = state.original.flags.find(f => f.key === action.key);
        const currentIdx = draft.current.flags.findIndex(f => f.key === action.key);
        if (originalFlag && currentIdx !== -1) {
          draft.current.flags[currentIdx] = JSON.parse(JSON.stringify(originalFlag));
          draft.modifiedFlags.delete(action.key);
        }
      });
    }

    case 'REVERT_ALL': {
      return createInitialState(state.original);
    }

    default:
      return state;
  }
}

// =============================================================================
// State Helpers
// =============================================================================

function produceState(
  state: EditorState,
  recipe: (draft: Draft<EditorState>) => void
): EditorState {
  const nextState = produce(state, draft => {
    recipe(draft);
    // Recompute derived state
    draft.isDirty = draft.modifiedFlags.size > 0;
  });

  // Recompute validation (outside Immer for non-draft objects)
  return {
    ...nextState,
    validation: validateSnapshot(nextState.current),
  };
}

export function createInitialState(snapshot: Snapshot): EditorState {
  // Deep clone for original to prevent accidental mutation
  const original = JSON.parse(JSON.stringify(snapshot)) as Snapshot;
  const current = JSON.parse(JSON.stringify(snapshot)) as Snapshot;

  return {
    current,
    original,
    validation: validateSnapshot(current),
    modifiedFlags: new Set(),
    isDirty: false,
  };
}

// =============================================================================
// Diff Computation
// =============================================================================

export function computeDiff(state: EditorState): SnapshotDiff {
  const flagDiffs: FlagDiff[] = [];

  for (const flagKey of state.modifiedFlags) {
    const before = state.original.flags.find(f => f.key === flagKey);
    const after = state.current.flags.find(f => f.key === flagKey);

    if (before && after) {
      const changes = computeFlagChanges(before, after);
      if (changes.length > 0) {
        flagDiffs.push({
          key: flagKey,
          type: 'modified',
          before,
          after,
          changes,
        });
      }
    }
  }

  return {
    flags: flagDiffs,
    hasChanges: flagDiffs.length > 0,
  };
}

function computeFlagChanges(before: Flag, after: Flag): FieldChange[] {
  const changes: FieldChange[] = [];

  // Top-level flag properties
  if (before.isActive !== after.isActive) {
    changes.push({ path: 'isActive', before: before.isActive, after: after.isActive });
  }
  if (before.salt !== after.salt) {
    changes.push({ path: 'salt', before: before.salt, after: after.salt });
  }
  if (!deepEqual(before.defaultValue, after.defaultValue)) {
    changes.push({ path: 'defaultValue', before: before.defaultValue, after: after.defaultValue });
  }
  if (!deepEqual(before.rampUpAllowlist, after.rampUpAllowlist)) {
    changes.push({ path: 'rampUpAllowlist', before: before.rampUpAllowlist, after: after.rampUpAllowlist });
  }

  // Rule-level changes
  const maxRules = Math.max(before.rules.length, after.rules.length);
  for (let i = 0; i < maxRules; i++) {
    const beforeRule = before.rules[i];
    const afterRule = after.rules[i];

    if (!beforeRule && afterRule) {
      changes.push({ path: `rules[${i}]`, before: undefined, after: afterRule });
    } else if (beforeRule && !afterRule) {
      changes.push({ path: `rules[${i}]`, before: beforeRule, after: undefined });
    } else if (beforeRule && afterRule && !deepEqual(beforeRule, afterRule)) {
      // Detailed rule diff
      if (!deepEqual(beforeRule.value, afterRule.value)) {
        changes.push({ path: `rules[${i}].value`, before: beforeRule.value, after: afterRule.value });
      }
      if (beforeRule.rampUp !== afterRule.rampUp) {
        changes.push({ path: `rules[${i}].rampUp`, before: beforeRule.rampUp, after: afterRule.rampUp });
      }
      if (!deepEqual(beforeRule.platforms, afterRule.platforms)) {
        changes.push({ path: `rules[${i}].platforms`, before: beforeRule.platforms, after: afterRule.platforms });
      }
      if (!deepEqual(beforeRule.locales, afterRule.locales)) {
        changes.push({ path: `rules[${i}].locales`, before: beforeRule.locales, after: afterRule.locales });
      }
      if (!deepEqual(beforeRule.versionRange, afterRule.versionRange)) {
        changes.push({ path: `rules[${i}].versionRange`, before: beforeRule.versionRange, after: afterRule.versionRange });
      }
      if (!deepEqual(beforeRule.axes, afterRule.axes)) {
        changes.push({ path: `rules[${i}].axes`, before: beforeRule.axes, after: afterRule.axes });
      }
      if (!deepEqual(beforeRule.rampUpAllowlist, afterRule.rampUpAllowlist)) {
        changes.push({ path: `rules[${i}].rampUpAllowlist`, before: beforeRule.rampUpAllowlist, after: afterRule.rampUpAllowlist });
      }
      if (beforeRule.note !== afterRule.note) {
        changes.push({ path: `rules[${i}].note`, before: beforeRule.note, after: afterRule.note });
      }
    }
  }

  return changes;
}

function deepEqual(a: unknown, b: unknown): boolean {
  return JSON.stringify(a) === JSON.stringify(b);
}

// =============================================================================
// Selectors
// =============================================================================

export function selectFlag(state: EditorState, key: FeatureId): Flag | undefined {
  return state.current.flags.find(f => f.key === key);
}

export function selectOriginalFlag(state: EditorState, key: FeatureId): Flag | undefined {
  return state.original.flags.find(f => f.key === key);
}

export function selectIsFlagModified(state: EditorState, key: FeatureId): boolean {
  return state.modifiedFlags.has(key);
}

export function selectFlagValidationIssues(
  state: EditorState,
  key: FeatureId
): EditorState['validation']['issues'] {
  const flagIndex = state.current.flags.findIndex(f => f.key === key);
  if (flagIndex === -1) return [];

  return state.validation.issues.filter(issue => {
    // Issues that start with ['flags', flagIndex, ...]
    return (
      issue.path[0] === 'flags' &&
      issue.path[1] === flagIndex
    );
  });
}

export function selectCanSave(state: EditorState): boolean {
  return state.isDirty && state.validation.valid;
}
