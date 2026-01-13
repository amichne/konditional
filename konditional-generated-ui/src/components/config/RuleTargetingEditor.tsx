/**
 * Rule Targeting Editor
 * Clean UI for configuring locales, platforms, versions, ramp-up, and axes
 */

import React, { useState } from 'react';
import {
  SerializableRule,
  FlagValue,
  Locale,
  Platform,
  VersionRange,
  Version,
  ALL_LOCALES,
  ALL_PLATFORMS,
  LOCALE_LABELS,
  PLATFORM_LABELS,
  formatVersionRange,
  hasAllLocales,
  hasAllPlatforms,
} from '@/types/konditional';
import { localeRegions } from '@/data/konditionalMockData';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Slider } from '@/components/ui/slider';
import { Badge } from '@/components/ui/badge';
import { Checkbox } from '@/components/ui/checkbox';
import { Textarea } from '@/components/ui/textarea';
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { cn } from '@/lib/utils';
import {
  ChevronDown,
  Globe,
  Smartphone,
  GitBranch,
  Users,
  Sliders,
  Check,
  X,
  Plus,
  Trash2,
} from 'lucide-react';

interface RuleTargetingEditorProps {
  rule: SerializableRule;
  onChange: (rule: SerializableRule) => void;
  disabled?: boolean;
}

/** Section wrapper with collapsible header */
function TargetingSection({
  icon: Icon,
  title,
  summary,
  children,
  defaultOpen = false,
  hasCustomization = false,
}: {
  icon: React.ElementType;
  title: string;
  summary: string;
  children: React.ReactNode;
  defaultOpen?: boolean;
  hasCustomization?: boolean;
}) {
  const [open, setOpen] = useState(defaultOpen);

  return (
    <Collapsible open={open} onOpenChange={setOpen}>
      <CollapsibleTrigger asChild>
        <button className={cn(
          'w-full flex items-center justify-between p-4 rounded-lg transition-colors',
          'hover:bg-muted/50 text-left',
          open && 'bg-muted/30'
        )}>
          <div className="flex items-center gap-3">
            <div className={cn(
              'p-2 rounded-md',
              hasCustomization ? 'bg-primary/10 text-primary' : 'bg-muted text-muted-foreground'
            )}>
              <Icon className="h-4 w-4" />
            </div>
            <div>
              <div className="font-medium text-sm">{title}</div>
              <div className="text-xs text-muted-foreground">{summary}</div>
            </div>
          </div>
          <ChevronDown className={cn(
            'h-4 w-4 text-muted-foreground transition-transform',
            open && 'rotate-180'
          )} />
        </button>
      </CollapsibleTrigger>
      <CollapsibleContent>
        <div className="px-4 pb-4 pt-2">
          {children}
        </div>
      </CollapsibleContent>
    </Collapsible>
  );
}

