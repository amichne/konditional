/**
 * DiffDisplay & SaveModal Components
 * 
 * Displays differences between original and modified state.
 * SaveModal provides pre-save review with aggregated diff and validation summary.
 */

import { useMemo } from 'react';
import type { Flag, FlagValue, VersionRange, Rule } from '../types/schema';
import type { SnapshotDiff, FieldChange } from '../types/state';
import type { ValidationResult } from '../types/validation';

// =============================================================================
// Inline Diff Display (within FlagCard)
// =============================================================================

interface FlagDiffInlineProps {
  before: Flag;
  after: Flag;
}

export function FlagDiffInline({ before, after }: FlagDiffInlineProps): JSX.Element {
  const changes = useMemo(() => computeDetailedChanges(before, after), [before, after]);

  if (changes.length === 0) {
    return <div className="ke-diff-inline ke-diff-empty">No changes</div>;
  }

  return (
    <div className="ke-diff-inline">
      <table className="ke-diff-table">
        <thead>
          <tr>
            <th className="ke-diff-th">Field</th>
            <th className="ke-diff-th ke-diff-th--before">Before</th>
            <th className="ke-diff-th ke-diff-th--after">After</th>
          </tr>
        </thead>
        <tbody>
          {changes.map((change, idx) => (
            <tr key={idx} className="ke-diff-row">
              <td className="ke-diff-path">{change.path}</td>
              <td className="ke-diff-before">
                <DiffValue value={change.before} />
              </td>
              <td className="ke-diff-after">
                <DiffValue value={change.after} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// =============================================================================
// Save Modal with Full Diff
// =============================================================================

interface SaveModalProps {
  diff: SnapshotDiff;
  validation: ValidationResult;
  isSaving: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export function SaveModal({
  diff,
  validation,
  isSaving,
  onConfirm,
  onCancel,
}: SaveModalProps): JSX.Element {
  const hasWarnings = validation.warningCount > 0;

  return (
    <div className="ke-modal-overlay" onClick={onCancel}>
      <div className="ke-modal" onClick={(e) => e.stopPropagation()}>
        <header className="ke-modal-header">
          <h2 className="ke-modal-title">Review Changes</h2>
          <button
            type="button"
            className="ke-modal-close"
            onClick={onCancel}
            aria-label="Close"
          >
            ×
          </button>
        </header>

        <div className="ke-modal-body">
          {/* Validation warnings (if any) */}
          {hasWarnings && (
            <div className="ke-modal-section ke-modal-warnings">
              <h3 className="ke-modal-section-title">
                ⚠ {validation.warningCount} Warning{validation.warningCount !== 1 ? 's' : ''}
              </h3>
              <ul className="ke-warning-list">
                {validation.issues
                  .filter((i) => i.severity === 'warning')
                  .map((issue, idx) => (
                    <li key={idx} className="ke-warning-item">
                      {issue.message}
                    </li>
                  ))}
              </ul>
            </div>
          )}

          {/* Changes summary */}
          <div className="ke-modal-section">
            <h3 className="ke-modal-section-title">
              {diff.flags.length} Flag{diff.flags.length !== 1 ? 's' : ''} Modified
            </h3>

            {diff.flags.length === 0 ? (
              <p className="ke-modal-empty">No changes to save.</p>
            ) : (
              <div className="ke-modal-diff-list">
                {diff.flags.map((flagDiff) => (
                  <FlagDiffSummary key={flagDiff.key} flagDiff={flagDiff} />
                ))}
              </div>
            )}
          </div>
        </div>

        <footer className="ke-modal-footer">
          <button
            type="button"
            className="ke-button ke-button-secondary"
            onClick={onCancel}
            disabled={isSaving}
          >
            Cancel
          </button>
          <button
            type="button"
            className="ke-button ke-button-primary"
            onClick={onConfirm}
            disabled={isSaving || diff.flags.length === 0}
          >
            {isSaving ? 'Saving...' : hasWarnings ? 'Save Anyway' : 'Save'}
          </button>
        </footer>
      </div>
    </div>
  );
}

// =============================================================================
// Flag Diff Summary (within SaveModal)
// =============================================================================

interface FlagDiffSummaryProps {
  flagDiff: SnapshotDiff['flags'][number];
}

function FlagDiffSummary({ flagDiff }: FlagDiffSummaryProps): JSX.Element {
  const featureName = flagDiff.key.split('::').pop() ?? flagDiff.key;

  return (
    <div className="ke-flag-diff-summary">
      <h4 className="ke-flag-diff-name">{featureName}</h4>
      <div className="ke-flag-diff-changes">
        {flagDiff.changes.map((change, idx) => (
          <div key={idx} className="ke-change-row">
            <span className="ke-change-path">{change.path}</span>
            <span className="ke-change-arrow">→</span>
            <span className="ke-change-before">
              <DiffValue value={change.before} compact />
            </span>
            <span className="ke-change-to">to</span>
            <span className="ke-change-after">
              <DiffValue value={change.after} compact />
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

// =============================================================================
// Diff Value Renderer
// =============================================================================

interface DiffValueProps {
  value: unknown;
  compact?: boolean;
}

function DiffValue({ value, compact = false }: DiffValueProps): JSX.Element {
  if (value === undefined) {
    return <span className="ke-diff-value ke-diff-value--undefined">(none)</span>;
  }

  if (value === null) {
    return <span className="ke-diff-value ke-diff-value--null">null</span>;
  }

  if (typeof value === 'boolean') {
    return (
      <span className={`ke-diff-value ke-diff-value--boolean ${value ? 'ke-true' : 'ke-false'}`}>
        {String(value)}
      </span>
    );
  }

  if (typeof value === 'number') {
    return <span className="ke-diff-value ke-diff-value--number">{value}</span>;
  }

  if (typeof value === 'string') {
    const display = compact && value.length > 20 ? value.slice(0, 20) + '...' : value;
    return (
      <span className="ke-diff-value ke-diff-value--string" title={value}>
        "{display}"
      </span>
    );
  }

  if (Array.isArray(value)) {
    if (compact) {
      return (
        <span className="ke-diff-value ke-diff-value--array">
          [{value.length} items]
        </span>
      );
    }
    return (
      <span className="ke-diff-value ke-diff-value--array">
        [{value.map((v, i) => (
          <span key={i}>
            {i > 0 && ', '}
            <DiffValue value={v} compact />
          </span>
        ))}]
      </span>
    );
  }

  if (typeof value === 'object') {
    // Handle FlagValue specifically
    if ('type' in value && typeof (value as any).type === 'string') {
      return <FlagValueDiff value={value as FlagValue} compact={compact} />;
    }

    // Handle VersionRange
    if ('type' in value && ['UNBOUNDED', 'MIN_BOUND', 'MAX_BOUND', 'MIN_AND_MAX_BOUND'].includes((value as any).type)) {
      return <VersionRangeDiff value={value as VersionRange} />;
    }

    // Generic object
    if (compact) {
      return (
        <span className="ke-diff-value ke-diff-value--object">
          {'{...}'}
        </span>
      );
    }

    return (
      <span className="ke-diff-value ke-diff-value--object">
        <pre>{JSON.stringify(value, null, 2)}</pre>
      </span>
    );
  }

  return <span className="ke-diff-value">{String(value)}</span>;
}

function FlagValueDiff({ value, compact }: { value: FlagValue; compact?: boolean }): JSX.Element {
  switch (value.type) {
    case 'BOOLEAN':
      return (
        <span className={`ke-diff-value ke-diff-value--boolean ${value.value ? 'ke-true' : 'ke-false'}`}>
          {String(value.value)}
        </span>
      );
    case 'STRING':
      const display = compact && value.value.length > 15 
        ? `"${value.value.slice(0, 15)}..."` 
        : `"${value.value}"`;
      return <span className="ke-diff-value ke-diff-value--string">{display}</span>;
    case 'INT':
    case 'DOUBLE':
      return <span className="ke-diff-value ke-diff-value--number">{value.value}</span>;
    case 'ENUM':
      return <span className="ke-diff-value ke-diff-value--enum">{value.value}</span>;
    case 'DATA_CLASS':
      return (
        <span className="ke-diff-value ke-diff-value--dataclass">
          {compact ? '{...}' : JSON.stringify(value.value)}
        </span>
      );
  }
}

function VersionRangeDiff({ value }: { value: VersionRange }): JSX.Element {
  const format = (v: { major: number; minor: number; patch: number }) =>
    `${v.major}.${v.minor}.${v.patch}`;

  switch (value.type) {
    case 'UNBOUNDED':
      return <span className="ke-diff-value">all versions</span>;
    case 'MIN_BOUND':
      return <span className="ke-diff-value">≥{format(value.min)}</span>;
    case 'MAX_BOUND':
      return <span className="ke-diff-value">≤{format(value.max)}</span>;
    case 'MIN_AND_MAX_BOUND':
      return <span className="ke-diff-value">{format(value.min)}–{format(value.max)}</span>;
  }
}

// =============================================================================
// Change Detection Helpers
// =============================================================================

function computeDetailedChanges(before: Flag, after: Flag): FieldChange[] {
  const changes: FieldChange[] = [];

  // Top-level properties
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
    changes.push({
      path: 'rampUpAllowlist',
      before: before.rampUpAllowlist,
      after: after.rampUpAllowlist,
    });
  }

  // Rules
  const maxRules = Math.max(before.rules.length, after.rules.length);
  for (let i = 0; i < maxRules; i++) {
    const beforeRule = before.rules[i];
    const afterRule = after.rules[i];

    if (!beforeRule && afterRule) {
      changes.push({ path: `rules[${i}]`, before: undefined, after: 'added' });
    } else if (beforeRule && !afterRule) {
      changes.push({ path: `rules[${i}]`, before: 'removed', after: undefined });
    } else if (beforeRule && afterRule && !deepEqual(beforeRule, afterRule)) {
      // Drill into rule changes
      const ruleChanges = computeRuleChanges(beforeRule, afterRule, i);
      changes.push(...ruleChanges);
    }
  }

  return changes;
}

function computeRuleChanges(before: Rule, after: Rule, index: number): FieldChange[] {
  const changes: FieldChange[] = [];
  const prefix = `rules[${index}]`;

  if (!deepEqual(before.value, after.value)) {
    changes.push({ path: `${prefix}.value`, before: before.value, after: after.value });
  }
  if (before.rampUp !== after.rampUp) {
    changes.push({ path: `${prefix}.rampUp`, before: before.rampUp, after: after.rampUp });
  }
  if (!deepEqual(before.platforms, after.platforms)) {
    changes.push({ path: `${prefix}.platforms`, before: before.platforms, after: after.platforms });
  }
  if (!deepEqual(before.locales, after.locales)) {
    changes.push({ path: `${prefix}.locales`, before: before.locales, after: after.locales });
  }
  if (!deepEqual(before.versionRange, after.versionRange)) {
    changes.push({
      path: `${prefix}.versionRange`,
      before: before.versionRange,
      after: after.versionRange,
    });
  }
  if (!deepEqual(before.axes, after.axes)) {
    changes.push({ path: `${prefix}.axes`, before: before.axes, after: after.axes });
  }
  if (!deepEqual(before.rampUpAllowlist, after.rampUpAllowlist)) {
    changes.push({
      path: `${prefix}.rampUpAllowlist`,
      before: before.rampUpAllowlist,
      after: after.rampUpAllowlist,
    });
  }
  if (before.note !== after.note) {
    changes.push({ path: `${prefix}.note`, before: before.note, after: after.note });
  }

  return changes;
}

function deepEqual(a: unknown, b: unknown): boolean {
  return JSON.stringify(a) === JSON.stringify(b);
}
