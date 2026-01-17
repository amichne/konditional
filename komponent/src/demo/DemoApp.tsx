/**
 * Demo Application for Konditional Editor
 * 
 * Provides a working example with sample data for development and testing.
 */

import { useState, useCallback } from 'react';
import { KonditionalEditor } from '../components/KonditionalEditor';
import { generateSchemaFromSnapshot } from '../types/schema';
import type { Snapshot } from '../types/schema';
import '../styles/editor.css';

const SAMPLE_SNAPSHOT_BASE: Snapshot = {
  meta: {
    version: '1.0.0',
    generatedAtEpochMillis: Date.now(),
    source: 'demo',
  },
  flags: [
    // Boolean flag - simple feature toggle
    {
      type: 'BOOLEAN',
      key: 'feature::payments::new_checkout_flow',
      defaultValue: { type: 'BOOLEAN', value: false },
      salt: 'v1',
      isActive: true,
      rampUpAllowlist: [],
      rules: [
        {
          value: { type: 'BOOLEAN', value: true },
          rampUp: 100,
          rampUpAllowlist: [],
          note: 'Beta testers',
          locales: ['UNITED_STATES', 'CANADA'],
          platforms: ['IOS', 'ANDROID'],
          versionRange: { type: 'MIN_BOUND', min: { major: 2, minor: 5, patch: 0 } },
          axes: {},
        },
        {
          value: { type: 'BOOLEAN', value: true },
          rampUp: 25,
          rampUpAllowlist: [],
          note: 'Gradual rollout',
          locales: 'UNITED_STATES',
          platforms: ['IOS', 'ANDROID', 'WEB'],
          versionRange: { type: 'MIN_BOUND', min: { major: 2, minor: 0, patch: 0 } },
          axes: {},
        },
      ],
    },

    // String flag - configuration value
    {
      type: 'STRING',
      key: 'feature::messaging::welcome_message',
      defaultValue: { type: 'STRING', value: 'Welcome to our app!' },
      salt: 'v1',
      isActive: true,
      rampUpAllowlist: [],
      rules: [
        {
          value: { type: 'STRING', value: 'Bienvenue!' },
          rampUp: 100,
          rampUpAllowlist: [],
          locales: ['FRANCE', 'CANADA_FRENCH', 'BELGIUM_FRENCH'],
          platforms: ['IOS', 'ANDROID', 'WEB'],
          versionRange: { type: 'UNBOUNDED' },
          axes: {},
        },
        {
          value: { type: 'STRING', value: 'Willkommen!' },
          rampUp: 100,
          rampUpAllowlist: [],
          locales: ['GERMANY', 'AUSTRIA'],
          platforms: ['IOS', 'ANDROID', 'WEB'],
          versionRange: { type: 'UNBOUNDED' },
          axes: {},
        },
      ],
    },

    // Int flag - numeric configuration
    {
      type: 'INT',
      key: 'feature::feed::page_size',
      defaultValue: { type: 'INT', value: 20 },
      salt: 'v1',
      isActive: true,
      rampUpAllowlist: [],
      rules: [
        {
          value: { type: 'INT', value: 50 },
          rampUp: 100,
          rampUpAllowlist: [],
          note: 'Larger page size for web',
          locales: 'UNITED_STATES',
          platforms: ['WEB'],
          versionRange: { type: 'UNBOUNDED' },
          axes: {},
        },
      ],
    },

    // Double flag
    {
      type: 'DOUBLE',
      key: 'feature::pricing::discount_rate',
      defaultValue: { type: 'DOUBLE', value: 0.0 },
      salt: 'v2',
      isActive: true,
      rampUpAllowlist: [],
      rules: [
        {
          value: { type: 'DOUBLE', value: 0.15 },
          rampUp: 100,
          rampUpAllowlist: [],
          note: 'Holiday discount',
          locales: ['UNITED_STATES', 'CANADA', 'UNITED_KINGDOM'],
          platforms: ['IOS', 'ANDROID', 'WEB'],
          versionRange: { type: 'UNBOUNDED' },
          axes: {},
        },
      ],
    },

    // Enum flag - theme selection
    {
      type: 'ENUM',
      key: 'feature::ui::default_theme',
      defaultValue: { type: 'ENUM', value: 'SYSTEM', enumClassName: 'com.example.Theme' },
      salt: 'v1',
      isActive: true,
      rampUpAllowlist: [],
      rules: [
        {
          value: { type: 'ENUM', value: 'DARK', enumClassName: 'com.example.Theme' },
          rampUp: 50,
          rampUpAllowlist: [],
          note: 'A/B test dark mode default',
          locales: 'UNITED_STATES',
          platforms: ['IOS', 'ANDROID'],
          versionRange: { type: 'MIN_BOUND', min: { major: 3, minor: 0, patch: 0 } },
          axes: {},
        },
      ],
    },

    // DataClass flag - complex configuration
    {
      type: 'DATA_CLASS',
      key: 'feature::api::rate_limit_config',
      defaultValue: {
        type: 'DATA_CLASS',
        dataClassName: 'com.example.RateLimitConfig',
        value: {
          requestsPerMinute: 100,
          burstLimit: 20,
          enabled: true,
          tier: 'FREE',
        },
      },
      salt: 'v1',
      isActive: true,
      rampUpAllowlist: [],
      rules: [
        {
          value: {
            type: 'DATA_CLASS',
            dataClassName: 'com.example.RateLimitConfig',
            value: {
              requestsPerMinute: 1000,
              burstLimit: 100,
              enabled: true,
              tier: 'ENTERPRISE',
            },
          },
          rampUp: 100,
          rampUpAllowlist: [],
          note: 'Enterprise tier limits',
          locales: 'UNITED_STATES',
          platforms: ['IOS', 'ANDROID', 'WEB'],
          versionRange: { type: 'UNBOUNDED' },
          axes: { tier: ['ENTERPRISE'] },
        },
        {
          value: {
            type: 'DATA_CLASS',
            dataClassName: 'com.example.RateLimitConfig',
            value: {
              requestsPerMinute: 500,
              burstLimit: 50,
              enabled: true,
              tier: 'PRO',
            },
          },
          rampUp: 100,
          rampUpAllowlist: [],
          note: 'Pro tier limits',
          locales: 'UNITED_STATES',
          platforms: ['IOS', 'ANDROID', 'WEB'],
          versionRange: { type: 'UNBOUNDED' },
          axes: { tier: ['PRO'] },
        },
      ],
    },

    // Inactive flag (for testing inactive display)
    {
      type: 'BOOLEAN',
      key: 'feature::legacy::deprecated_feature',
      defaultValue: { type: 'BOOLEAN', value: false },
      salt: 'v1',
      isActive: false,
      rampUpAllowlist: [],
      rules: [
        {
          value: { type: 'BOOLEAN', value: true },
          rampUp: 100,
          rampUpAllowlist: [],
          locales: 'UNITED_STATES',
          platforms: ['WEB'],
          versionRange: { type: 'UNBOUNDED' },
          axes: {},
        },
      ],
    },

    // Flag with validation issues (for testing validation display)
    {
      type: 'BOOLEAN',
      key: 'feature::test::validation_example',
      defaultValue: { type: 'BOOLEAN', value: false },
      salt: 'v1',
      isActive: true,
      rampUpAllowlist: [],
      rules: [
        {
          value: { type: 'BOOLEAN', value: true },
          rampUp: 0,
          rampUpAllowlist: [], // 0% with no allowlist = warning
          locales: 'UNITED_STATES',
          platforms: ['IOS', 'ANDROID', 'WEB'],
          versionRange: {
            type: 'MIN_AND_MAX_BOUND',
            min: { major: 5, minor: 0, patch: 0 },
            max: { major: 3, minor: 0, patch: 0 }, // min > max = error
          },
          axes: {},
        },
      ],
    },
  ],
};

