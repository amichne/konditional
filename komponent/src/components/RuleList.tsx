/**
 * RuleList & RuleEditor Components
 * 
 * Renders the list of rules for a flag and individual rule editing.
 * Rules are displayed in specificity order with visual precedence indicators.
 */

import { memo, useCallback, useState } from 'react';
import type {
  Rule,
  FlagValueType,
  FeatureId,
  Platform,
  AppLocale,
  VersionRange,
  Version,
} from '../types/schema';
import { useRule } from '../types/context';
import { ValueEditor } from './ValueEditor';

// =============================================================================
// Specificity Calculation (mirrors backend logic)
// =============================================================================

function computeSpecificity(rule: Rule): number {
  let score = 0;

  // Platforms contribute if not all platforms
  if (rule.platforms.length > 0 && rule.platforms.length < 3) {
    score += rule.platforms.length;
  }

  // Locales contribute
  const locales = Array.isArray(rule.locales) ? rule.locales : [rule.locales];
  score += locales.length;

  // Version bounds contribute
  if (rule.versionRange.type !== 'UNBOUNDED') {
    score += rule.versionRange.type === 'MIN_AND_MAX_BOUND' ? 2 : 1;
  }

  // Axes contribute
  for (const values of Object.values(rule.axes)) {
    score += values.length;
  }

  return score;
}

// =============================================================================
// RuleList Component
// =============================================================================

interface RuleListProps {
  flagKey: FeatureId;
  rules: Rule[];
  flagType: FlagValueType;
}

export function RuleList({ flagKey, rules, flagType }: RuleListProps): JSX.Element {
  // Rules are already sorted by specificity in the backend, but we display
  // the specificity score for clarity
  const rulesWithSpecificity = rules.map((rule, index) => ({
    rule,
    index,
    specificity: computeSpecificity(rule),
  }));

  // Sort by specificity descending (highest first = matches first)
  const sortedRules = [...rulesWithSpecificity].sort(
    (a, b) => b.specificity - a.specificity
  );

  return (
    <div className="ke-rule-list">
      {sortedRules.map(({ index, specificity }, displayOrder) => (
        <RuleEditor
          key={index}
          flagKey={flagKey}
          ruleIndex={index}
          flagType={flagType}
          specificity={specificity}
          evaluationOrder={displayOrder + 1}
        />
      ))}
    </div>
  );
}

// =============================================================================
// RuleEditor Component
// =============================================================================

interface RuleEditorProps {
  flagKey: FeatureId;
  ruleIndex: number;
  flagType: FlagValueType;
  specificity: number;
  evaluationOrder: number;
}

