import React from 'react';
import { Stack, Grid, Inline, Box, Divider } from '@/components/primitives/Layout';
import { Heading, Text, Code } from '@/components/primitives/Typography';
import { Surface, Card, CardContent } from '@/components/primitives/Surface';
import { Badge } from '@/components/primitives/Badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Checkbox } from '@/components/ui/checkbox';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { Slider } from '@/components/ui/slider';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { Plus, Search, Trash2, Save, Download } from 'lucide-react';

const ComponentsInputs: React.FC = () => {
  const [sliderValue, setSliderValue] = React.useState([50]);

  return (
    <Stack gap="xl" className="animate-fade-in">
      <Stack gap="sm">
        <Badge variant="default" size="sm">Core Components</Badge>
        <Heading level={1}>Inputs & Forms</Heading>
        <Text textColor="muted" className="max-w-2xl">
          Form components for building configuration interfaces with validation support.
        </Text>
      </Stack>

      {/* Buttons */}
      <Stack gap="md">
        <Heading level={3}>Buttons</Heading>
        <Surface elevation={1} className="p-6">
          <Stack gap="lg">
            <Stack gap="sm">
              <Text weight="medium">Variants</Text>
              <Inline gap="sm" wrap="wrap">
                <Button variant="default">Default</Button>
                <Button variant="secondary">Secondary</Button>
                <Button variant="outline">Outline</Button>
                <Button variant="ghost">Ghost</Button>
                <Button variant="link">Link</Button>
              </Inline>
            </Stack>

            <Stack gap="sm">
              <Text weight="medium">Semantic Variants</Text>
              <Inline gap="sm" wrap="wrap">
                <Button variant="accent">Accent</Button>
                <Button variant="accent-outline">Accent Outline</Button>
                <Button variant="destructive">Destructive</Button>
                <Button variant="success">Success</Button>
                <Button variant="warning">Warning</Button>
              </Inline>
            </Stack>

            <Stack gap="sm">
              <Text weight="medium">Sizes</Text>
              <Inline gap="sm" align="center">
                <Button size="sm">Small</Button>
                <Button size="default">Default</Button>
                <Button size="lg">Large</Button>
                <Button size="xl">Extra Large</Button>
              </Inline>
            </Stack>

            <Stack gap="sm">
              <Text weight="medium">With Icons</Text>
              <Inline gap="sm">
                <Button leftIcon={<Plus className="h-4 w-4" />}>Create New</Button>
                <Button variant="outline" rightIcon={<Download className="h-4 w-4" />}>Export</Button>
                <Button variant="ghost" size="icon"><Search className="h-4 w-4" /></Button>
                <Button variant="destructive" size="icon-sm"><Trash2 className="h-4 w-4" /></Button>
              </Inline>
            </Stack>

            <Stack gap="sm">
              <Text weight="medium">States</Text>
              <Inline gap="sm">
                <Button disabled>Disabled</Button>
                <Button loading>Loading</Button>
              </Inline>
            </Stack>
          </Stack>
        </Surface>
      </Stack>

      {/* Text Inputs */}
      <Stack gap="md">
        <Heading level={3}>Text Inputs</Heading>
        <Surface elevation={1} className="p-6">
          <Grid cols={1} gap="lg" className="lg:grid-cols-2">
            <Stack gap="sm">
              <Label htmlFor="basic">Basic Input</Label>
              <Input id="basic" placeholder="Enter text..." />
            </Stack>

            <Stack gap="sm">
              <Label htmlFor="disabled">Disabled Input</Label>
              <Input id="disabled" placeholder="Disabled" disabled />
            </Stack>

            <Stack gap="sm">
              <Label htmlFor="with-icon">With Search Icon</Label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input id="with-icon" className="pl-10" placeholder="Search..." />
              </div>
            </Stack>

            <Stack gap="sm">
              <Label htmlFor="textarea">Textarea</Label>
              <Textarea id="textarea" placeholder="Enter description..." rows={3} />
            </Stack>
          </Grid>
        </Surface>
      </Stack>

      {/* Select */}
      <Stack gap="md">
        <Heading level={3}>Select</Heading>
        <Surface elevation={1} className="p-6">
          <Grid cols={1} gap="lg" className="lg:grid-cols-2">
            <Stack gap="sm">
              <Label>Log Level</Label>
              <Select defaultValue="info">
                <SelectTrigger>
                  <SelectValue placeholder="Select log level" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="debug">Debug</SelectItem>
                  <SelectItem value="info">Info</SelectItem>
                  <SelectItem value="warn">Warning</SelectItem>
                  <SelectItem value="error">Error</SelectItem>
                </SelectContent>
              </Select>
            </Stack>

            <Stack gap="sm">
              <Label>Environment</Label>
              <Select defaultValue="development">
                <SelectTrigger>
                  <SelectValue placeholder="Select environment" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="development">Development</SelectItem>
                  <SelectItem value="staging">Staging</SelectItem>
                  <SelectItem value="production">Production</SelectItem>
                </SelectContent>
              </Select>
            </Stack>
          </Grid>
        </Surface>
      </Stack>

      {/* Toggles */}
      <Stack gap="md">
        <Heading level={3}>Toggles</Heading>
        <Surface elevation={1} className="p-6">
          <Grid cols={1} gap="lg" className="lg:grid-cols-2">
            <Stack gap="md">
              <Text weight="medium">Checkboxes</Text>
              <Stack gap="sm">
                <Inline gap="sm">
                  <Checkbox id="check1" />
                  <Label htmlFor="check1">Enable feature A</Label>
                </Inline>
                <Inline gap="sm">
                  <Checkbox id="check2" defaultChecked />
                  <Label htmlFor="check2">Enable feature B (default checked)</Label>
                </Inline>
                <Inline gap="sm">
                  <Checkbox id="check3" disabled />
                  <Label htmlFor="check3">Disabled option</Label>
                </Inline>
              </Stack>
            </Stack>

            <Stack gap="md">
              <Text weight="medium">Switches</Text>
              <Stack gap="sm">
                <Inline gap="sm" justify="between">
                  <Label htmlFor="switch1">Dark mode</Label>
                  <Switch id="switch1" />
                </Inline>
                <Inline gap="sm" justify="between">
                  <Label htmlFor="switch2">Notifications</Label>
                  <Switch id="switch2" defaultChecked />
                </Inline>
                <Inline gap="sm" justify="between">
                  <Label htmlFor="switch3">Auto-save (disabled)</Label>
                  <Switch id="switch3" disabled />
                </Inline>
              </Stack>
            </Stack>
          </Grid>
        </Surface>
      </Stack>

      {/* Slider */}
      <Stack gap="md">
        <Heading level={3}>Slider</Heading>
        <Surface elevation={1} className="p-6">
          <Stack gap="md" className="max-w-md">
            <Inline justify="between">
              <Label>API Timeout (ms)</Label>
              <Text weight="medium">{sliderValue[0] * 100}</Text>
            </Inline>
            <Slider
              value={sliderValue}
              onValueChange={setSliderValue}
              max={100}
              step={1}
            />
            <Inline justify="between">
              <Text variant="caption" textColor="muted">1000ms</Text>
              <Text variant="caption" textColor="muted">10000ms</Text>
            </Inline>
          </Stack>
        </Surface>
      </Stack>
    </Stack>
  );
};

export default ComponentsInputs;