/** Locale selection with region grouping */
function LocaleEditor({
  locales,
  onChange,
  disabled,
}: {
  locales: Locale[];
  onChange: (locales: Locale[]) => void;
  disabled?: boolean;
}) {
  const allSelected = hasAllLocales(locales);
  const localeSet = new Set(locales);

  const toggleAll = () => {
    onChange(allSelected ? [] : [...ALL_LOCALES]);
  };

  const toggleLocale = (locale: Locale) => {
    if (localeSet.has(locale)) {
      onChange(locales.filter(l => l !== locale));
    } else {
      onChange([...locales, locale]);
    }
  };

  const toggleRegion = (regionLocales: Locale[]) => {
    const allInRegion = regionLocales.every(l => localeSet.has(l));
    if (allInRegion) {
      onChange(locales.filter(l => !regionLocales.includes(l)));
    } else {
      const newLocales = new Set(locales);
      regionLocales.forEach(l => newLocales.add(l));
      onChange([...newLocales]);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <Button
          variant="ghost"
          size="sm"
          onClick={toggleAll}
          disabled={disabled}
          className="text-xs"
        >
          {allSelected ? 'Deselect all' : 'Select all'}
        </Button>
        <span className="text-xs text-muted-foreground">
          {locales.length} of {ALL_LOCALES.length} selected
        </span>
      </div>

      <div className="space-y-4">
        {Object.entries(localeRegions).map(([region, regionLocales]) => {
          const selectedInRegion = regionLocales.filter(l => localeSet.has(l)).length;
          const allInRegion = selectedInRegion === regionLocales.length;

          return (
            <div key={region} className="space-y-2">
              <button
                onClick={() => toggleRegion(regionLocales)}
                disabled={disabled}
                className="flex items-center gap-2 text-sm font-medium hover:text-primary transition-colors"
              >
                <Checkbox
                  checked={allInRegion}
                  className="pointer-events-none"
                />
                {region}
                <span className="text-xs text-muted-foreground font-normal">
                  ({selectedInRegion}/{regionLocales.length})
                </span>
              </button>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 pl-6">
                {regionLocales.map((locale) => (
                  <label
                    key={locale}
                    className={cn(
                      'flex items-center gap-2 text-sm cursor-pointer p-1.5 rounded transition-colors',
                      'hover:bg-muted/50',
                      disabled && 'opacity-50 cursor-not-allowed'
                    )}
                  >
                    <Checkbox
                      checked={localeSet.has(locale)}
                      onCheckedChange={() => toggleLocale(locale)}
                      disabled={disabled}
                    />
                    <span className="truncate">{LOCALE_LABELS[locale]}</span>
                  </label>
                ))}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

/** Platform selection */
function PlatformEditor({
  platforms,
  onChange,
  disabled,
}: {
  platforms: Platform[];
  onChange: (platforms: Platform[]) => void;
  disabled?: boolean;
}) {
  const platformSet = new Set(platforms);

  const togglePlatform = (platform: Platform) => {
    if (platformSet.has(platform)) {
      onChange(platforms.filter(p => p !== platform));
    } else {
      onChange([...platforms, platform]);
    }
  };

  return (
    <div className="flex flex-wrap gap-2">
      {ALL_PLATFORMS.map((platform) => {
        const selected = platformSet.has(platform);
        return (
          <button
            key={platform}
            onClick={() => togglePlatform(platform)}
            disabled={disabled}
            className={cn(
              'px-4 py-2 rounded-lg border-2 transition-all font-medium text-sm',
              selected
                ? 'border-primary bg-primary/10 text-primary'
                : 'border-border hover:border-muted-foreground/50 text-muted-foreground'
            )}
          >
            {selected && <Check className="h-3 w-3 inline mr-2" />}
            {PLATFORM_LABELS[platform]}
          </button>
        );
      })}
    </div>
  );
}

/** Version range editor */
function VersionRangeEditor({
  versionRange,
  onChange,
  disabled,
}: {
  versionRange: VersionRange;
  onChange: (versionRange: VersionRange) => void;
  disabled?: boolean;
}) {
  const handleTypeChange = (type: string) => {
    switch (type) {
      case 'UNBOUNDED':
        onChange({ type: 'UNBOUNDED' });
        break;
      case 'MIN_BOUND':
        onChange({ type: 'MIN_BOUND', min: { major: 1, minor: 0, patch: 0 } });
        break;
      case 'MAX_BOUND':
        onChange({ type: 'MAX_BOUND', max: { major: 99, minor: 0, patch: 0 } });
        break;
      case 'MIN_AND_MAX_BOUND':
        onChange({
          type: 'MIN_AND_MAX_BOUND',
          min: { major: 1, minor: 0, patch: 0 },
          max: { major: 99, minor: 0, patch: 0 },
        });
        break;
    }
  };

  const updateVersion = (field: 'min' | 'max', version: Version) => {
    if (versionRange.type === 'MIN_BOUND' && field === 'min') {
      onChange({ ...versionRange, min: version });
    } else if (versionRange.type === 'MAX_BOUND' && field === 'max') {
      onChange({ ...versionRange, max: version });
    } else if (versionRange.type === 'MIN_AND_MAX_BOUND') {
      onChange({ ...versionRange, [field]: version });
    }
  };

  const VersionInput = ({ label, version, field }: { label: string; version: Version; field: 'min' | 'max' }) => (
    <div className="space-y-2">
      <Label className="text-xs">{label}</Label>
      <div className="flex items-center gap-2">
        <Input
          type="number"
          min={0}
          value={version.major}
          onChange={(e) => updateVersion(field, { ...version, major: parseInt(e.target.value) || 0 })}
          disabled={disabled}
          className="w-16 font-mono text-center"
          placeholder="0"
        />
        <span className="text-muted-foreground">.</span>
        <Input
          type="number"
          min={0}
          value={version.minor}
          onChange={(e) => updateVersion(field, { ...version, minor: parseInt(e.target.value) || 0 })}
          disabled={disabled}
          className="w-16 font-mono text-center"
          placeholder="0"
        />
        <span className="text-muted-foreground">.</span>
        <Input
          type="number"
          min={0}
          value={version.patch}
          onChange={(e) => updateVersion(field, { ...version, patch: parseInt(e.target.value) || 0 })}
          disabled={disabled}
          className="w-16 font-mono text-center"
          placeholder="0"
        />
      </div>
    </div>
  );

  return (
    <div className="space-y-4">
      <Select value={versionRange.type} onValueChange={handleTypeChange} disabled={disabled}>
        <SelectTrigger className="w-full max-w-[280px]">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="UNBOUNDED">All versions</SelectItem>
          <SelectItem value="MIN_BOUND">Minimum version</SelectItem>
          <SelectItem value="MAX_BOUND">Maximum version</SelectItem>
          <SelectItem value="MIN_AND_MAX_BOUND">Version range</SelectItem>
        </SelectContent>
      </Select>

      {versionRange.type === 'MIN_BOUND' && (
        <VersionInput label="Minimum version (inclusive)" version={versionRange.min} field="min" />
      )}

      {versionRange.type === 'MAX_BOUND' && (
        <VersionInput label="Maximum version (inclusive)" version={versionRange.max} field="max" />
      )}

      {versionRange.type === 'MIN_AND_MAX_BOUND' && (
        <div className="flex items-end gap-4">
          <VersionInput label="From" version={versionRange.min} field="min" />
          <span className="text-muted-foreground pb-2">to</span>
          <VersionInput label="To" version={versionRange.max} field="max" />
        </div>
      )}
    </div>
  );
}

/** Ramp-up percentage editor */
function RampUpEditor({
  rampUp,
  allowlist,
  onRampUpChange,
  onAllowlistChange,
  disabled,
}: {
  rampUp: number;
  allowlist: string[];
  onRampUpChange: (rampUp: number) => void;
  onAllowlistChange: (allowlist: string[]) => void;
  disabled?: boolean;
}) {
  const [newId, setNewId] = useState('');

  const addToAllowlist = () => {
    if (newId.trim() && !allowlist.includes(newId.trim())) {
      onAllowlistChange([...allowlist, newId.trim()]);
      setNewId('');
    }
  };

  const removeFromAllowlist = (id: string) => {
    onAllowlistChange(allowlist.filter(i => i !== id));
  };

  return (
    <div className="space-y-6">
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <Label>Rollout percentage</Label>
          <span className={cn(
            'text-2xl font-bold tabular-nums',
            rampUp === 100 && 'text-emerald-500',
            rampUp === 0 && 'text-muted-foreground',
            rampUp > 0 && rampUp < 100 && 'text-amber-500'
          )}>
            {rampUp}%
          </span>
        </div>
        <Slider
          value={[rampUp]}
          onValueChange={([v]) => onRampUpChange(v)}
          min={0}
          max={100}
          step={1}
          disabled={disabled}
          className="py-4"
        />
        <div className="flex justify-between text-xs text-muted-foreground">
          <span>0% (Off)</span>
          <span>100% (Full)</span>
        </div>
      </div>

      <div className="space-y-3">
        <Label>Allowlist (always included)</Label>
        <div className="flex gap-2">
          <Input
            value={newId}
            onChange={(e) => setNewId(e.target.value)}
            placeholder="User ID..."
            disabled={disabled}
            onKeyDown={(e) => e.key === 'Enter' && addToAllowlist()}
            className="flex-1"
          />
          <Button onClick={addToAllowlist} disabled={disabled || !newId.trim()} size="icon" variant="secondary">
            <Plus className="h-4 w-4" />
          </Button>
        </div>
        {allowlist.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {allowlist.map((id) => (
              <Badge key={id} variant="secondary" className="gap-1">
                {id}
                <button onClick={() => removeFromAllowlist(id)} disabled={disabled} className="hover:text-destructive">
                  <X className="h-3 w-3" />
                </button>
              </Badge>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

/** Custom axes editor */
function AxesEditor({
  axes,
  onChange,
  disabled,
}: {
  axes: Record<string, string[]>;
  onChange: (axes: Record<string, string[]>) => void;
  disabled?: boolean;
}) {
  const [newAxisKey, setNewAxisKey] = useState('');

  const addAxis = () => {
    if (newAxisKey.trim() && !axes[newAxisKey.trim()]) {
      onChange({ ...axes, [newAxisKey.trim()]: [] });
      setNewAxisKey('');
    }
  };

  const removeAxis = (key: string) => {
    const newAxes = { ...axes };
    delete newAxes[key];
    onChange(newAxes);
  };

  const updateAxisValues = (key: string, valuesText: string) => {
    const values = valuesText.split(',').map(v => v.trim()).filter(Boolean);
    onChange({ ...axes, [key]: values });
  };

  const axisEntries = Object.entries(axes);

  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        Define custom targeting dimensions (e.g., userTier, experimentGroup)
      </p>

      {axisEntries.length > 0 && (
        <div className="space-y-3">
          {axisEntries.map(([key, values]) => (
            <div key={key} className="flex gap-2 items-start">
              <div className="flex-1 space-y-1">
                <Label className="text-xs font-mono">{key}</Label>
                <Input
                  value={values.join(', ')}
                  onChange={(e) => updateAxisValues(key, e.target.value)}
                  placeholder="value1, value2, ..."
                  disabled={disabled}
                  className="font-mono text-sm"
                />
              </div>
              <Button
                variant="ghost"
                size="icon"
                onClick={() => removeAxis(key)}
                disabled={disabled}
                className="mt-5 text-muted-foreground hover:text-destructive"
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>
          ))}
        </div>
      )}

      <div className="flex gap-2">
        <Input
          value={newAxisKey}
          onChange={(e) => setNewAxisKey(e.target.value)}
          placeholder="New axis name..."
          disabled={disabled}
          onKeyDown={(e) => e.key === 'Enter' && addAxis()}
          className="flex-1"
        />
        <Button onClick={addAxis} disabled={disabled || !newAxisKey.trim()} variant="secondary">
          <Plus className="h-4 w-4 mr-2" />
          Add axis
        </Button>
      </div>
    </div>
  );
}

/** Main rule targeting editor */
export function RuleTargetingEditor({
  rule,
  onChange,
  disabled = false,
}: RuleTargetingEditorProps) {
  const hasCustomLocales = !hasAllLocales(rule.locales);
  const hasCustomPlatforms = !hasAllPlatforms(rule.platforms);
  const hasVersionConstraint = rule.versionRange.type !== 'UNBOUNDED';
  const hasPartialRollout = rule.rampUp < 100 || rule.rampUpAllowlist.length > 0;
  const hasAxes = Object.keys(rule.axes).length > 0;

  const localeSummary = hasCustomLocales
    ? `${rule.locales.length} locale${rule.locales.length !== 1 ? 's' : ''}`
    : 'All locales';

  const platformSummary = hasCustomPlatforms
    ? rule.platforms.map(p => PLATFORM_LABELS[p]).join(', ')
    : 'All platforms';

  const versionSummary = formatVersionRange(rule.versionRange);

  const rolloutSummary = hasPartialRollout
    ? `${rule.rampUp}%${rule.rampUpAllowlist.length > 0 ? ` + ${rule.rampUpAllowlist.length} allowlisted` : ''}`
    : 'Full rollout';

  const axesSummary = hasAxes
    ? `${Object.keys(rule.axes).length} custom dimension${Object.keys(rule.axes).length !== 1 ? 's' : ''}`
    : 'No custom targeting';

  return (
    <div className="divide-y divide-border rounded-lg border">
      <TargetingSection
        icon={Globe}
        title="Locales"
        summary={localeSummary}
        hasCustomization={hasCustomLocales}
      >
        <LocaleEditor
          locales={rule.locales}
          onChange={(locales) => onChange({ ...rule, locales })}
          disabled={disabled}
        />
      </TargetingSection>

      <TargetingSection
        icon={Smartphone}
        title="Platforms"
        summary={platformSummary}
        hasCustomization={hasCustomPlatforms}
      >
        <PlatformEditor
          platforms={rule.platforms}
          onChange={(platforms) => onChange({ ...rule, platforms })}
          disabled={disabled}
        />
      </TargetingSection>

      <TargetingSection
        icon={GitBranch}
        title="App Version"
        summary={versionSummary}
        hasCustomization={hasVersionConstraint}
      >
        <VersionRangeEditor
          versionRange={rule.versionRange}
          onChange={(versionRange) => onChange({ ...rule, versionRange })}
          disabled={disabled}
        />
      </TargetingSection>

      <TargetingSection
        icon={Users}
        title="Rollout"
        summary={rolloutSummary}
        hasCustomization={hasPartialRollout}
        defaultOpen={hasPartialRollout}
      >
        <RampUpEditor
          rampUp={rule.rampUp}
          allowlist={rule.rampUpAllowlist}
          onRampUpChange={(rampUp) => onChange({ ...rule, rampUp })}
          onAllowlistChange={(rampUpAllowlist) => onChange({ ...rule, rampUpAllowlist })}
          disabled={disabled}
        />
      </TargetingSection>

      <TargetingSection
        icon={Sliders}
        title="Custom Axes"
        summary={axesSummary}
        hasCustomization={hasAxes}
      >
        <AxesEditor
          axes={rule.axes}
          onChange={(axes) => onChange({ ...rule, axes })}
          disabled={disabled}
        />
      </TargetingSection>

      <div className="p-4">
        <Label className="text-sm font-medium">Note</Label>
        <Textarea
          value={rule.note || ''}
          onChange={(e) => onChange({ ...rule, note: e.target.value })}
          disabled={disabled}
          placeholder="Describe what this rule does..."
          className="mt-2"
        />
      </div>
    </div>
  );
}