export const RuleEditor = memo(function RuleEditor({
  flagKey,
  ruleIndex,
  flagType: _flagType, // Reserved for type-specific rendering
  specificity,
  evaluationOrder,
}: RuleEditorProps): JSX.Element | null {
  const {
    rule,
    validationIssues,
    hasErrors,
    schema,
    setValue,
    setRampUp,
    setPlatforms,
    setLocales,
    setVersionRange,
    setAxes,
    setAllowlist,
    setNote,
  } = useRule(flagKey, ruleIndex);

  const [isExpanded, setIsExpanded] = useState(false);

  if (!rule) return null;

  const toggleExpanded = useCallback(() => {
    setIsExpanded((prev) => !prev);
  }, []);

  // Format locales for display
  const localesArray = Array.isArray(rule.locales) ? rule.locales : [rule.locales];

  return (
    <div
      className={`ke-rule-editor ${hasErrors ? 'ke-rule-editor--error' : ''} ${
        isExpanded ? 'ke-rule-editor--expanded' : ''
      }`}
    >
      {/* Rule Header */}
      <header className="ke-rule-header" onClick={toggleExpanded}>
        <div className="ke-rule-header-left">
          <span className="ke-expand-icon">{isExpanded ? '▼' : '▶'}</span>
          <span className="ke-rule-order" title="Evaluation order (lower = checked first)">
            #{evaluationOrder}
          </span>
          <SpecificityIndicator score={specificity} />
        </div>

        <div className="ke-rule-header-center">
          {/* Quick summary of targeting */}
          <span className="ke-rule-summary">
            {rule.platforms.length < 3 && (
              <span className="ke-rule-chip">{rule.platforms.join(', ')}</span>
            )}
            {localesArray.length <= 2 && (
              <span className="ke-rule-chip">
                {localesArray.map(formatLocale).join(', ')}
              </span>
            )}
            {rule.versionRange.type !== 'UNBOUNDED' && (
              <span className="ke-rule-chip">{formatVersionRange(rule.versionRange)}</span>
            )}
            {Object.keys(rule.axes).length > 0 && (
              <span className="ke-rule-chip">
                +{Object.keys(rule.axes).length} axes
              </span>
            )}
          </span>
        </div>

        <div className="ke-rule-header-right">
          <span className="ke-rule-value-preview">
            → <ValueEditor value={rule.value} onChange={() => {}} compact schema={schema} />
          </span>
          <span className="ke-rule-rampup">{rule.rampUp}%</span>
        </div>
      </header>

      {/* Rule Body */}
      {isExpanded && (
        <div className="ke-rule-body">
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

          {/* Note */}
          <div className="ke-rule-row">
            <label className="ke-rule-label">Note</label>
            <input
              type="text"
              value={rule.note ?? ''}
              onChange={(e) => setNote(e.target.value || undefined)}
              placeholder="Optional description"
              className="ke-input"
            />
          </div>

          {/* Value */}
          <div className="ke-rule-row">
            <label className="ke-rule-label">Value</label>
            <ValueEditor value={rule.value} onChange={setValue} schema={schema} />
          </div>

          {/* Ramp-up */}
          <div className="ke-rule-row">
            <label className="ke-rule-label">
              Ramp-up
              <span className="ke-rule-hint">
                Percentage of matched users who receive this value
              </span>
            </label>
            <RampUpSlider value={rule.rampUp} onChange={setRampUp} />
          </div>

          {/* Platforms */}
          <div className="ke-rule-row">
            <label className="ke-rule-label">Platforms</label>
            <PlatformSelector
              selected={rule.platforms}
              onChange={setPlatforms}
            />
          </div>

          {/* Locales */}
          <div className="ke-rule-row">
            <label className="ke-rule-label">Locales</label>
            <LocaleSelector
              selected={localesArray}
              onChange={setLocales}
            />
          </div>

          {/* Version Range */}
          <div className="ke-rule-row">
            <label className="ke-rule-label">Version Range</label>
            <VersionRangeEditor
              value={rule.versionRange}
              onChange={setVersionRange}
            />
          </div>

          {/* Axes */}
          {Object.keys(rule.axes).length > 0 && (
            <div className="ke-rule-row">
              <label className="ke-rule-label">Axes</label>
              <AxesEditor value={rule.axes} onChange={setAxes} />
            </div>
          )}

          {/* Rule-level allowlist */}
          <div className="ke-rule-row">
            <label className="ke-rule-label">
              Allowlist
              <span className="ke-rule-hint">
                StableIds that bypass ramp-up for this rule
              </span>
            </label>
            <textarea
              value={rule.rampUpAllowlist.join('\n')}
              onChange={(e) => {
                const lines = e.target.value
                  .split('\n')
                  .map((l) => l.trim())
                  .filter((l) => l.length > 0);
                setAllowlist(lines);
              }}
              placeholder="One hex-encoded StableId per line"
              className="ke-textarea"
              rows={2}
            />
          </div>
        </div>
      )}
    </div>
  );
});

// =============================================================================
// Helper Components
// =============================================================================

interface SpecificityIndicatorProps {
  score: number;
}

function SpecificityIndicator({ score }: SpecificityIndicatorProps): JSX.Element {
  // Visual representation: filled stars based on score
  const maxStars = 5;
  const filledStars = Math.min(score, maxStars);

  return (
    <span className="ke-specificity" title={`Specificity score: ${score}`}>
      {Array.from({ length: maxStars }, (_, i) => (
        <span
          key={i}
          className={`ke-specificity-star ${i < filledStars ? 'ke-specificity-star--filled' : ''}`}
        >
          ★
        </span>
      ))}
    </span>
  );
}

interface RampUpSliderProps {
  value: number;
  onChange: (value: number) => void;
}

function RampUpSlider({ value, onChange }: RampUpSliderProps): JSX.Element {
  return (
    <div className="ke-rampup-slider">
      <input
        type="range"
        min={0}
        max={100}
        step={1}
        value={value}
        onChange={(e) => onChange(parseInt(e.target.value, 10))}
        className="ke-slider"
      />
      <input
        type="number"
        min={0}
        max={100}
        value={value}
        onChange={(e) => onChange(Math.max(0, Math.min(100, parseInt(e.target.value, 10) || 0)))}
        className="ke-input ke-input-sm ke-rampup-input"
      />
      <span className="ke-rampup-unit">%</span>
    </div>
  );
}

interface PlatformSelectorProps {
  selected: Platform[];
  onChange: (platforms: Platform[]) => void;
}

