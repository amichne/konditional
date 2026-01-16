/**
 * Flag Value Editor Components
 * Renders appropriate input for each flag value type
 */

import React from 'react';
import { FlagValue, FlagValueType, getFlagValueTypeLabel } from '@/types/konditional';
import { Input } from '@/components/ui/input';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { ToggleLeft, Type, Hash, Percent, List, Braces } from 'lucide-react';

interface FlagValueEditorProps {
  value: FlagValue;
  onChange: (value: FlagValue) => void;
  enumOptions?: string[];
  disabled?: boolean;
  compact?: boolean;
}

/** Icon for each value type */
export function ValueTypeIcon({ type, className }: { type: FlagValueType; className?: string }) {
  const iconClass = cn('h-4 w-4', className);
  
  switch (type) {
    case 'BOOLEAN':
      return <ToggleLeft className={iconClass} />;
    case 'STRING':
      return <Type className={iconClass} />;
    case 'INT':
      return <Hash className={iconClass} />;
    case 'DOUBLE':
      return <Percent className={iconClass} />;
    case 'ENUM':
      return <List className={iconClass} />;
    case 'DATA_CLASS':
      return <Braces className={iconClass} />;
  }
}

/** Badge showing the value type */
export function ValueTypeBadge({ type }: { type: FlagValueType }) {
  const colorMap: Record<FlagValueType, string> = {
    BOOLEAN: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20',
    STRING: 'bg-blue-500/10 text-blue-600 dark:text-blue-400 border-blue-500/20',
    INT: 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20',
    DOUBLE: 'bg-orange-500/10 text-orange-600 dark:text-orange-400 border-orange-500/20',
    ENUM: 'bg-purple-500/10 text-purple-600 dark:text-purple-400 border-purple-500/20',
    DATA_CLASS: 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border-rose-500/20',
  };

  return (
    <Badge variant="outline" className={cn('font-mono text-xs', colorMap[type])}>
      <ValueTypeIcon type={type} className="h-3 w-3 mr-1" />
      {getFlagValueTypeLabel(type)}
    </Badge>
  );
}

/** Boolean value editor */
function BooleanEditor({
  value,
  onChange,
  disabled,
  compact,
}: {
  value: boolean;
  onChange: (value: boolean) => void;
  disabled?: boolean;
  compact?: boolean;
}) {
  return (
    <div className={cn('flex items-center gap-3', compact ? '' : 'p-4 bg-muted/30 rounded-lg')}>
      <Switch
        checked={value}
        onCheckedChange={onChange}
        disabled={disabled}
        className="data-[state=checked]:bg-emerald-500"
      />
      <span className={cn(
        'font-medium transition-colors',
        value ? 'text-emerald-600 dark:text-emerald-400' : 'text-muted-foreground'
      )}>
        {value ? 'True' : 'False'}
      </span>
    </div>
  );
}

/** String value editor */
function StringEditor({
  value,
  onChange,
  disabled,
  multiline,
}: {
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  multiline?: boolean;
}) {
  if (multiline) {
    return (
      <Textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
        placeholder="Enter value..."
        className="font-mono text-sm min-h-[100px]"
      />
    );
  }
  
  return (
    <Input
      type="text"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      disabled={disabled}
      placeholder="Enter value..."
      className="font-mono"
    />
  );
}

/** Integer value editor */
function IntEditor({
  value,
  onChange,
  disabled,
}: {
  value: number;
  onChange: (value: number) => void;
  disabled?: boolean;
}) {
  return (
    <Input
      type="number"
      value={value}
      onChange={(e) => onChange(parseInt(e.target.value, 10) || 0)}
      disabled={disabled}
      step={1}
      className="font-mono max-w-[180px]"
    />
  );
}

/** Double value editor */
function DoubleEditor({
  value,
  onChange,
  disabled,
}: {
  value: number;
  onChange: (value: number) => void;
  disabled?: boolean;
}) {
  return (
    <Input
      type="number"
      value={value}
      onChange={(e) => onChange(parseFloat(e.target.value) || 0)}
      disabled={disabled}
      step={0.01}
      className="font-mono max-w-[180px]"
    />
  );
}

