/**
 * Mock data for Konditional configuration demo
 */

import {
  SerializableFlag,
  SerializableSnapshot,
  ALL_LOCALES,
  ALL_PLATFORMS,
  Locale,
} from '@/types/konditional';

// Example boolean feature flag
const darkModeFlag: SerializableFlag = {
  key: 'feature::ui::dark_mode_enabled',
  defaultValue: { type: 'BOOLEAN', value: false },
  salt: 'v1',
  isActive: true,
  rampUpAllowlist: [],
  rules: [
    {
      value: { type: 'BOOLEAN', value: true },
      rampUp: 100,
      rampUpAllowlist: [],
      note: 'Enable for all iOS users',
      locales: [...ALL_LOCALES],
      platforms: ['IOS'],
      versionRange: { type: 'UNBOUNDED' },
      axes: {},
    },
    {
      value: { type: 'BOOLEAN', value: true },
      rampUp: 50,
      rampUpAllowlist: ['beta-tester-1', 'beta-tester-2'],
      note: 'Gradual rollout for Android',
      locales: [...ALL_LOCALES],
      platforms: ['ANDROID'],
      versionRange: { type: 'MIN_BOUND', min: { major: 5, minor: 0, patch: 0 } },
      axes: {},
    },
  ],
};

// Example string flag with localized content
const welcomeMessageFlag: SerializableFlag = {
  key: 'feature::onboarding::welcome_message',
  defaultValue: { type: 'STRING', value: 'Welcome to our app!' },
  salt: 'v1',
  isActive: true,
  rampUpAllowlist: [],
  rules: [
    {
      value: { type: 'STRING', value: 'G\'day mate! Welcome to the app!' },
      rampUp: 100,
      rampUpAllowlist: [],
      note: 'Australian localization',
      locales: ['AUSTRALIA', 'NEW_ZEALAND'],
      platforms: [...ALL_PLATFORMS],
      versionRange: { type: 'UNBOUNDED' },
      axes: {},
    },
    {
      value: { type: 'STRING', value: 'Bienvenue dans notre application!' },
      rampUp: 100,
      rampUpAllowlist: [],
      note: 'French localization',
      locales: ['FRANCE', 'CANADA_FRENCH', 'BELGIUM_FRENCH'],
      platforms: [...ALL_PLATFORMS],
      versionRange: { type: 'UNBOUNDED' },
      axes: {},
    },
  ],
};

// Example integer flag for configuration
const maxRetryCountFlag: SerializableFlag = {
  key: 'feature::networking::max_retry_count',
  defaultValue: { type: 'INT', value: 3 },
  salt: 'v1',
  isActive: true,
  rampUpAllowlist: [],
  rules: [
    {
      value: { type: 'INT', value: 5 },
      rampUp: 100,
      rampUpAllowlist: [],
      note: 'More retries for regions with poor connectivity',
      locales: ['INDIA', 'MEXICO'],
      platforms: [...ALL_PLATFORMS],
      versionRange: { type: 'UNBOUNDED' },
      axes: {},
    },
  ],
};

// Example double flag for thresholds
const animationSpeedFlag: SerializableFlag = {
  key: 'feature::ui::animation_speed_multiplier',
  defaultValue: { type: 'DOUBLE', value: 1.0 },
  salt: 'v1',
  isActive: true,
  rampUpAllowlist: [],
  rules: [
    {
      value: { type: 'DOUBLE', value: 0.5 },
      rampUp: 100,
      rampUpAllowlist: [],
      note: 'Slower animations for older devices',
      locales: [...ALL_LOCALES],
      platforms: ['ANDROID'],
      versionRange: { type: 'MAX_BOUND', max: { major: 3, minor: 0, patch: 0 } },
      axes: {},
    },
  ],
};