const ALL_PLATFORMS: Platform[] = ['IOS', 'ANDROID', 'WEB'];

function PlatformSelector({ selected, onChange }: PlatformSelectorProps): JSX.Element {
  const togglePlatform = useCallback(
    (platform: Platform) => {
      if (selected.includes(platform)) {
        onChange(selected.filter((p) => p !== platform));
      } else {
        onChange([...selected, platform]);
      }
    },
    [selected, onChange]
  );

  return (
    <div className="ke-chip-selector">
      {ALL_PLATFORMS.map((platform) => (
        <button
          key={platform}
          type="button"
          className={`ke-chip ${selected.includes(platform) ? 'ke-chip--selected' : ''}`}
          onClick={() => togglePlatform(platform)}
        >
          {platform}
        </button>
      ))}
    </div>
  );
}

interface LocaleSelectorProps {
  selected: AppLocale[];
  onChange: (locales: AppLocale[]) => void;
}

// Common locales shown by default, others in dropdown
const COMMON_LOCALES: AppLocale[] = [
  'UNITED_STATES',
  'UNITED_KINGDOM',
  'CANADA',
  'AUSTRALIA',
  'GERMANY',
  'FRANCE',
  'JAPAN',
];

function LocaleSelector({ selected, onChange }: LocaleSelectorProps): JSX.Element {
  const [showAll, setShowAll] = useState(false);

  const toggleLocale = useCallback(
    (locale: AppLocale) => {
      if (selected.includes(locale)) {
        onChange(selected.filter((l) => l !== locale));
      } else {
        onChange([...selected, locale]);
      }
    },
    [selected, onChange]
  );

  // Show common locales + any selected that aren't in common
  const visibleLocales = showAll
    ? [...COMMON_LOCALES, ...selected.filter((l) => !COMMON_LOCALES.includes(l))]
    : [...new Set([...COMMON_LOCALES, ...selected])];

  return (
    <div className="ke-locale-selector">
      <div className="ke-chip-selector ke-chip-selector--wrap">
        {visibleLocales.map((locale) => (
          <button
            key={locale}
            type="button"
            className={`ke-chip ${selected.includes(locale) ? 'ke-chip--selected' : ''}`}
            onClick={() => toggleLocale(locale)}
          >
            {formatLocale(locale)}
          </button>
        ))}
        <button
          type="button"
          className="ke-chip ke-chip--more"
          onClick={() => setShowAll(!showAll)}
        >
          {showAll ? 'Show less' : `+${26 - COMMON_LOCALES.length} more`}
        </button>
      </div>
    </div>
  );
}

interface VersionRangeEditorProps {
  value: VersionRange;
  onChange: (range: VersionRange) => void;
}

function VersionRangeEditor({ value, onChange }: VersionRangeEditorProps): JSX.Element {
  const handleTypeChange = useCallback(
    (newType: VersionRange['type']) => {
      switch (newType) {
        case 'UNBOUNDED':
          onChange({ type: 'UNBOUNDED' });
          break;
        case 'MIN_BOUND':
          onChange({
            type: 'MIN_BOUND',
            min: 'min' in value ? value.min : { major: 0, minor: 0, patch: 0 },
          });
          break;
        case 'MAX_BOUND':
          onChange({
            type: 'MAX_BOUND',
            max: 'max' in value ? value.max : { major: 99, minor: 99, patch: 99 },
          });
          break;
        case 'MIN_AND_MAX_BOUND':
          onChange({
            type: 'MIN_AND_MAX_BOUND',
            min: 'min' in value ? value.min : { major: 0, minor: 0, patch: 0 },
            max: 'max' in value ? value.max : { major: 99, minor: 99, patch: 99 },
          });
          break;
      }
    },
    [value, onChange]
  );

  const handleMinChange = useCallback(
    (min: Version) => {
      if (value.type === 'MIN_BOUND') {
        onChange({ type: 'MIN_BOUND', min });
      } else if (value.type === 'MIN_AND_MAX_BOUND') {
        onChange({ ...value, min });
      }
    },
    [value, onChange]
  );

  const handleMaxChange = useCallback(
    (max: Version) => {
      if (value.type === 'MAX_BOUND') {
        onChange({ type: 'MAX_BOUND', max });
      } else if (value.type === 'MIN_AND_MAX_BOUND') {
        onChange({ ...value, max });
      }
    },
    [value, onChange]
  );

  return (
    <div className="ke-version-range-editor">
      <div className="ke-version-type-selector">
        <label className="ke-radio-option">
          <input
            type="radio"
            checked={value.type === 'UNBOUNDED'}
            onChange={() => handleTypeChange('UNBOUNDED')}
          />
          <span>All versions</span>
        </label>
        <label className="ke-radio-option">
          <input
            type="radio"
            checked={value.type === 'MIN_BOUND'}
            onChange={() => handleTypeChange('MIN_BOUND')}
          />
          <span>Minimum</span>
        </label>
        <label className="ke-radio-option">
          <input
            type="radio"
            checked={value.type === 'MAX_BOUND'}
            onChange={() => handleTypeChange('MAX_BOUND')}
          />
          <span>Maximum</span>
        </label>
        <label className="ke-radio-option">
          <input
            type="radio"
            checked={value.type === 'MIN_AND_MAX_BOUND'}
            onChange={() => handleTypeChange('MIN_AND_MAX_BOUND')}
          />
          <span>Range</span>
        </label>
      </div>

      {(value.type === 'MIN_BOUND' || value.type === 'MIN_AND_MAX_BOUND') && (
        <div className="ke-version-input-row">
          <span className="ke-version-label">Min:</span>
          <VersionInput value={value.min} onChange={handleMinChange} />
        </div>
      )}

      {(value.type === 'MAX_BOUND' || value.type === 'MIN_AND_MAX_BOUND') && (
        <div className="ke-version-input-row">
          <span className="ke-version-label">Max:</span>
          <VersionInput value={value.max} onChange={handleMaxChange} />
        </div>
      )}
    </div>
  );
}

