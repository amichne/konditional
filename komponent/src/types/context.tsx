/**
 * Konditional Editor - React Context & Hooks
 * 
 * Provides the editor state and dispatch function to all child components.
 * Encapsulates the reducer pattern behind a clean hook API.
 */

import {
  createContext,
  useContext,
  useReducer,
  useCallback,
  useMemo,
  type ReactNode,
  type Dispatch,
} from 'react';

import type {
  Snapshot,
  FlagValue,
  VersionRange,
  Platform,
  AppLocale,
  FeatureId,
  SchemaMetadata,
} from './schema';

import {
  EditorState,
  EditorAction,
  editorReducer,
  createInitialState,
  computeDiff,
  selectFlag,
  selectOriginalFlag,
  selectIsFlagModified,
  selectFlagValidationIssues,
  selectCanSave,
  SnapshotDiff,
} from './state';

// =============================================================================
// Context Definition
// =============================================================================

interface EditorContextValue {
  state: EditorState;
  dispatch: Dispatch<EditorAction>;
  schema: SchemaMetadata | undefined;
}

const EditorContext = createContext<EditorContextValue | null>(null);

// =============================================================================
// Provider Component
// =============================================================================

interface EditorProviderProps {
  snapshot: Snapshot;
  children: ReactNode;
}

export function EditorProvider({ snapshot, children }: EditorProviderProps): JSX.Element {
  const [state, dispatch] = useReducer(editorReducer, snapshot, createInitialState);

  const value = useMemo(
    () => ({
      state,
      dispatch,
      schema: state.current.schema,
    }),
    [state]
  );

  return (
    <EditorContext.Provider value={value}>
      {children}
    </EditorContext.Provider>
  );
}

// =============================================================================
// Base Hook
// =============================================================================

function useEditorContext(): EditorContextValue {
  const context = useContext(EditorContext);
  if (!context) {
    throw new Error('useEditorContext must be used within an EditorProvider');
  }
  return context;
}

// =============================================================================
// Specialized Hooks
// =============================================================================

/**
 * Access the full editor state and dispatch.
 * Use more specific hooks when possible for better encapsulation.
 */
export function useEditor() {
  const { state, dispatch, schema } = useEditorContext();

  const reset = useCallback(
    (snapshot: Snapshot) => dispatch({ type: 'RESET', snapshot }),
    [dispatch]
  );

  const revertAll = useCallback(
    () => dispatch({ type: 'REVERT_ALL' }),
    [dispatch]
  );

  const diff = useMemo(() => computeDiff(state), [state]);

  return {
    snapshot: state.current,
    originalSnapshot: state.original,
    flags: state.current.flags,
    validation: state.validation,
    isDirty: state.isDirty,
    canSave: selectCanSave(state),
    diff,
    schema,
    reset,
    revertAll,
  };
}

/**
 * Hook for working with a single flag.
 * Provides flag-specific state and update functions.
 */
export function useFlag(key: FeatureId) {
  const { state, dispatch, schema } = useEditorContext();

  const flag = useMemo(() => selectFlag(state, key), [state, key]);
  const originalFlag = useMemo(() => selectOriginalFlag(state, key), [state, key]);
  const isModified = useMemo(() => selectIsFlagModified(state, key), [state, key]);
  const validationIssues = useMemo(
    () => selectFlagValidationIssues(state, key),
    [state, key]
  );

  const setActive = useCallback(
    (isActive: boolean) => dispatch({ type: 'SET_FLAG_ACTIVE', key, isActive }),
    [dispatch, key]
  );

  const setSalt = useCallback(
    (salt: string) => dispatch({ type: 'SET_FLAG_SALT', key, salt }),
    [dispatch, key]
  );

  const setDefaultValue = useCallback(
    (value: FlagValue) => dispatch({ type: 'SET_DEFAULT_VALUE', key, value }),
    [dispatch, key]
  );

  const setAllowlist = useCallback(
    (allowlist: string[]) => dispatch({ type: 'SET_FLAG_ALLOWLIST', key, allowlist }),
    [dispatch, key]
  );

  const revert = useCallback(
    () => dispatch({ type: 'REVERT_FLAG', key }),
    [dispatch, key]
  );

  return {
    flag,
    originalFlag,
    isModified,
    validationIssues,
    hasErrors: validationIssues.some(i => i.severity === 'error'),
    hasWarnings: validationIssues.some(i => i.severity === 'warning'),
    schema,
    setActive,
    setSalt,
    setDefaultValue,
    setAllowlist,
    revert,
  };
}