const SAMPLE_SNAPSHOT: Snapshot = {
  ...SAMPLE_SNAPSHOT_BASE,
  schema: generateSchemaFromSnapshot(SAMPLE_SNAPSHOT_BASE),
};

export function DemoApp(): JSX.Element {
  const [snapshot, setSnapshot] = useState<Snapshot>(SAMPLE_SNAPSHOT);
  const [savedSnapshots, setSavedSnapshots] = useState<Snapshot[]>([]);

  const handleSave = useCallback(async (newSnapshot: Snapshot) => {
    // Simulate async save
    await new Promise((resolve) => setTimeout(resolve, 500));
    
    console.log('Saving snapshot:', newSnapshot);
    setSnapshot(newSnapshot);
    setSavedSnapshots((prev) => [...prev, newSnapshot]);
    
    alert('Snapshot saved successfully!');
  }, []);

  const handleReset = useCallback(() => {
    setSnapshot(SAMPLE_SNAPSHOT);
    setSavedSnapshots([]);
  }, []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      {/* Demo controls */}
      <div style={{
        padding: '8px 16px',
        background: '#1e293b',
        color: '#e2e8f0',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        fontSize: '13px',
      }}>
        <span>
          <strong>Konditional Editor Demo</strong>
          {' • '}
          {snapshot.flags.length} flags
          {savedSnapshots.length > 0 && ` • ${savedSnapshots.length} save(s)`}
        </span>
        <button
          onClick={handleReset}
          style={{
            padding: '4px 12px',
            background: '#475569',
            color: '#e2e8f0',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '12px',
          }}
        >
          Reset Demo
        </button>
      </div>

      {/* Editor */}
      <div style={{ flex: 1, overflow: 'hidden' }}>
        <KonditionalEditor
          snapshot={snapshot}
          onSave={handleSave}
          onChange={(s) => console.log('Change:', s)}
        />
      </div>
    </div>
  );
}
