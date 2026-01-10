import React, { useState } from 'react';
import styles from './ScopeExplorer.module.css';

interface ScopeItem {
  name: string;
  type: 'property' | 'method' | 'dsl';
  signature: string;
  description?: string;
  inherited?: boolean;
}

interface ScopeExplorerProps {
  title: string;
  items: ScopeItem[];
  context?: string;
}

/**
 * Shows what's available in a given scope - what IntelliJ autocomplete would show.
 */
export default function ScopeExplorer({
  title,
  items,
  context,
}: ScopeExplorerProps): JSX.Element {
  const [filter, setFilter] = useState<'all' | 'dsl' | 'properties' | 'methods'>('all');

  const filteredItems = items.filter(item => {
    if (filter === 'all') return true;
    if (filter === 'dsl') return item.type === 'dsl';
    if (filter === 'properties') return item.type === 'property';
    if (filter === 'methods') return item.type === 'method';
    return true;
  });

  const getIcon = (type: ScopeItem['type']) => {
    switch (type) {
      case 'dsl': return '✨';
      case 'method': return '⚡';
      case 'property': return '📦';
    }
  };

  return (
    <div className={styles.scopeExplorer}>
      <div className={styles.header}>
        <div className={styles.titleSection}>
          <span className={styles.icon}>🔍</span>
          <h4 className={styles.title}>{title}</h4>
        </div>
        {context && (
          <div className={styles.context}>
            <span className={styles.contextLabel}>In scope:</span>
            <code className={styles.contextCode}>{context}</code>
          </div>
        )}
      </div>

      <div className={styles.filters}>
        <button
          className={`${styles.filterBtn} ${filter === 'all' ? styles.active : ''}`}
          onClick={() => setFilter('all')}
        >
          All ({items.length})
        </button>
        <button
          className={`${styles.filterBtn} ${filter === 'dsl' ? styles.active : ''}`}
          onClick={() => setFilter('dsl')}
        >
          DSL ({items.filter(i => i.type === 'dsl').length})
        </button>
        <button
          className={`${styles.filterBtn} ${filter === 'properties' ? styles.active : ''}`}
          onClick={() => setFilter('properties')}
        >
          Properties ({items.filter(i => i.type === 'property').length})
        </button>
        <button
          className={`${styles.filterBtn} ${filter === 'methods' ? styles.active : ''}`}
          onClick={() => setFilter('methods')}
        >
          Methods ({items.filter(i => i.type === 'method').length})
        </button>
      </div>

      <div className={styles.itemsList}>
        {filteredItems.map((item, i) => (
          <div key={i} className={`${styles.item} ${item.inherited ? styles.inherited : ''}`}>
            <div className={styles.itemHeader}>
              <span className={styles.itemIcon}>{getIcon(item.type)}</span>
              <code className={styles.itemName}>{item.name}</code>
              {item.inherited && (
                <span className={styles.inheritedBadge}>inherited</span>
              )}
            </div>
            <code className={styles.signature}>{item.signature}</code>
            {item.description && (
              <div className={styles.description}>{item.description}</div>
            )}
          </div>
        ))}
        {filteredItems.length === 0 && (
          <div className={styles.emptyState}>
            No items match the current filter.
          </div>
        )}
      </div>
    </div>
  );
}