interface VersionInputProps {
  value: Version;
  onChange: (version: Version) => void;
}

function VersionInput({ value, onChange }: VersionInputProps): JSX.Element {
  return (
    <div className="ke-version-input">
      <input
        type="number"
        min={0}
        value={value.major}
        onChange={(e) =>
          onChange({ ...value, major: Math.max(0, parseInt(e.target.value, 10) || 0) })
        }
        className="ke-input ke-input-xs"
        placeholder="0"
      />
      <span className="ke-version-separator">.</span>
      <input
        type="number"
        min={0}
        value={value.minor}
        onChange={(e) =>
          onChange({ ...value, minor: Math.max(0, parseInt(e.target.value, 10) || 0) })
        }
        className="ke-input ke-input-xs"
        placeholder="0"
      />
      <span className="ke-version-separator">.</span>
      <input
        type="number"
        min={0}
        value={value.patch}
        onChange={(e) =>
          onChange({ ...value, patch: Math.max(0, parseInt(e.target.value, 10) || 0) })
        }
        className="ke-input ke-input-xs"
        placeholder="0"
      />
    </div>
  );
}

interface AxesEditorProps {
  value: Record<string, string[]>;
  onChange: (axes: Record<string, string[]>) => void;
}

function AxesEditor({ value }: AxesEditorProps): JSX.Element {
  return (
    <div className="ke-axes-editor">
      {Object.entries(value).map(([axisKey, axisValues]) => (
        <div key={axisKey} className="ke-axis-row">
          <span className="ke-axis-key">{axisKey}:</span>
          <div className="ke-chip-selector">
            {axisValues.map((v) => (
              <span key={v} className="ke-chip ke-chip--selected">
                {v}
              </span>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

// =============================================================================
// Formatting Helpers
// =============================================================================

function formatLocale(locale: AppLocale): string {
  // Convert UNITED_STATES -> US, UNITED_KINGDOM -> UK, etc.
  const shortNames: Partial<Record<AppLocale, string>> = {
    UNITED_STATES: 'US',
    UNITED_KINGDOM: 'UK',
    CANADA: 'CA',
    CANADA_FRENCH: 'CA-FR',
    AUSTRALIA: 'AU',
    NEW_ZEALAND: 'NZ',
    HONG_KONG: 'HK',
    HONG_KONG_ENGLISH: 'HK-EN',
    BELGIUM_DUTCH: 'BE-NL',
    BELGIUM_FRENCH: 'BE-FR',
  };
  return shortNames[locale] ?? locale.replace(/_/g, ' ');
}

function formatVersionRange(range: VersionRange): string {
  switch (range.type) {
    case 'UNBOUNDED':
      return 'all versions';
    case 'MIN_BOUND':
      return `≥${range.min.major}.${range.min.minor}.${range.min.patch}`;
    case 'MAX_BOUND':
      return `≤${range.max.major}.${range.max.minor}.${range.max.patch}`;
    case 'MIN_AND_MAX_BOUND':
      return `${range.min.major}.${range.min.minor}.${range.min.patch}–${range.max.major}.${range.max.minor}.${range.max.patch}`;
  }
}
