import React, { useState } from 'react';
import styles from './IDEInsight.module.css';

interface AutocompleteItem {
  text: string;
  type: string;
  description?: string;
}

interface TypeHint {
  line: number;
  text: string;
  type: string;
}

interface IDEInsightProps {
  title?: string;
  children: React.ReactNode;
  autocomplete?: AutocompleteItem[];
  typeHints?: TypeHint[];
  inferredTypes?: Record<string, string>;
}

export default function IDEInsight({
  title = "IDE Insight",
  children,
  autocomplete,
  typeHints,
  inferredTypes,
}: IDEInsightProps): JSX.Element {
  const [activeTab, setActiveTab] = useState<'code' | 'types' | 'autocomplete'>('code');

  return (
    <div className={styles.ideInsight}>
      <div className={styles.header}>
        <div className={styles.title}>
          <span className={styles.icon}>💡</span>
          {title}
        </div>
        <div className={styles.tabs}>
          <button
            className={`${styles.tab} ${activeTab === 'code' ? styles.active : ''}`}
            onClick={() => setActiveTab('code')}
          >
            Code
          </button>
          {typeHints && (
            <button
              className={`${styles.tab} ${activeTab === 'types' ? styles.active : ''}`}
              onClick={() => setActiveTab('types')}
            >
              Type Hints
            </button>
          )}
          {autocomplete && (
            <button
              className={`${styles.tab} ${activeTab === 'autocomplete' ? styles.active : ''}`}
              onClick={() => setActiveTab('autocomplete')}
            >
              Autocomplete
            </button>
          )}
        </div>
      </div>

      <div className={styles.content}>
        {activeTab === 'code' && (
          <div className={styles.codeView}>
            {children}
          </div>
        )}

        {activeTab === 'types' && typeHints && (
          <div className={styles.typeHintsView}>
            <div className={styles.codeWithHints}>
              {children}
              <div className={styles.hintsOverlay}>
                {typeHints.map((hint, i) => (
                  <div key={i} className={styles.typeHint} style={{ top: `${hint.line * 1.5}em` }}>
                    <span className={styles.hintLabel}>{hint.text}:</span>
                    <span className={styles.hintType}>{hint.type}</span>
                  </div>
                ))}
              </div>
            </div>
            {inferredTypes && (
              <div className={styles.inferredSection}>
                <h4>Inferred Types</h4>
                {Object.entries(inferredTypes).map(([name, type]) => (
                  <div key={name} className={styles.inferredType}>
                    <code className={styles.varName}>{name}</code>
                    <span className={styles.separator}>:</span>
                    <code className={styles.typeName}>{type}</code>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'autocomplete' && autocomplete && (
          <div className={styles.autocompleteView}>
            <div className={styles.autocompleteHint}>
              What IntelliJ shows when you type:
            </div>
            <div className={styles.autocompleteList}>
              {autocomplete.map((item, i) => (
                <div key={i} className={styles.autocompleteItem}>
                  <div className={styles.itemHeader}>
                    <span className={styles.itemIcon}>
                      {item.type === 'method' ? '⚡' : item.type === 'property' ? '📦' : '🔧'}
                    </span>
                    <code className={styles.itemText}>{item.text}</code>
                    <span className={styles.itemType}>{item.type}</span>
                  </div>
                  {item.description && (
                    <div className={styles.itemDescription}>{item.description}</div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
