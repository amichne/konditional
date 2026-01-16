import React from 'react';
import { Stack, Grid, Inline, Box, Divider } from '@/components/primitives/Layout';
import { Heading, Text, Code } from '@/components/primitives/Typography';
import { Surface, Card, CardContent } from '@/components/primitives/Surface';
import { Badge } from '@/components/primitives/Badge';

const colorTokens = [
  { name: 'Primary', token: '--primary', sample: 'bg-primary' },
  { name: 'Accent', token: '--accent', sample: 'bg-accent' },
  { name: 'Success', token: '--success', sample: 'bg-success' },
  { name: 'Warning', token: '--warning', sample: 'bg-warning' },
  { name: 'Error', token: '--error', sample: 'bg-error' },
  { name: 'Info', token: '--info', sample: 'bg-info' },
];

const neutralScale = [
  { name: 'Neutral 50', token: '--neutral-50', sample: 'bg-neutral-50' },
  { name: 'Neutral 100', token: '--neutral-100', sample: 'bg-neutral-100' },
  { name: 'Neutral 200', token: '--neutral-200', sample: 'bg-neutral-200' },
  { name: 'Neutral 300', token: '--neutral-300', sample: 'bg-neutral-300' },
  { name: 'Neutral 400', token: '--neutral-400', sample: 'bg-neutral-400' },
  { name: 'Neutral 500', token: '--neutral-500', sample: 'bg-neutral-500' },
  { name: 'Neutral 600', token: '--neutral-600', sample: 'bg-neutral-600' },
  { name: 'Neutral 700', token: '--neutral-700', sample: 'bg-neutral-700' },
  { name: 'Neutral 800', token: '--neutral-800', sample: 'bg-neutral-800' },
  { name: 'Neutral 900', token: '--neutral-900', sample: 'bg-neutral-900' },
  { name: 'Neutral 950', token: '--neutral-950', sample: 'bg-neutral-950' },
];

const surfaceTokens = [
  { name: 'Surface 0', token: '--surface-0', sample: 'bg-surface-0' },
  { name: 'Surface 1', token: '--surface-1', sample: 'bg-surface-1' },
  { name: 'Surface 2', token: '--surface-2', sample: 'bg-surface-2' },
  { name: 'Surface 3', token: '--surface-3', sample: 'bg-surface-3' },
];

const PrimitivesColors: React.FC = () => {
  return (
    <Stack gap="xl" className="animate-fade-in">
      <Stack gap="sm">
        <Badge variant="default" size="sm">Design Primitives</Badge>
        <Heading level={1}>Colors</Heading>
        <Text textColor="muted" className="max-w-2xl">
          Our color system is built on HSL values defined as CSS custom properties.
          All colors adapt automatically for light and dark themes.
        </Text>
      </Stack>

      {/* Semantic Colors */}
      <Stack gap="md">
        <Heading level={3}>Semantic Colors</Heading>
        <Text textColor="muted">
          Use semantic colors to convey meaning: success, warning, error, and info states.
        </Text>
        <Grid cols={2} gap="md" className="lg:grid-cols-3">
          {colorTokens.map((color) => (
            <Surface key={color.name} elevation={1} className="p-4 overflow-hidden">
              <Stack gap="sm">
                <div className={`h-16 rounded-lg ${color.sample}`} />
                <Inline justify="between" align="center">
                  <Stack gap="none">
                    <Text weight="medium">{color.name}</Text>
                    <Code>{color.token}</Code>
                  </Stack>
                </Inline>
              </Stack>
            </Surface>
          ))}
        </Grid>
      </Stack>

      {/* Neutral Scale */}
      <Stack gap="md">
        <Heading level={3}>Neutral Scale</Heading>
        <Text textColor="muted">
          A comprehensive neutral palette from light to dark for backgrounds, borders, and text.
        </Text>
        <Surface elevation={1} className="p-4">
          <div className="flex flex-wrap gap-2">
            {neutralScale.map((color) => (
              <div key={color.name} className="text-center">
                <div
                  className={`h-12 w-12 rounded-lg ${color.sample} border border-border`}
                  title={color.name}
                />
                <Text variant="caption" className="mt-1 block">
                  {color.name.split(' ')[1]}
                </Text>
              </div>
            ))}
          </div>
        </Surface>
      </Stack>

      {/* Surface Elevation */}
      <Stack gap="md">
        <Heading level={3}>Surface Elevation</Heading>
        <Text textColor="muted">
          Surface tokens create visual hierarchy through background colors and shadows.
        </Text>
        <Grid cols={2} gap="md" className="lg:grid-cols-4">
          {surfaceTokens.map((surface, index) => (
            <Surface key={surface.name} elevation={index as 0 | 1 | 2 | 3} className="p-6">
              <Stack gap="xs" align="center">
                <Text weight="medium">{surface.name}</Text>
                <Code className="text-xs">{surface.token}</Code>
                <Badge variant="outline" size="sm">
                  Elevation {index}
                </Badge>
              </Stack>
            </Surface>
          ))}
        </Grid>
      </Stack>

      {/* Usage */}
      <Stack gap="md">
        <Heading level={3}>Usage</Heading>
        <Surface elevation={1} className="p-4">
          <Code variant="block">
{`/* Using color tokens in Tailwind */
<div className="bg-primary text-primary-foreground">
  Primary button
</div>

<div className="bg-surface-1 border-border">
  Card with elevation 1
</div>

<span className="text-error">
  Error message
</span>`}
          </Code>
        </Surface>
      </Stack>
    </Stack>
  );
};

export default PrimitivesColors;
