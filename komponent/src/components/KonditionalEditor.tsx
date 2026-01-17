/**
 * KonditionalEditor - Root Component
 * 
 * This is the primary public API for integrators.
 * It accepts a snapshot, provides editing capabilities, and calls back on save.
 * 
 * USAGE:
 * ```tsx
 * <KonditionalEditor
 *   snapshot={fetchedSnapshot}
 *   onSave={async (snapshot) => { await persistToBackend(snapshot); }}
 * />
 * ```
 */

import { useCallback, useState, useMemo } from 'react';
import type { Snapshot, Flag } from '../types/schema';
import { EditorProvider, useEditor } from '../types/context';
import { FlagList } from './FlagList';
import { SaveModal } from './DiffDisplay';

// =============================================================================
// Public Props Interface
// =============================================================================

export interface KonditionalEditorProps {
  /** Initial snapshot to render and edit */
  snapshot: Snapshot;

  /** Called when user confirms save. Receives the modified snapshot. */
  onSave: (snapshot: Snapshot) => void | Promise<void>;

  /** Optional: called on every change for live preview or external validation */
  onChange?: (snapshot: Snapshot) => void;

  /** Optional: filter which flags are displayed/editable */
  filter?: (flag: Flag) => boolean;

  /** Optional: theme preference */
  theme?: 'light' | 'dark' | 'system';

  /** Optional: custom class name for the root element */
  className?: string;
}

// =============================================================================
// Main Component (Public Export)
// =============================================================================

export function KonditionalEditor({
  snapshot,
  onSave,
  onChange,
  filter,
  theme = 'system',
  className = '',
}: KonditionalEditorProps): JSX.Element {
  return (
    <EditorProvider snapshot={snapshot}>
      <EditorShell
        onSave={onSave}
        onChange={onChange}
        filter={filter}
        theme={theme}
        className={className}
      />
    </EditorProvider>
  );
}

// =============================================================================
// Internal Shell (has access to context)
// =============================================================================

interface EditorShellProps {
  onSave: (snapshot: Snapshot) => void | Promise<void>;
  onChange?: (snapshot: Snapshot) => void;
  filter?: (flag: Flag) => boolean;
  theme: 'light' | 'dark' | 'system';
  className: string;
}

function EditorShell({
  onSave,
  onChange: _onChange, // Reserved for future live preview
  filter,
  theme,
  className,
}: EditorShellProps): JSX.Element {
  const {
    snapshot,
    flags,
    validation,
    isDirty,
    canSave,
    diff,
    revertAll,
  } = useEditor();

  const [isSaveModalOpen, setIsSaveModalOpen] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  // Filter flags based on prop and search query
  const visibleFlags = useMemo(() => {
    let result = flags;

    // Apply custom filter
    if (filter) {
      result = result.filter(filter);
    }

    // Apply search filter
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      result = result.filter(flag =>
        flag.key.toLowerCase().includes(query) ||
        flag.type.toLowerCase().includes(query)
      );
    }

    return result;
  }, [flags, filter, searchQuery]);

  // Grouped by namespace
  const flagsByNamespace = useMemo(() => {
    const groups = new Map<string, Flag[]>();

    for (const flag of visibleFlags) {
      const parts = flag.key.split('::');
      const namespace = parts.length >= 2 ? parts[1] : 'unknown';

      if (!groups.has(namespace)) {
        groups.set(namespace, []);
      }
      groups.get(namespace)!.push(flag);
    }

    return groups;
  }, [visibleFlags]);

  const handleSaveClick = useCallback(() => {
    if (canSave) {
      setIsSaveModalOpen(true);
    }
  }, [canSave]);

  const handleConfirmSave = useCallback(async () => {
    setIsSaving(true);
    try {
      await onSave(snapshot);
      setIsSaveModalOpen(false);
      // Note: We don't reset state here - let the parent decide whether to
      // reload the snapshot (which would trigger a RESET via prop change)
    } catch (error) {
      console.error('Save failed:', error);
      // Keep modal open on failure so user can retry
    } finally {
      setIsSaving(false);
    }
  }, [snapshot, onSave]);

  const handleCancelSave = useCallback(() => {
    setIsSaveModalOpen(false);
  }, []);

  // Determine theme class
  const themeClass = theme === 'system'
    ? '' // Let system/browser decide via prefers-color-scheme
    : theme === 'dark'
      ? 'dark'
      : 'light';

  return (
    <div className={`konditional-editor ${themeClass} ${className}`.trim()}>
      {/* Header */}
      <header className="ke-header">
        <div className="ke-header-left">
          <h1 className="ke-title">Configuration Editor</h1>
          {isDirty && (
            <span className="ke-dirty-indicator" title="Unsaved changes">
              ●
            </span>
          )}
        </div>

        <div className="ke-header-center">
          <input
            type="search"
            placeholder="Search flags..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="ke-search-input"
          />
        </div>

        <div className="ke-header-right">
          {/* Validation summary */}
          {validation.errorCount > 0 && (
            <span className="ke-validation-badge ke-validation-error">
              {validation.errorCount} error{validation.errorCount !== 1 ? 's' : ''}
            </span>
          )}
          {validation.warningCount > 0 && (
            <span className="ke-validation-badge ke-validation-warning">
              {validation.warningCount} warning{validation.warningCount !== 1 ? 's' : ''}
            </span>
          )}

          {/* Actions */}
          <button
            onClick={revertAll}
            disabled={!isDirty}
            className="ke-button ke-button-secondary"
          >
            Revert All
          </button>
          <button
            onClick={handleSaveClick}
            disabled={!canSave}
            className="ke-button ke-button-primary"
            title={
              !isDirty
                ? 'No changes to save'
                : !validation.valid
                  ? 'Fix validation errors before saving'
                  : 'Save changes'
            }
          >
            Save
          </button>
        </div>
      </header>

      {/* Main content */}
      <main className="ke-main">
        {flagsByNamespace.size === 0 ? (
          <div className="ke-empty-state">
            {searchQuery
              ? `No flags match "${searchQuery}"`
              : 'No flags in snapshot'}
          </div>
        ) : (
          Array.from(flagsByNamespace.entries()).map(([namespace, nsFlags]) => (
            <section key={namespace} className="ke-namespace-section">
              <h2 className="ke-namespace-header">{namespace}</h2>
              <FlagList flags={nsFlags} />
            </section>
          ))
        )}
      </main>

      {/* Save confirmation modal */}
      {isSaveModalOpen && (
        <SaveModal
          diff={diff}
          validation={validation}
          isSaving={isSaving}
          onConfirm={handleConfirmSave}
          onCancel={handleCancelSave}
        />
      )}
    </div>
  );
}

export default KonditionalEditor;
