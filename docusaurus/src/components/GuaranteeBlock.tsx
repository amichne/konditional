import React from 'react';
import styles from './GuaranteeBlock.module.css';

interface GuaranteeBlockProps {
  label: string;
  guarantee?: string;
  mechanism?: string;
  boundary?: string;
}

export default function GuaranteeBlock({
  label,
  guarantee,
  mechanism,
  boundary
}: GuaranteeBlockProps): JSX.Element {
  return (
    <div className={styles.guaranteeBlock}>
      <div className={styles.label}>
        {label}
        <span className={styles.hoverHint}>ⓘ</span>
      </div>
      <div className={styles.details}>
        {guarantee && (
          <div className={styles.detailItem}>
            <strong>Guarantee:</strong> {guarantee}
          </div>
        )}
        {mechanism && (
          <div className={styles.detailItem}>
            <strong>Mechanism:</strong> {mechanism}
          </div>
        )}
        {boundary && (
          <div className={styles.detailItem}>
            <strong>Boundary:</strong> {boundary}
          </div>
        )}
      </div>
    </div>
  );
}
