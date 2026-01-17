/**
 * FlagList & FlagCard Components
 * 
 * Renders the list of flags and individual flag editing cards.
 * FlagCard handles the flag-level display and delegates to RuleList for rules.
 */

import { useState, useCallback, memo } from 'react';
import type { Flag, FeatureId, FlagValueType } from '../types/schema';
import { useFlag } from '../types/context';
import { RuleList } from './RuleList';
import { ValueEditor } from './ValueEditor';
import { FlagDiffInline } from './DiffDisplay';

// =============================================================================
// FlagList Component
// =============================================================================

interface FlagListProps {
  flags: Flag[];
}

export function FlagList({ flags }: FlagListProps): JSX.Element {
  return (
    <div className="ke-flag-list">
      {flags.map((flag) => (
        <FlagCard key={flag.key} flagKey={flag.key} />
      ))}
    </div>
  );
}

// =============================================================================
// FlagCard Component
// =============================================================================

interface FlagCardProps {
  flagKey: FeatureId;
}

export const FlagCard = memo(function FlagCard({ flagKey }: FlagCardProps): JSX.Element | null {
  const {
    flag,
    originalFlag,
    isModified,
    validationIssues,
    hasErrors,
    hasWarnings,
    schema,
    setActive,
    setSalt,
    setDefaultValue,
    setAllowlist,
    revert,
  } = useFlag(flagKey);

  const [isExpanded, setIsExpanded] = useState(false);
  const [showDiff, setShowDiff] = useState(false);

  const toggleExpanded = useCallback(() => {
    setIsExpanded((prev) => !prev);
  }, []);

  const toggleDiff = useCallback(() => {
    setShowDiff((prev) => !prev);
  }, []);

  if (!flag) return null;

  // Parse the flag key to extract the feature name
  const keyParts = flag.key.split('::');
  const featureName = keyParts.length >= 3 ? keyParts[2] : flag.key;

  return (
    <article
      className={`ke-flag-card ${isExpanded ? 'ke-flag-card--expanded' : ''} ${
        hasErrors ? 'ke-flag-card--error' : hasWarnings ? 'ke-flag-card--warning' : ''
      } ${isModified ? 'ke-flag-card--modified' : ''}`}
      data-flag-key={flag.key}
    >
      {/* Card Header - Always visible */}
      <header className="ke-flag-header" onClick={toggleExpanded}>
        <div className="ke-flag-header-left">
          {/* Expand/collapse indicator */}
          <span className={`ke-expand-icon ${isExpanded ? 'ke-expand-icon--open' : ''}`}>
            ▶
          </span>

          {/* Type badge */}
          <TypeBadge type={flag.type} />

          {/* Feature name */}
          <h3 className="ke-flag-name">{featureName}</h3>

          {/* Status indicators */}
          {!flag.isActive && (
            <span className="ke-status-badge ke-status-inactive" title="Flag is inactive">
              Inactive
            </span>
          )}
          {isModified && (
            <span className="ke-status-badge ke-status-modified" title="Has unsaved changes">
              Modified
            </span>
          )}
        </div>

        <div className="ke-flag-header-right">
          {/* Rule count */}
          <span className="ke-rule-count">
            {flag.rules.length} rule{flag.rules.length !== 1 ? 's' : ''}
          </span>

          {/* Validation indicator */}
          {hasErrors && (
            <span className="ke-validation-indicator ke-validation-indicator--error" title="Has validation errors">
              ✗
            </span>
          )}
          {!hasErrors && hasWarnings && (
            <span className="ke-validation-indicator ke-validation-indicator--warning" title="Has warnings">
              ⚠
            </span>
          )}
        </div>
      </header>

      {/* Card Body - Visible when expanded */}
      {isExpanded && (
        <div className="ke-flag-body">
          {/* Validation issues */}
          {validationIssues.length > 0 && (
            <div className="ke-validation-issues">
              {validationIssues.map((issue, idx) => (
                <div
                  key={idx}
                  className={`ke-validation-issue ke-validation-issue--${issue.severity}`}
                >
                  <span className="ke-validation-issue-icon">
                    {issue.severity === 'error' ? '✗' : '⚠'}
                  </span>
                  <span className="ke-validation-issue-message">{issue.message}</span>
                </div>
              ))}
            </div>
          )}

          {/* Inline diff toggle */}
          {isModified && originalFlag && (
            <div className="ke-diff-toggle">
              <button
                onClick={(e) => { e.stopPropagation(); toggleDiff(); }}
                className="ke-button ke-button-text"
              >
                {showDiff ? 'Hide Changes' : 'Show Changes'}
              </button>
              <button
                onClick={(e) => { e.stopPropagation(); revert(); }}
                className="ke-button ke-button-text ke-button-danger"
              >
                Revert
              </button>
            </div>
          )}

          {/* Diff display */}
          {showDiff && originalFlag && (
            <FlagDiffInline before={originalFlag} after={flag} />
          )}

          {/* Flag-level settings */}
          <section className="ke-flag-settings">
            <h4 className="ke-section-title">Flag Settings</h4>

            <div className="ke-settings-grid">
              {/* Active toggle */}
              <div className="ke-setting-row">
                <label className="ke-setting-label">Active</label>
                <div className="ke-setting-control">
                  <Toggle
                    checked={flag.isActive}
                    onChange={setActive}
                    label={flag.isActive ? 'Enabled' : 'Disabled'}
                  />
                </div>
              </div>

              {/* Salt */}
              <div className="ke-setting-row">
                <label className="ke-setting-label">
                  Salt
                  <span className="ke-setting-hint">
                    (changing redistributes ramp-up buckets)
                  </span>
                </label>
                <div className="ke-setting-control">
                  <input
                    type="text"
                    value={flag.salt}
                    onChange={(e) => setSalt(e.target.value)}
                    className="ke-input ke-input-sm"
                    placeholder="v1"
                  />
                </div>
              </div>

              {/* Default value */}
              <div className="ke-setting-row">
                <label className="ke-setting-label">Default Value</label>
                <div className="ke-setting-control">
                  <ValueEditor
                    value={flag.defaultValue}
                    onChange={setDefaultValue}
                    schema={schema}
                  />
                </div>
              </div>

              {/* Flag-level allowlist */}
              {flag.rampUpAllowlist.length > 0 && (
                <div className="ke-setting-row">
                  <label className="ke-setting-label">
                    Global Allowlist
                    <span className="ke-setting-hint">
                      ({flag.rampUpAllowlist.length} IDs)
                    </span>
                  </label>
                  <div className="ke-setting-control">
                    <AllowlistEditor
                      value={flag.rampUpAllowlist}
                      onChange={setAllowlist}
                    />
                  </div>
                </div>
              )}
            </div>
          </section>

          {/* Rules section */}
          <section className="ke-rules-section">
            <h4 className="ke-section-title">
              Rules
              <span className="ke-section-subtitle">
                (evaluated in specificity order, highest first)
              </span>
            </h4>

            {flag.rules.length === 0 ? (
              <p className="ke-empty-rules">
                No rules defined. All evaluations will return the default value.
              </p>
            ) : (
              <RuleList flagKey={flag.key} rules={flag.rules} flagType={flag.type} />
            )}
          </section>
        </div>
      )}
    </article>
  );
});

