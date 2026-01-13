/**
 * Schema Forms Pattern Demo
 * Demonstrates rendering complex configuration from OpenAPI schema
 */

import React, { useEffect, useState } from 'react';
import { 
  AnySerializableFlag, 
  AnySerializableRule,
  SerializablePatch,
  SnapshotMeta,
  parseFeatureId,
  createDefaultRule,
} from '@/types/konditional';
import { mockSnapshot, getFlagsByNamespace, paymentProviderOptions } from '@/data/konditionalMockData';
import { applySnapshotPatch, fetchSnapshot } from '@/lib/konditionalApi';
import { FlagValueEditor, ValueTypeBadge, FlagValueDisplay } from '@/components/config/FlagValueEditor';
import { RuleTargetingEditor } from '@/components/config/RuleTargetingEditor';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Switch } from '@/components/ui/switch';
import { cn } from '@/lib/utils';
import {
  Plus,
  ChevronRight,
  Settings2,
  Eye,
  Code,
  Trash2,
  Copy,
  ArrowLeft,
} from 'lucide-react';

/** Flag list item */
function FlagListItem({ 
  flag, 
  selected,
  onSelect 
}: { 
  flag: AnySerializableFlag;
  selected: boolean;
  onSelect: () => void;
}) {
  const parsed = parseFeatureId(flag.key);
  
  return (
    <button
      onClick={onSelect}
      className={cn(
        'w-full text-left p-4 rounded-lg border transition-all',
        'hover:border-primary/50 hover:bg-muted/30',
        selected && 'border-primary bg-primary/5 ring-1 ring-primary/20',
        !flag.isActive && 'opacity-60'
      )}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <span className="font-mono text-sm font-medium truncate">
              {parsed?.key || flag.key}
            </span>
            {!flag.isActive && (
              <Badge variant="secondary" className="text-xs">Inactive</Badge>
            )}
          </div>
          <div className="flex items-center gap-2">
            <ValueTypeBadge type={flag.defaultValue.type} />
            {flag.rules.length > 0 && (
              <span className="text-xs text-muted-foreground">
                {flag.rules.length} rule{flag.rules.length !== 1 ? 's' : ''}
              </span>
            )}
          </div>
        </div>
        <ChevronRight className="h-4 w-4 text-muted-foreground shrink-0 mt-1" />
      </div>
    </button>
  );
}

