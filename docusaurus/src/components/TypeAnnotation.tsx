import React from 'react';
import styles from './TypeAnnotation.module.css';

interface TypeAnnotationProps {
  children: React.ReactNode;
  type: string;
  description?: string;
}

/**
 * Inline component to show type information on hover.
 * Wraps code tokens to show what the IDE infers.
 */
export default function TypeAnnotation({
  children,
  type,
  description,
}: TypeAnnotationProps): JSX.Element {
  return (
    <span className={styles.typeAnnotation}>
      <span className={styles.annotatedCode}>{children}</span>
      <span className={styles.tooltip}>
        <span className={styles.typeInfo}>
          <span className={styles.typeLabel}>Type:</span>
          <code className={styles.typeName}>{type}</code>
        </span>
        {description && (
          <span className={styles.description}>{description}</span>
        )}
      </span>
    </span>
  );
}
