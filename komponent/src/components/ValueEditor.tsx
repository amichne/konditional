/**
 * ValueEditor Component
 * 
 * Type-discriminated editor for flag values.
 * Renders the appropriate input control based on the value's type discriminator.
 * 
 * DESIGN: Each value type gets a dedicated editor that enforces its constraints.
 * The parent component is responsible for ensuring type consistency (e.g., not
 * trying to set a STRING value on a BOOLEAN flag).
 */

import { useCallback, useMemo } from 'react';
import type {
  FlagValue,
  BooleanFlagValue,
  StringFlagValue,
  IntFlagValue,
  DoubleFlagValue,
  EnumFlagValue,
  DataClassFlagValue,
  SchemaMetadata,
  DataClassFieldSchema,
} from '../types/schema';

// =============================================================================
// Main ValueEditor Component
// =============================================================================

interface ValueEditorProps {
  value: FlagValue;
  onChange: (value: FlagValue) => void;
  schema?: SchemaMetadata;
  /** Optional: render in compact mode for inline display */
  compact?: boolean;
}

export function ValueEditor({
  value,
  onChange,
  schema,
  compact = false,
}: ValueEditorProps): JSX.Element {
  switch (value.type) {
    case 'BOOLEAN':
      return (
        <BooleanEditor
          value={value}
          onChange={onChange as (v: BooleanFlagValue) => void}
          compact={compact}
        />
      );

    case 'STRING':
      return (
        <StringEditor
          value={value}
          onChange={onChange as (v: StringFlagValue) => void}
          compact={compact}
        />
      );

    case 'INT':
      return (
        <IntEditor
          value={value}
          onChange={onChange as (v: IntFlagValue) => void}
          compact={compact}
        />
      );

    case 'DOUBLE':
      return (
        <DoubleEditor
          value={value}
          onChange={onChange as (v: DoubleFlagValue) => void}
          compact={compact}
        />
      );

    case 'ENUM':
      return (
        <EnumEditor
          value={value}
          onChange={onChange as (v: EnumFlagValue) => void}
          schema={schema}
          compact={compact}
        />
      );

    case 'DATA_CLASS':
      return (
        <DataClassEditor
          value={value}
          onChange={onChange as (v: DataClassFlagValue) => void}
          schema={schema}
          compact={compact}
        />
      );

    default: {
      // Exhaustiveness check - this should never happen
      const _exhaustive: never = value;
      void _exhaustive;
      return <span className="ke-value-unknown">Unknown type</span>;
    }
  }
}

// =============================================================================
// Boolean Editor
// =============================================================================

interface BooleanEditorProps {
  value: BooleanFlagValue;
  onChange: (value: BooleanFlagValue) => void;
  compact?: boolean;
}

function BooleanEditor({ value, onChange, compact }: BooleanEditorProps): JSX.Element {
  const handleChange = useCallback(
    (checked: boolean) => {
      onChange({ type: 'BOOLEAN', value: checked });
    },
    [onChange]
  );

  if (compact) {
    return (
      <span className={`ke-value-boolean ${value.value ? 'ke-value-true' : 'ke-value-false'}`}>
        {value.value ? 'true' : 'false'}
      </span>
    );
  }

  return (
    <label className="ke-boolean-editor">
      <input
        type="checkbox"
        checked={value.value}
        onChange={(e) => handleChange(e.target.checked)}
        className="ke-checkbox"
      />
      <span className="ke-boolean-label">
        {value.value ? 'true' : 'false'}
      </span>
    </label>
  );
}

// =============================================================================
// String Editor
// =============================================================================

interface StringEditorProps {
  value: StringFlagValue;
  onChange: (value: StringFlagValue) => void;
  compact?: boolean;
}

function StringEditor({ value, onChange, compact }: StringEditorProps): JSX.Element {
  const handleChange = useCallback(
    (newValue: string) => {
      onChange({ type: 'STRING', value: newValue });
    },
    [onChange]
  );

  if (compact) {
    return (
      <span className="ke-value-string" title={value.value}>
        "{value.value.length > 30 ? value.value.slice(0, 30) + '...' : value.value}"
      </span>
    );
  }

  return (
    <input
      type="text"
      value={value.value}
      onChange={(e) => handleChange(e.target.value)}
      className="ke-input ke-string-editor"
      placeholder="Enter string value"
    />
  );
}

// =============================================================================
// Int Editor
// =============================================================================

interface IntEditorProps {
  value: IntFlagValue;
  onChange: (value: IntFlagValue) => void;
  compact?: boolean;
}