/**
 * Hook for working with a single rule within a flag.
 */
export function useRule(flagKey: FeatureId, ruleIndex: number) {
  const { state, dispatch, schema } = useEditorContext();

  const flag = useMemo(() => selectFlag(state, flagKey), [state, flagKey]);
  const rule = flag?.rules[ruleIndex];

  const validationIssues = useMemo(() => {
    const flagIndex = state.current.flags.findIndex(f => f.key === flagKey);
    if (flagIndex === -1) return [];

    return state.validation.issues.filter(issue =>
      issue.path[0] === 'flags' &&
      issue.path[1] === flagIndex &&
      issue.path[2] === 'rules' &&
      issue.path[3] === ruleIndex
    );
  }, [state, flagKey, ruleIndex]);

  const setValue = useCallback(
    (value: FlagValue) =>
      dispatch({ type: 'SET_RULE_VALUE', flagKey, ruleIndex, value }),
    [dispatch, flagKey, ruleIndex]
  );

  const setRampUp = useCallback(
    (rampUp: number) =>
      dispatch({ type: 'SET_RULE_RAMPUP', flagKey, ruleIndex, rampUp }),
    [dispatch, flagKey, ruleIndex]
  );

  const setPlatforms = useCallback(
    (platforms: Platform[]) =>
      dispatch({ type: 'SET_RULE_PLATFORMS', flagKey, ruleIndex, platforms }),
    [dispatch, flagKey, ruleIndex]
  );

  const setLocales = useCallback(
    (locales: AppLocale[]) =>
      dispatch({ type: 'SET_RULE_LOCALES', flagKey, ruleIndex, locales }),
    [dispatch, flagKey, ruleIndex]
  );

  const setVersionRange = useCallback(
    (versionRange: VersionRange) =>
      dispatch({ type: 'SET_RULE_VERSION_RANGE', flagKey, ruleIndex, versionRange }),
    [dispatch, flagKey, ruleIndex]
  );

  const setAxes = useCallback(
    (axes: Record<string, string[]>) =>
      dispatch({ type: 'SET_RULE_AXES', flagKey, ruleIndex, axes }),
    [dispatch, flagKey, ruleIndex]
  );

  const setAllowlist = useCallback(
    (allowlist: string[]) =>
      dispatch({ type: 'SET_RULE_ALLOWLIST', flagKey, ruleIndex, allowlist }),
    [dispatch, flagKey, ruleIndex]
  );

  const setNote = useCallback(
    (note: string | undefined) =>
      dispatch({ type: 'SET_RULE_NOTE', flagKey, ruleIndex, note }),
    [dispatch, flagKey, ruleIndex]
  );

  return {
    rule,
    flagType: flag?.type,
    validationIssues,
    hasErrors: validationIssues.some(i => i.severity === 'error'),
    schema,
    setValue,
    setRampUp,
    setPlatforms,
    setLocales,
    setVersionRange,
    setAxes,
    setAllowlist,
    setNote,
  };
}

/**
 * Hook for validation state only.
 */
export function useValidation() {
  const { state } = useEditorContext();

  return {
    validation: state.validation,
    isValid: state.validation.valid,
    errorCount: state.validation.errorCount,
    warningCount: state.validation.warningCount,
    issues: state.validation.issues,
  };
}

/**
 * Hook for diff computation.
 */
export function useDiff(): SnapshotDiff {
  const { state } = useEditorContext();
  return useMemo(() => computeDiff(state), [state]);
}
