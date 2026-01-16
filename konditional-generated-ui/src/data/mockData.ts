import { Environment, Role } from '@/lib/store';

// Application data
export interface Application {
  id: string;
  name: string;
  description: string;
  owner: string;
  createdAt: string;
  updatedAt: string;
  configVersions: ConfigVersion[];
}

export interface ConfigVersion {
  id: string;
  version: string;
  status: 'draft' | 'pending' | 'approved' | 'published' | 'rejected';
  createdAt: string;
  createdBy: string;
  approvedBy?: string;
  publishedAt?: string;
}

// Configuration schema types
export type FieldType = 'string' | 'number' | 'boolean' | 'enum' | 'array' | 'object';

export interface FieldConstraint {
  min?: number;
  max?: number;
  minLength?: number;
  maxLength?: number;
  pattern?: string;
  enumValues?: string[];
}

export interface RequiredRule {
  environments: Environment[];
  roles?: Role[];
  condition?: string; // Field path for conditional requirement
}

export interface ConfigField {
  id: string;
  name: string;
  label: string;
  type: FieldType;
  description?: string;
  helpText?: string;
  placeholder?: string;
  defaultValue?: any;
  defaultsByEnvironment?: Partial<Record<Environment, any>>;
  required?: RequiredRule;
  constraints?: FieldConstraint;
  visibleWhen?: { field: string; value: any }; // Conditional visibility
  enabledWhen?: { field: string; value: any }; // Conditional enablement
}

export interface ConfigSection {
  id: string;
  name: string;
  label: string;
  description?: string;
  collapsible?: boolean;
  defaultCollapsed?: boolean;
  visibleWhen?: { field: string; value: any };
  fields: ConfigField[];
}

export interface ConfigSchema {
  id: string;
  name: string;
  version: string;
  sections: ConfigSection[];
}

// Config values
export interface ConfigValue {
  [key: string]: any;
}

// Mock applications
export const mockApplications: Application[] = [
  {
    id: 'app-1',
    name: 'Payment Gateway',
    description: 'Core payment processing service',
    owner: 'Platform Team',
    createdAt: '2024-01-15T10:00:00Z',
    updatedAt: '2024-12-20T15:30:00Z',
    configVersions: [
      {
        id: 'v3',
        version: '3.0.0',
        status: 'published',
        createdAt: '2024-12-15T10:00:00Z',
        createdBy: 'admin@example.com',
        approvedBy: 'lead@example.com',
        publishedAt: '2024-12-18T14:00:00Z',
      },
      {
        id: 'v4',
        version: '4.0.0-draft',
        status: 'draft',
        createdAt: '2024-12-20T15:30:00Z',
        createdBy: 'admin@example.com',
      },
    ],
  },
  {
    id: 'app-2',
    name: 'User Authentication',
    description: 'OAuth and identity management service',
    owner: 'Security Team',
    createdAt: '2024-02-10T09:00:00Z',
    updatedAt: '2024-12-19T11:00:00Z',
    configVersions: [
      {
        id: 'v2',
        version: '2.1.0',
        status: 'published',
        createdAt: '2024-11-01T10:00:00Z',
        createdBy: 'security@example.com',
        approvedBy: 'lead@example.com',
        publishedAt: '2024-11-05T12:00:00Z',
      },
      {
        id: 'v3',
        version: '3.0.0',
        status: 'pending',
        createdAt: '2024-12-19T11:00:00Z',
        createdBy: 'security@example.com',
      },
    ],
  },
  {
    id: 'app-3',
    name: 'Email Service',
    description: 'Transactional email and notification service',
    owner: 'Communications Team',
    createdAt: '2024-03-05T14:00:00Z',
    updatedAt: '2024-12-18T16:45:00Z',
    configVersions: [
      {
        id: 'v1',
        version: '1.5.0',
        status: 'published',
        createdAt: '2024-10-20T10:00:00Z',
        createdBy: 'comms@example.com',
        approvedBy: 'lead@example.com',
        publishedAt: '2024-10-25T09:00:00Z',
      },
    ],
  },
];