function IntEditor({ value, onChange, compact }: IntEditorProps): JSX.Element {
  const handleChange = useCallback(
    (newValue: number) => {
      // Ensure integer
      const intValue = Math.trunc(newValue);
      onChange({ type: 'INT', value: intValue });
    },
    [onChange]
  );

  if (compact) {
    return <span className="ke-value-int">{value.value}</span>;
  }

  return (
    <input
      type="number"
      value={value.value}
      onChange={(e) => handleChange(parseFloat(e.target.value) || 0)}
      step={1}
      className="ke-input ke-number-editor"
    />
  );
}

// =============================================================================
// Double Editor
// =============================================================================

interface DoubleEditorProps {
  value: DoubleFlagValue;
  onChange: (value: DoubleFlagValue) => void;
  compact?: boolean;
}

function DoubleEditor({ value, onChange, compact }: DoubleEditorProps): JSX.Element {
  const handleChange = useCallback(
    (newValue: number) => {
      onChange({ type: 'DOUBLE', value: newValue });
    },
    [onChange]
  );

  if (compact) {
    return <span className="ke-value-double">{value.value}</span>;
  }

  return (
    <input
      type="number"
      value={value.value}
      onChange={(e) => handleChange(parseFloat(e.target.value) || 0)}
      step={0.01}
      className="ke-input ke-number-editor"
    />
  );
}

// =============================================================================
// Enum Editor
// =============================================================================

interface EnumEditorProps {
  value: EnumFlagValue;
  onChange: (value: EnumFlagValue) => void;
  schema?: SchemaMetadata;
  compact?: boolean;
}

function EnumEditor({ value, onChange, schema, compact }: EnumEditorProps): JSX.Element {
  // Get valid enum values from schema
  const validValues = useMemo(() => {
    if (!schema?.enums) return null;
    return schema.enums[value.enumClassName] ?? null;
  }, [schema, value.enumClassName]);

  const handleChange = useCallback(
    (newValue: string) => {
      onChange({
        type: 'ENUM',
        value: newValue,
        enumClassName: value.enumClassName,
      });
    },
    [onChange, value.enumClassName]
  );

  if (compact) {
    return (
      <span className="ke-value-enum" title={value.enumClassName}>
        {value.value}
      </span>
    );
  }

  if (!validValues) {
    // Schema not available - render as text input with warning
    return (
      <div className="ke-enum-editor ke-enum-editor--no-schema">
        <input
          type="text"
          value={value.value}
          onChange={(e) => handleChange(e.target.value)}
          className="ke-input"
        />
        <span className="ke-enum-warning">
          Schema for {value.enumClassName} not found
        </span>
      </div>
    );
  }

  return (
    <div className="ke-enum-editor">
      <select
        value={value.value}
        onChange={(e) => handleChange(e.target.value)}
        className="ke-select"
      >
        {!validValues.includes(value.value) && (
          <option value={value.value} disabled>
            {value.value} (invalid)
          </option>
        )}
        {validValues.map((enumValue) => (
          <option key={enumValue} value={enumValue}>
            {enumValue}
          </option>
        ))}
      </select>
      <span className="ke-enum-class-name" title={value.enumClassName}>
        {value.enumClassName.split('.').pop()}
      </span>
    </div>
  );
}

// =============================================================================
// DataClass Editor
// =============================================================================

interface DataClassEditorProps {
  value: DataClassFlagValue;
  onChange: (value: DataClassFlagValue) => void;
  schema?: SchemaMetadata;
  compact?: boolean;
}

function DataClassEditor({
  value,
  onChange,
  schema,
  compact,
}: DataClassEditorProps): JSX.Element {
  // Get data class schema
  const classSchema = useMemo(() => {
    if (!schema?.dataClasses) return null;
    return schema.dataClasses[value.dataClassName] ?? null;
  }, [schema, value.dataClassName]);

  const handleFieldChange = useCallback(
    (fieldName: string, fieldValue: unknown) => {
      onChange({
        type: 'DATA_CLASS',
        dataClassName: value.dataClassName,
        value: {
          ...value.value,
          [fieldName]: fieldValue,
        },
      });
    },
    [onChange, value.dataClassName, value.value]
  );

  if (compact) {
    return (
      <span className="ke-value-dataclass" title={JSON.stringify(value.value, null, 2)}>
        {value.dataClassName.split('.').pop()} {'{ ... }'}
      </span>
    );
  }

  if (!classSchema) {
    // No schema - render as JSON editor
    return (
      <div className="ke-dataclass-editor ke-dataclass-editor--no-schema">
        <span className="ke-dataclass-warning">
          Schema for {value.dataClassName} not found. Editing as raw JSON.
        </span>
        <JsonEditor
          value={value.value}
          onChange={(newValue) =>
            onChange({
              type: 'DATA_CLASS',
              dataClassName: value.dataClassName,
              value: newValue as Record<string, unknown>,
            })
          }
        />
      </div>
    );
  }

  return (
    <div className="ke-dataclass-editor">
      <span className="ke-dataclass-name">
        {value.dataClassName.split('.').pop()}
      </span>
      <div className="ke-dataclass-fields">
        {Object.entries(classSchema.properties).map(([fieldName, fieldSchema]) => (
          <DataClassFieldEditor
            key={fieldName}
            fieldName={fieldName}
            fieldSchema={fieldSchema}
            value={value.value[fieldName]}
            required={classSchema.required.includes(fieldName)}
            onChange={(newValue) => handleFieldChange(fieldName, newValue)}
          />
        ))}
      </div>
    </div>
  );
}