/** Flag editor panel */
function FlagEditor({ 
  flag, 
  onChange,
  onBack,
}: { 
  flag: AnySerializableFlag;
  onChange: (flag: AnySerializableFlag) => void;
  onBack: () => void;
}) {
  const [activeRuleIndex, setActiveRuleIndex] = useState<number | null>(null);
  const parsed = parseFeatureId(flag.key);

  const addRule = () => {
    const newRule = createDefaultRule(flag.defaultValue);
    newRule.note = 'New rule';
    onChange({
      ...flag,
      rules: [...flag.rules, newRule],
    });
    setActiveRuleIndex(flag.rules.length);
  };

  const updateRule = (index: number, rule: AnySerializableRule) => {
    const newRules = [...flag.rules];
    newRules[index] = rule as any;
    onChange({ ...flag, rules: newRules });
  };

  const deleteRule = (index: number) => {
    onChange({
      ...flag,
      rules: flag.rules.filter((_, i) => i !== index),
    });
    setActiveRuleIndex(null);
  };

  const getEnumOptions = () => {
    if (flag.defaultValue.type === 'ENUM') {
      return paymentProviderOptions;
    }
    return [];
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={onBack}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div className="flex-1">
          <div className="flex items-center gap-2">
            <h2 className="text-xl font-semibold font-mono">{parsed?.key}</h2>
            <ValueTypeBadge type={flag.defaultValue.type} />
          </div>
          <p className="text-sm text-muted-foreground font-mono">{flag.key}</p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm text-muted-foreground">Active</span>
          <Switch
            checked={flag.isActive}
            onCheckedChange={(isActive) => onChange({ ...flag, isActive })}
          />
        </div>
      </div>

      <Tabs defaultValue="config" className="w-full">
        <TabsList>
          <TabsTrigger value="config" className="gap-2">
            <Settings2 className="h-4 w-4" />
            Configure
          </TabsTrigger>
          <TabsTrigger value="preview" className="gap-2">
            <Eye className="h-4 w-4" />
            Preview
          </TabsTrigger>
          <TabsTrigger value="json" className="gap-2">
            <Code className="h-4 w-4" />
            JSON
          </TabsTrigger>
        </TabsList>

        <TabsContent value="config" className="space-y-6 mt-6">
          {/* Default Value */}
          <Card>
            <CardHeader className="pb-4">
              <CardTitle className="text-base">Default Value</CardTitle>
            </CardHeader>
            <CardContent>
              <FlagValueEditor
                value={flag.defaultValue}
                onChange={(defaultValue) => onChange({ ...flag, defaultValue: defaultValue as any })}
                enumOptions={getEnumOptions()}
              />
            </CardContent>
          </Card>

          {/* Rules */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-semibold">Targeting Rules</h3>
              <Button onClick={addRule} size="sm" variant="outline">
                <Plus className="h-4 w-4 mr-2" />
                Add Rule
              </Button>
            </div>

            {flag.rules.length === 0 ? (
              <Card className="border-dashed">
                <CardContent className="py-8 text-center text-muted-foreground">
                  <p>No rules defined. The default value will be used for all users.</p>
                  <Button onClick={addRule} variant="link" className="mt-2">
                    Add your first rule
                  </Button>
                </CardContent>
              </Card>
            ) : (
              <div className="space-y-3">
                {flag.rules.map((rule, index) => (
                  <Card 
                    key={index}
                    className={cn(
                      'cursor-pointer transition-all',
                      activeRuleIndex === index && 'ring-2 ring-primary'
                    )}
                  >
                    <CardHeader 
                      className="pb-2 cursor-pointer"
                      onClick={() => setActiveRuleIndex(activeRuleIndex === index ? null : index)}
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <Badge variant="outline">Rule {index + 1}</Badge>
                          <span className="text-sm text-muted-foreground">
                            {rule.note || 'No description'}
                          </span>
                        </div>
                        <div className="flex items-center gap-2">
                          <FlagValueDisplay value={rule.value} />
                          <Badge variant={rule.rampUp === 100 ? 'default' : 'secondary'}>
                            {rule.rampUp}%
                          </Badge>
                        </div>
                      </div>
                    </CardHeader>
                    
                    {activeRuleIndex === index && (
                      <CardContent className="space-y-4 pt-4 border-t">
                        <div>
                          <h4 className="text-sm font-medium mb-3">Value</h4>
                          <FlagValueEditor
                            value={rule.value}
                            onChange={(value) => updateRule(index, { ...rule, value: value as any })}
                            enumOptions={getEnumOptions()}
                          />
                        </div>
                        
                        <div>
                          <h4 className="text-sm font-medium mb-3">Targeting</h4>
                          <RuleTargetingEditor
                            rule={rule as any}
                            onChange={(r) => updateRule(index, r as any)}
                          />
                        </div>

                        <div className="flex justify-end pt-4">
                          <Button 
                            variant="destructive" 
                            size="sm"
                            onClick={() => deleteRule(index)}
                          >
                            <Trash2 className="h-4 w-4 mr-2" />
                            Delete Rule
                          </Button>
                        </div>
                      </CardContent>
                    )}
                  </Card>
                ))}
              </div>
            )}
          </div>
        </TabsContent>

        <TabsContent value="preview" className="mt-6">
          <Card>
            <CardContent className="py-6">
              <p className="text-muted-foreground text-center">
                Preview how this flag resolves for different user contexts
              </p>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="json" className="mt-6">
          <Card>
            <CardContent className="p-0">
              <pre className="p-4 overflow-auto text-xs font-mono bg-muted/30 rounded-lg max-h-[500px]">
                {JSON.stringify(flag, null, 2)}
              </pre>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}

/** Main Schema Forms page */
export default function SchemaFormsPage() {
  const [flags, setFlags] = useState<AnySerializableFlag[]>(mockSnapshot.flags as AnySerializableFlag[]);
  const [meta, setMeta] = useState<SnapshotMeta | undefined>(mockSnapshot.meta);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [selectedFlagKey, setSelectedFlagKey] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    fetchSnapshot()
      .then((snapshot) => {
        if (!isMounted) {
          return;
        }
        setFlags(snapshot.flags as AnySerializableFlag[]);
        setMeta(snapshot.meta);
        setErrorMessage(null);
      })
      .catch((error) => {
        if (!isMounted) {
          return;
        }
        setErrorMessage(error instanceof Error ? error.message : 'Failed to load snapshot');
      })
      .finally(() => {
        if (isMounted) {
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, []);
  
  const flagsByNamespace = getFlagsByNamespace(flags as any);
  const selectedFlag = flags.find(f => f.key === selectedFlagKey);

  const updateFlag = (updated: AnySerializableFlag) => {
    setFlags(flags.map(f => f.key === updated.key ? updated : f));
  };

  const saveSnapshot = async () => {
    setIsSaving(true);
    setErrorMessage(null);
    try {
      const patch: SerializablePatch = {
        meta,
        flags: flags as any,
        removeKeys: [],
      };
      const updatedSnapshot = await applySnapshotPatch(patch);
      setFlags(updatedSnapshot.flags as AnySerializableFlag[]);
      setMeta(updatedSnapshot.meta);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to save snapshot');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="min-h-screen">
      <div className="max-w-6xl mx-auto p-6">
        <div className="mb-8">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h1 className="text-3xl font-bold mb-2">Schema-Driven Forms</h1>
            <Button onClick={saveSnapshot} disabled={isSaving || isLoading} variant="secondary">
              {isSaving ? 'Saving...' : 'Save changes'}
            </Button>
          </div>
          <p className="text-muted-foreground">
            Configure feature flags with type-safe editors generated from the OpenAPI schema.
            Complex targeting made simple through progressive disclosure.
          </p>
          {isLoading && (
            <p className="text-sm text-muted-foreground mt-2">Loading snapshot...</p>
          )}
          {errorMessage && (
            <p className="text-sm text-destructive mt-2">{errorMessage}</p>
          )}
        </div>

        {selectedFlag ? (
          <FlagEditor
            flag={selectedFlag}
            onChange={updateFlag}
            onBack={() => setSelectedFlagKey(null)}
          />
        ) : (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {Object.entries(flagsByNamespace).map(([namespace, nsFlags]) => (
              <div key={namespace}>
                <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">
                  {namespace}
                </h3>
                <div className="space-y-2">
                  {nsFlags.map((flag) => (
                    <FlagListItem
                      key={flag.key}
                      flag={flag}
                      selected={selectedFlagKey === flag.key}
                      onSelect={() => setSelectedFlagKey(flag.key)}
                    />
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