/** Enum value editor */
function EnumEditor({
  value,
  enumClassName,
  onChange,
  options = [],
  disabled,
}: {
  value: string;
  enumClassName: string;
  onChange: (value: string) => void;
  options?: string[];
  disabled?: boolean;
}) {
  return (
    <div className="space-y-2">
      <div className="text-xs text-muted-foreground font-mono">{enumClassName}</div>
      <Select value={value} onValueChange={onChange} disabled={disabled}>
        <SelectTrigger className="w-full max-w-[280px]">
          <SelectValue placeholder="Select value..." />
        </SelectTrigger>
        <SelectContent>
          {options.map((opt) => (
            <SelectItem key={opt} value={opt} className="font-mono">
              {opt}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}

/** Data class (JSON object) editor */
function DataClassEditor({
  value,
  dataClassName,
  onChange,
  disabled,
}: {
  value: Record<string, unknown>;
  dataClassName: string;
  onChange: (value: Record<string, unknown>) => void;
  disabled?: boolean;
}) {
  const [jsonText, setJsonText] = React.useState(() => JSON.stringify(value, null, 2));
  const [error, setError] = React.useState<string | null>(null);

  const handleChange = (text: string) => {
    setJsonText(text);
    try {
      const parsed = JSON.parse(text);
      setError(null);
      onChange(parsed);
    } catch {
      setError('Invalid JSON');
    }
  };

  return (
    <div className="space-y-2">
      <div className="text-xs text-muted-foreground font-mono">{dataClassName}</div>
      <Textarea
        value={jsonText}
        onChange={(e) => handleChange(e.target.value)}
        disabled={disabled}
        className={cn(
          'font-mono text-sm min-h-[150px]',
          error && 'border-destructive focus-visible:ring-destructive'
        )}
      />
      {error && (
        <p className="text-xs text-destructive">{error}</p>
      )}
    </div>
  );
}

/** Main flag value editor that delegates to the appropriate type-specific editor */
export function FlagValueEditor({
  value,
  onChange,
  enumOptions = [],
  disabled = false,
  compact = false,
}: FlagValueEditorProps) {
  switch (value.type) {
    case 'BOOLEAN':
      return (
        <BooleanEditor
          value={value.value}
          onChange={(v) => onChange({ ...value, value: v })}
          disabled={disabled}
          compact={compact}
        />
      );
    
    case 'STRING':
      return (
        <StringEditor
          value={value.value}
          onChange={(v) => onChange({ ...value, value: v })}
          disabled={disabled}
          multiline={value.value.length > 50}
        />
      );
    
    case 'INT':
      return (
        <IntEditor
          value={value.value}
          onChange={(v) => onChange({ ...value, value: v })}
          disabled={disabled}
        />
      );
    
    case 'DOUBLE':
      return (
        <DoubleEditor
          value={value.value}
          onChange={(v) => onChange({ ...value, value: v })}
          disabled={disabled}
        />
      );
    
    case 'ENUM':
      return (
        <EnumEditor
          value={value.value}
          enumClassName={value.enumClassName}
          onChange={(v) => onChange({ ...value, value: v })}
          options={enumOptions}
          disabled={disabled}
        />
      );
    
    case 'DATA_CLASS':
      return (
        <DataClassEditor
          value={value.value}
          dataClassName={value.dataClassName}
          onChange={(v) => onChange({ ...value, value: v })}
          disabled={disabled}
        />
      );
  }
}

/** Compact display of a flag value (for read-only preview) */
export function FlagValueDisplay({ value }: { value: FlagValue }) {
  switch (value.type) {
    case 'BOOLEAN':
      return (
        <span className={cn(
          'font-mono font-medium',
          value.value ? 'text-emerald-600 dark:text-emerald-400' : 'text-muted-foreground'
        )}>
          {value.value ? 'true' : 'false'}
        </span>
      );
    
    case 'STRING':
      return (
        <span className="font-mono text-blue-600 dark:text-blue-400 truncate max-w-[200px] inline-block">
          "{value.value}"
        </span>
      );
    
    case 'INT':
    case 'DOUBLE':
      return (
        <span className="font-mono text-amber-600 dark:text-amber-400">
          {value.value}
        </span>
      );
    
    case 'ENUM':
      return (
        <span className="font-mono text-purple-600 dark:text-purple-400">
          {value.value}
        </span>
      );
    
    case 'DATA_CLASS':
      return (
        <span className="font-mono text-rose-600 dark:text-rose-400 text-xs">
          {JSON.stringify(value.value).slice(0, 50)}...
        </span>
      );
  }
}