// =============================================================================
// DataClass Field Editor
// =============================================================================

interface DataClassFieldEditorProps {
  fieldName: string;
  fieldSchema: DataClassFieldSchema;
  value: unknown;
  required: boolean;
  onChange: (value: unknown) => void;
}

function DataClassFieldEditor({
  fieldName,
  fieldSchema,
  value,
  required,
  onChange,
}: DataClassFieldEditorProps): JSX.Element {
  // Render appropriate input based on field type
  const renderInput = () => {
    switch (fieldSchema.type) {
      case 'boolean':
        return (
          <input
            type="checkbox"
            checked={Boolean(value)}
            onChange={(e) => onChange(e.target.checked)}
            className="ke-checkbox"
          />
        );

      case 'integer':
      case 'number':
        return (
          <input
            type="number"
            value={typeof value === 'number' ? value : ''}
            onChange={(e) => {
              const num = fieldSchema.type === 'integer'
                ? parseInt(e.target.value, 10)
                : parseFloat(e.target.value);
              onChange(isNaN(num) ? null : num);
            }}
            step={fieldSchema.type === 'integer' ? 1 : 0.01}
            min={fieldSchema.minimum}
            max={fieldSchema.maximum}
            className="ke-input ke-input-sm"
          />
        );

      case 'string':
        if (fieldSchema.enum) {
          return (
            <select
              value={String(value ?? '')}
              onChange={(e) => onChange(e.target.value)}
              className="ke-select ke-select-sm"
            >
              <option value="">—</option>
              {fieldSchema.enum.map((opt) => (
                <option key={opt} value={opt}>
                  {opt}
                </option>
              ))}
            </select>
          );
        }
        return (
          <input
            type="text"
            value={String(value ?? '')}
            onChange={(e) => onChange(e.target.value)}
            minLength={fieldSchema.minLength}
            maxLength={fieldSchema.maxLength}
            pattern={fieldSchema.pattern}
            className="ke-input ke-input-sm"
          />
        );

      case 'object':
      case 'array':
        // Complex types - render as JSON
        return (
          <JsonEditor
            value={value}
            onChange={onChange}
          />
        );

      default:
        return (
          <input
            type="text"
            value={String(value ?? '')}
            onChange={(e) => onChange(e.target.value)}
            className="ke-input ke-input-sm"
          />
        );
    }
  };

  return (
    <div className="ke-dataclass-field">
      <label className="ke-dataclass-field-label">
        {fieldName}
        {required && <span className="ke-required-indicator">*</span>}
      </label>
      <div className="ke-dataclass-field-input">{renderInput()}</div>
    </div>
  );
}

// =============================================================================
// JSON Editor (fallback for complex/unknown types)
// =============================================================================

interface JsonEditorProps {
  value: unknown;
  onChange: (value: unknown) => void;
}

function JsonEditor({ value, onChange }: JsonEditorProps): JSX.Element {
  const jsonString = useMemo(() => {
    try {
      return JSON.stringify(value, null, 2);
    } catch {
      return String(value);
    }
  }, [value]);

  const handleChange = useCallback(
    (text: string) => {
      try {
        const parsed = JSON.parse(text);
        onChange(parsed);
      } catch {
        // Invalid JSON - don't update
      }
    },
    [onChange]
  );

  return (
    <textarea
      value={jsonString}
      onChange={(e) => handleChange(e.target.value)}
      className="ke-textarea ke-json-editor"
      rows={Math.min(10, Math.max(3, jsonString.split('\n').length))}
      spellCheck={false}
    />
  );
}

export { BooleanEditor, StringEditor, IntEditor, DoubleEditor, EnumEditor, DataClassEditor };
