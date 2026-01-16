import React from 'react';
import { Link } from 'react-router-dom';
import { Stack, Grid, Inline } from '@/components/primitives/Layout';
import { Heading, Text, Code, Kbd } from '@/components/primitives/Typography';
import { Surface, Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/primitives/Surface';
import { Badge, StatusBadge, EnvironmentBadge } from '@/components/primitives/Badge';
import { Button } from '@/components/ui/button';
import { mockApplications } from '@/data/mockData';
import {
  Palette,
  Box,
  Puzzle,
  PlayCircle,
  Settings2,
  ArrowRight,
  Sparkles,
  Zap,
  Shield,
  Layers,
} from 'lucide-react';

const features = [
  {
    icon: Palette,
    title: 'Design Primitives',
    description: 'Complete design token system with colors, typography, spacing, and motion.',
    href: '/primitives',
    color: 'text-info',
  },
  {
    icon: Box,
    title: 'Core Components',
    description: 'Robust UI components built for configuration management workflows.',
    href: '/components',
    color: 'text-success',
  },
  {
    icon: Puzzle,
    title: 'Patterns & Recipes',
    description: 'Best practices for schema-driven forms, safe publishing, and more.',
    href: '/patterns',
    color: 'text-warning',
  },
  {
    icon: PlayCircle,
    title: 'Playground',
    description: 'Interactive environment to test themes, density, and components.',
    href: '/playground',
    color: 'text-accent',
  },
];

const highlights = [
  {
    icon: Sparkles,
    title: 'Schema-Driven',
    description: 'Forms generated from JSON schemas with conditional logic.',
  },
  {
    icon: Zap,
    title: 'Environment-Aware',
    description: 'Required fields and defaults vary by environment.',
  },
  {
    icon: Shield,
    title: 'Safe Publishing',
    description: 'Draft → Review → Approve → Publish workflow.',
  },
  {
    icon: Layers,
    title: 'Accessible',
    description: 'WCAG 2.1 compliant with full keyboard navigation.',
  },
];

const Index: React.FC = () => {
  return (
    <Stack gap="xl" className="animate-fade-in">
      {/* Hero Section */}
      <div className="relative -mx-8 -mt-6 overflow-hidden rounded-xl bg-gradient-to-br from-surface-1 via-surface-2 to-surface-1 px-8 py-12">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_30%_20%,hsl(var(--accent)/0.1),transparent_50%)]" />
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_70%_80%,hsl(var(--info)/0.1),transparent_50%)]" />
        
        <Stack gap="lg" className="relative">
          <Stack gap="sm">
            <Badge variant="default" size="md">
              <Sparkles className="h-3 w-3" />
              Configuration Management UI
            </Badge>
            <Heading level={1} className="max-w-2xl">
              Build robust config management interfaces
            </Heading>
            <Text variant="body-lg" textColor="muted" className="max-w-xl">
              A comprehensive component library and design system for building
              enterprise-grade configuration management applications.
            </Text>
          </Stack>
          
          <Inline gap="md">
            <Button asChild size="lg" variant="accent">
              <Link to="/demo">
                <Settings2 className="h-4 w-4" />
                View Demo
                <ArrowRight className="h-4 w-4" />
              </Link>
            </Button>
            <Button asChild size="lg" variant="outline">
              <Link to="/components">
                Browse Components
              </Link>
            </Button>
          </Inline>

          <Inline gap="md" className="pt-4">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Kbd>⌘</Kbd>
              <Kbd>K</Kbd>
              <span>to open command palette</span>
            </div>
          </Inline>
        </Stack>
      </div>

      {/* Feature Cards */}
      <Stack gap="md">
        <Heading level={3}>Explore the Library</Heading>
        <Grid cols={2} gap="md" className="lg:grid-cols-4">
          {features.map((feature) => {
            const Icon = feature.icon;
            return (
              <Link key={feature.href} to={feature.href}>
                <Surface
                  elevation={1}
                  interactive
                  className="h-full p-5 transition-all hover:-translate-y-1"
                >
                  <Stack gap="sm">
                    <div className={`inline-flex h-10 w-10 items-center justify-center rounded-lg bg-surface-2 ${feature.color}`}>
                      <Icon className="h-5 w-5" />
                    </div>
                    <Text weight="semibold">{feature.title}</Text>
                    <Text variant="small" textColor="muted">
                      {feature.description}
                    </Text>
                  </Stack>
                </Surface>
              </Link>
            );
          })}
        </Grid>
      </Stack>

      {/* Highlights */}
      <Stack gap="md">
        <Heading level={3}>Key Features</Heading>
        <Grid cols={2} gap="md" className="lg:grid-cols-4">
          {highlights.map((highlight) => {
            const Icon = highlight.icon;
            return (
              <Card key={highlight.title} elevation={0} className="bg-surface-1">
                <CardContent className="p-4">
                  <Inline gap="sm" align="start">
                    <Icon className="h-5 w-5 text-accent shrink-0 mt-0.5" />
                    <Stack gap="xs">
                      <Text weight="medium">{highlight.title}</Text>
                      <Text variant="small" textColor="muted">
                        {highlight.description}
                      </Text>
                    </Stack>
                  </Inline>
                </CardContent>
              </Card>
            );
          })}
        </Grid>
      </Stack>

      {/* Applications Preview */}
      <Stack gap="md">
        <Inline justify="between" align="center">
          <Heading level={3}>Recent Applications</Heading>
          <Button variant="ghost" size="sm" asChild>
            <Link to="/demo">
              View all
              <ArrowRight className="h-4 w-4" />
            </Link>
          </Button>
        </Inline>
        <Grid cols={1} gap="md" className="lg:grid-cols-3">
          {mockApplications.map((app) => {
            const latestVersion = app.configVersions[app.configVersions.length - 1];
            return (
              <Card key={app.id} elevation={1} interactive className="p-4">
                <CardContent>
                  <Stack gap="sm">
                    <Inline justify="between" align="start">
                      <Stack gap="xs">
                        <Text weight="semibold">{app.name}</Text>
                        <Text variant="small" textColor="muted">
                          {app.description}
                        </Text>
                      </Stack>
                      <StatusBadge status={latestVersion.status} />
                    </Inline>
                    <Inline gap="sm">
                      <Badge variant="outline" size="sm">
                        v{latestVersion.version}
                      </Badge>
                      <Text variant="caption">{app.owner}</Text>
                    </Inline>
                  </Stack>
                </CardContent>
              </Card>
            );
          })}
        </Grid>
      </Stack>

      {/* Quick Stats */}
      <Surface elevation={1} className="p-6">
        <Grid cols={2} gap="lg" className="lg:grid-cols-4">
          <Stack gap="xs" align="center">
            <Text variant="body-lg" weight="bold" className="text-2xl">50+</Text>
            <Text variant="small" textColor="muted">Components</Text>
          </Stack>
          <Stack gap="xs" align="center">
            <Text variant="body-lg" weight="bold" className="text-2xl">100%</Text>
            <Text variant="small" textColor="muted">TypeScript</Text>
          </Stack>
          <Stack gap="xs" align="center">
            <Text variant="body-lg" weight="bold" className="text-2xl">A11y</Text>
            <Text variant="small" textColor="muted">Accessible</Text>
          </Stack>
          <Stack gap="xs" align="center">
            <Text variant="body-lg" weight="bold" className="text-2xl">Themed</Text>
            <Text variant="small" textColor="muted">Dark & Light</Text>
          </Stack>
        </Grid>
      </Surface>
    </Stack>
  );
};

export default Index;