// Example enum flag
const paymentProviderFlag: SerializableFlag = {
  key: 'feature::payments::provider',
  defaultValue: { type: 'ENUM', value: 'STRIPE', enumClassName: 'PaymentProvider' },
  salt: 'v1',
  isActive: true,
  rampUpAllowlist: [],
  rules: [
    {
      value: { type: 'ENUM', value: 'ADYEN', enumClassName: 'PaymentProvider' },
      rampUp: 100,
      rampUpAllowlist: [],
      note: 'Use Adyen in European markets',
      locales: [
        'AUSTRIA', 'BELGIUM_DUTCH', 'BELGIUM_FRENCH', 'FINLAND', 
        'FRANCE', 'GERMANY', 'ITALY', 'NETHERLANDS', 'NORWAY', 
        'SPAIN', 'SWEDEN', 'UNITED_KINGDOM'
      ],
      platforms: [...ALL_PLATFORMS],
      versionRange: { type: 'UNBOUNDED' },
      axes: {},
    },
  ],
};

// Example data class flag for complex config
const experimentConfigFlag: SerializableFlag = {
  key: 'feature::experiment::checkout_flow_config',
  defaultValue: {
    type: 'DATA_CLASS',
    dataClassName: 'CheckoutFlowConfig',
    value: {
      showProgressBar: true,
      skipReviewStep: false,
      enableOneClick: false,
      maxItemsPerOrder: 100,
    },
  },
  salt: 'v1',
  isActive: true,
  rampUpAllowlist: [],
  rules: [
    {
      value: {
        type: 'DATA_CLASS',
        dataClassName: 'CheckoutFlowConfig',
        value: {
          showProgressBar: true,
          skipReviewStep: true,
          enableOneClick: true,
          maxItemsPerOrder: 50,
        },
      },
      rampUp: 25,
      rampUpAllowlist: ['power-user-1'],
      note: 'Streamlined checkout experiment',
      locales: ['UNITED_STATES'],
      platforms: ['WEB'],
      versionRange: { type: 'UNBOUNDED' },
      axes: {
        'userTier': ['premium', 'enterprise'],
      },
    },
  ],
};

// Inactive flag example
const legacyFeatureFlag: SerializableFlag = {
  key: 'feature::legacy::old_navigation',
  defaultValue: { type: 'BOOLEAN', value: true },
  salt: 'v1',
  isActive: false,
  rampUpAllowlist: [],
  rules: [],
};

export const mockSnapshot: SerializableSnapshot = {
  meta: {
    version: '2024.12.001',
    generatedAtEpochMillis: Date.now(),
    source: 'config-service',
  },
  flags: [
    darkModeFlag,
    welcomeMessageFlag,
    maxRetryCountFlag,
    animationSpeedFlag,
    paymentProviderFlag,
    experimentConfigFlag,
    legacyFeatureFlag,
  ],
};

// Enum options for dropdowns
export const paymentProviderOptions = ['STRIPE', 'ADYEN', 'BRAINTREE', 'PAYPAL'];

// Locale region groupings for UI
export const localeRegions: Record<string, Locale[]> = {
  'North America': ['UNITED_STATES', 'CANADA', 'CANADA_FRENCH', 'MEXICO'],
  'Europe': [
    'UNITED_KINGDOM', 'FRANCE', 'GERMANY', 'ITALY', 'SPAIN',
    'NETHERLANDS', 'BELGIUM_DUTCH', 'BELGIUM_FRENCH', 'AUSTRIA',
    'FINLAND', 'NORWAY', 'SWEDEN', 'ICC_EN_EU', 'ICC_EN_EI'
  ],
  'Asia Pacific': [
    'AUSTRALIA', 'NEW_ZEALAND', 'JAPAN', 'HONG_KONG', 
    'HONG_KONG_ENGLISH', 'SINGAPORE', 'TAIWAN', 'INDIA'
  ],
};

// Helper to get a single flag by key
export function getFlagByKey(key: string): SerializableFlag | undefined {
  return mockSnapshot.flags.find(f => f.key === key);
}

// Helper to group flags by namespace
export function getFlagsByNamespace(): Record<string, SerializableFlag[]> {
  const groups: Record<string, SerializableFlag[]> = {};
  
  for (const flag of mockSnapshot.flags) {
    const match = flag.key.match(/^feature::([^:]+)::/);
    const namespace = match ? match[1] : 'other';
    
    if (!groups[namespace]) {
      groups[namespace] = [];
    }
    groups[namespace].push(flag);
  }
  
  return groups;
}
