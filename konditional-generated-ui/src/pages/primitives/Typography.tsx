import React from 'react';
import { Stack, Grid, Inline } from '@/components/primitives/Layout';
import { Heading, Text, Code } from '@/components/primitives/Typography';
import { Surface } from '@/components/primitives/Surface';
import { Badge } from '@/components/primitives/Badge';

const PrimitivesTypography: React.FC = () => {
  return (
    <Stack gap="xl" className="animate-fade-in">
      <Stack gap="sm">
        <Badge variant="default" size="sm">Design Primitives</Badge>
        <Heading level={1}>Typography</Heading>
        <Text textColor="muted" className="max-w-2xl">
          Typography scale using IBM Plex Sans for UI and JetBrains Mono for code.
        </Text>
      </Stack>

      {/* Heading Scale */}
      <Stack gap="md">
        <Heading level={3}>Headings</Heading>
        <Surface elevation={1} className="p-6">
          <Stack gap="lg">
            <Stack gap="xs">
              <Heading level={1}>Heading 1</Heading>
              <Code>level={1} • text-4xl lg:text-5xl</Code>
            </Stack>
            <Stack gap="xs">
              <Heading level={2}>Heading 2</Heading>
              <Code>level={2} • text-3xl lg:text-4xl</Code>
            </Stack>
            <Stack gap="xs">
              <Heading level={3}>Heading 3</Heading>
              <Code>level={3} • text-2xl lg:text-3xl</Code>
            </Stack>
            <Stack gap="xs">
              <Heading level={4}>Heading 4</Heading>
              <Code>level={4} • text-xl lg:text-2xl</Code>
            </Stack>
            <Stack gap="xs">
              <Heading level={5}>Heading 5</Heading>
              <Code>level={5} • text-lg lg:text-xl</Code>
            </Stack>
            <Stack gap="xs">
              <Heading level={6}>Heading 6</Heading>
              <Code>level={6} • text-base lg:text-lg</Code>
            </Stack>
          </Stack>
        </Surface>
      </Stack>

      {/* Body Text */}
      <Stack gap="md">
        <Heading level={3}>Body Text</Heading>
        <Surface elevation={1} className="p-6">
          <Stack gap="md">
            <Stack gap="xs">
              <Text variant="body-lg">
                Body Large - Perfect for lead paragraphs and important content that needs extra emphasis.
              </Text>
              <Code>variant="body-lg" • text-base</Code>
            </Stack>
            <Stack gap="xs">
              <Text variant="body">
                Body - The default text style for paragraphs and general content throughout the application.
              </Text>
              <Code>variant="body" • text-sm</Code>
            </Stack>
            <Stack gap="xs">
              <Text variant="small">
                Small - Used for less important text, footnotes, or secondary information.
              </Text>
              <Code>variant="small" • text-xs</Code>
            </Stack>
            <Stack gap="xs">
              <Text variant="caption">
                Caption - Smallest text variant for labels and metadata.
              </Text>
              <Code>variant="caption" • text-xs text-muted-foreground</Code>
            </Stack>
          </Stack>
        </Surface>
      </Stack>

      {/* Code & Mono */}
      <Stack gap="md">
        <Heading level={3}>Code & Monospace</Heading>
        <Surface elevation={1} className="p-6">
          <Stack gap="md">
            <Stack gap="xs">
              <Text>
                Inline code: <Code>const value = "hello";</Code>
              </Text>
              <Text variant="small" textColor="muted">
                JetBrains Mono font for code clarity
              </Text>
            </Stack>
            <Stack gap="xs">
              <Code variant="block">
{`function greet(name: string): string {
  return \`Hello, \${name}!\`;
}

const message = greet("World");
console.log(message);`}
              </Code>
              <Text variant="small" textColor="muted">
                Code blocks with syntax highlighting ready
              </Text>
            </Stack>
          </Stack>
        </Surface>
      </Stack>

      {/* Font Weights */}
      <Stack gap="md">
        <Heading level={3}>Font Weights</Heading>
        <Surface elevation={1} className="p-6">
          <Grid cols={2} gap="md" className="lg:grid-cols-4">
            <Stack gap="xs">
              <Text weight="normal" className="text-lg">Normal</Text>
              <Code>weight="normal" • 400</Code>
            </Stack>
            <Stack gap="xs">
              <Text weight="medium" className="text-lg">Medium</Text>
              <Code>weight="medium" • 500</Code>
            </Stack>
            <Stack gap="xs">
              <Text weight="semibold" className="text-lg">Semibold</Text>
              <Code>weight="semibold" • 600</Code>
            </Stack>
            <Stack gap="xs">
              <Text weight="bold" className="text-lg">Bold</Text>
              <Code>weight="bold" • 700</Code>
            </Stack>
          </Grid>
        </Surface>
      </Stack>
    </Stack>
  );
};

export default PrimitivesTypography;