// Mock schema for Payment Gateway
export const paymentGatewaySchema: ConfigSchema = {
  id: 'schema-payment',
  name: 'Payment Gateway Configuration',
  version: '4.0.0',
  sections: [
    {
      id: 'general',
      name: 'general',
      label: 'General Settings',
      description: 'Basic configuration for the payment gateway',
      fields: [
        {
          id: 'serviceName',
          name: 'serviceName',
          label: 'Service Name',
          type: 'string',
          description: 'Display name for the payment service',
          placeholder: 'Enter service name',
          required: {
            environments: ['development', 'staging', 'production'],
          },
          constraints: {
            minLength: 3,
            maxLength: 50,
          },
        },
        {
          id: 'enabled',
          name: 'enabled',
          label: 'Enable Service',
          type: 'boolean',
          description: 'Toggle to enable or disable the payment service',
          defaultValue: true,
        },
        {
          id: 'logLevel',
          name: 'logLevel',
          label: 'Log Level',
          type: 'enum',
          description: 'Logging verbosity level',
          defaultValue: 'info',
          defaultsByEnvironment: {
            development: 'debug',
            staging: 'info',
            production: 'warn',
          },
          constraints: {
            enumValues: ['debug', 'info', 'warn', 'error'],
          },
        },
      ],
    },
    {
      id: 'api',
      name: 'api',
      label: 'API Configuration',
      description: 'API endpoint and authentication settings',
      collapsible: true,
      fields: [
        {
          id: 'apiEndpoint',
          name: 'apiEndpoint',
          label: 'API Endpoint',
          type: 'string',
          description: 'Base URL for the payment API',
          placeholder: 'https://api.example.com',
          required: {
            environments: ['staging', 'production'],
          },
          constraints: {
            pattern: '^https?:\\/\\/.+',
          },
        },
        {
          id: 'apiTimeout',
          name: 'apiTimeout',
          label: 'API Timeout (ms)',
          type: 'number',
          description: 'Request timeout in milliseconds',
          defaultValue: 5000,
          constraints: {
            min: 1000,
            max: 30000,
          },
        },
        {
          id: 'retryEnabled',
          name: 'retryEnabled',
          label: 'Enable Retries',
          type: 'boolean',
          description: 'Automatically retry failed requests',
          defaultValue: true,
        },
        {
          id: 'maxRetries',
          name: 'maxRetries',
          label: 'Max Retries',
          type: 'number',
          description: 'Maximum number of retry attempts',
          defaultValue: 3,
          visibleWhen: { field: 'retryEnabled', value: true },
          constraints: {
            min: 1,
            max: 10,
          },
        },
      ],
    },
    {
      id: 'security',
      name: 'security',
      label: 'Security Settings',
      description: 'Security and encryption configuration',
      collapsible: true,
      fields: [
        {
          id: 'encryptionEnabled',
          name: 'encryptionEnabled',
          label: 'Enable Encryption',
          type: 'boolean',
          description: 'Encrypt sensitive data in transit',
          defaultValue: true,
          required: {
            environments: ['production'],
          },
        },
        {
          id: 'encryptionAlgorithm',
          name: 'encryptionAlgorithm',
          label: 'Encryption Algorithm',
          type: 'enum',
          description: 'Encryption algorithm to use',
          defaultValue: 'AES-256',
          visibleWhen: { field: 'encryptionEnabled', value: true },
          constraints: {
            enumValues: ['AES-128', 'AES-256', 'RSA-2048', 'RSA-4096'],
          },
        },
        {
          id: 'allowedOrigins',
          name: 'allowedOrigins',
          label: 'Allowed Origins',
          type: 'array',
          description: 'CORS allowed origins',
          helpText: 'Enter comma-separated domain names',
          placeholder: 'https://example.com, https://app.example.com',
        },
      ],
    },
    {
      id: 'rateLimit',
      name: 'rateLimit',
      label: 'Rate Limiting',
      description: 'Request rate limiting configuration',
      collapsible: true,
      defaultCollapsed: true,
      visibleWhen: { field: 'enabled', value: true },
      fields: [
        {
          id: 'rateLimitEnabled',
          name: 'rateLimitEnabled',
          label: 'Enable Rate Limiting',
          type: 'boolean',
          description: 'Limit the number of requests per client',
          defaultValue: true,
        },
        {
          id: 'requestsPerMinute',
          name: 'requestsPerMinute',
          label: 'Requests per Minute',
          type: 'number',
          description: 'Maximum requests allowed per minute per client',
          defaultValue: 100,
          defaultsByEnvironment: {
            development: 1000,
            staging: 500,
            production: 100,
          },
          visibleWhen: { field: 'rateLimitEnabled', value: true },
          constraints: {
            min: 10,
            max: 10000,
          },
        },
        {
          id: 'burstLimit',
          name: 'burstLimit',
          label: 'Burst Limit',
          type: 'number',
          description: 'Allow temporary burst above limit',
          defaultValue: 150,
          visibleWhen: { field: 'rateLimitEnabled', value: true },
          constraints: {
            min: 0,
            max: 1000,
          },
        },
      ],
    },
  ],
};

// Mock config values
export const mockConfigValues: ConfigValue = {
  serviceName: 'Production Payment Gateway',
  enabled: true,
  logLevel: 'warn',
  apiEndpoint: 'https://api.payments.example.com',
  apiTimeout: 5000,
  retryEnabled: true,
  maxRetries: 3,
  encryptionEnabled: true,
  encryptionAlgorithm: 'AES-256',
  allowedOrigins: ['https://app.example.com', 'https://checkout.example.com'],
  rateLimitEnabled: true,
  requestsPerMinute: 100,
  burstLimit: 150,
};

// Large dataset for virtualization demo
export const generateLargeOptionSet = (count: number) => {
  return Array.from({ length: count }, (_, i) => ({
    id: `option-${i}`,
    label: `Option ${i + 1}`,
    description: `Description for option ${i + 1}`,
    category: ['Category A', 'Category B', 'Category C', 'Category D'][i % 4],
  }));
};

export const largeOptions = generateLargeOptionSet(10000);