// =============================================================================
// Helper Components
// =============================================================================

interface TypeBadgeProps {
  type: FlagValueType;
}

function TypeBadge({ type }: TypeBadgeProps): JSX.Element {
  const colors: Record<FlagValueType, string> = {
    BOOLEAN: 'ke-type-badge--boolean',
    STRING: 'ke-type-badge--string',
    INT: 'ke-type-badge--int',
    DOUBLE: 'ke-type-badge--double',
    ENUM: 'ke-type-badge--enum',
    DATA_CLASS: 'ke-type-badge--dataclass',
  };

  return (
    <span className={`ke-type-badge ${colors[type]}`}>
      {type.toLowerCase()}
    </span>
  );
}

interface ToggleProps {
  checked: boolean;
  onChange: (checked: boolean) => void;
  label?: string;
}

function Toggle({ checked, onChange, label }: ToggleProps): JSX.Element {
  return (
    <label className="ke-toggle">
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        className="ke-toggle-input"
      />
      <span className="ke-toggle-track">
        <span className="ke-toggle-thumb" />
      </span>
      {label && <span className="ke-toggle-label">{label}</span>}
    </label>
  );
}

interface AllowlistEditorProps {
  value: string[];
  onChange: (value: string[]) => void;
}

function AllowlistEditor({ value, onChange }: AllowlistEditorProps): JSX.Element {
  const handleTextChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      const lines = e.target.value
        .split('\n')
        .map((line) => line.trim())
        .filter((line) => line.length > 0);
      onChange(lines);
    },
    [onChange]
  );

  return (
    <textarea
      value={value.join('\n')}
      onChange={handleTextChange}
      placeholder="One hex-encoded StableId per line"
      className="ke-textarea ke-allowlist-editor"
      rows={Math.min(5, Math.max(2, value.length + 1))}
    />
  );
}

export { Toggle, AllowlistEditor, TypeBadge };
