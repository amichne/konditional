import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import Fuse from 'fuse.js';
import { cn } from '@/lib/utils';
import { useAppStore } from '@/lib/store';
import {
  Command,
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
  CommandSeparator,
  CommandShortcut,
} from '@/components/ui/command';
import {
  Palette,
  Box,
  Puzzle,
  PlayCircle,
  Settings2,
  Sun,
  Moon,
  Monitor,
  Maximize,
  Minimize,
  FileCode,
  Search,
} from 'lucide-react';

interface CommandAction {
  id: string;
  label: string;
  description?: string;
  icon: React.ElementType;
  keywords: string[];
  action: () => void;
  shortcut?: string;
  group: string;
}

export const CommandPalette: React.FC = () => {
  const navigate = useNavigate();
  const {
    commandPaletteOpen,
    setCommandPaletteOpen,
    theme,
    setTheme,
    density,
    setDensity,
  } = useAppStore();

  const [search, setSearch] = useState('');
  const [results, setResults] = useState<CommandAction[]>([]);

  const actions: CommandAction[] = [
    // Navigation
    {
      id: 'nav-primitives',
      label: 'Design Primitives',
      description: 'View design tokens and primitives',
      icon: Palette,
      keywords: ['colors', 'typography', 'spacing', 'tokens', 'design'],
      action: () => navigate('/primitives'),
      group: 'Navigation',
    },
    {
      id: 'nav-components',
      label: 'Core Components',
      description: 'Browse component library',
      icon: Box,
      keywords: ['components', 'ui', 'library', 'widgets'],
      action: () => navigate('/components'),
      group: 'Navigation',
    },
    {
      id: 'nav-patterns',
      label: 'Patterns & Recipes',
      description: 'Common patterns and best practices',
      icon: Puzzle,
      keywords: ['patterns', 'recipes', 'examples', 'best practices'],
      action: () => navigate('/patterns'),
      group: 'Navigation',
    },
    {
      id: 'nav-playground',
      label: 'Playground',
      description: 'Interactive component playground',
      icon: PlayCircle,
      keywords: ['playground', 'sandbox', 'test', 'interactive'],
      action: () => navigate('/playground'),
      group: 'Navigation',
    },
    {
      id: 'nav-demo',
      label: 'Config Demo',
      description: 'Full configuration management demo',
      icon: Settings2,
      keywords: ['demo', 'config', 'application', 'showcase'],
      action: () => navigate('/demo'),
      group: 'Navigation',
    },
    // Theme
    {
      id: 'theme-light',
      label: 'Light Theme',
      icon: Sun,
      keywords: ['theme', 'light', 'appearance', 'mode'],
      action: () => setTheme('light'),
      group: 'Theme',
    },
    {
      id: 'theme-dark',
      label: 'Dark Theme',
      icon: Moon,
      keywords: ['theme', 'dark', 'appearance', 'mode'],
      action: () => setTheme('dark'),
      group: 'Theme',
    },
    {
      id: 'theme-system',
      label: 'System Theme',
      icon: Monitor,
      keywords: ['theme', 'system', 'auto', 'appearance'],
      action: () => setTheme('system'),
      group: 'Theme',
    },
    // Density
    {
      id: 'density-comfortable',
      label: 'Comfortable Density',
      icon: Maximize,
      keywords: ['density', 'comfortable', 'spacing', 'relaxed'],
      action: () => setDensity('comfortable'),
      group: 'Preferences',
    },
    {
      id: 'density-compact',
      label: 'Compact Density',
      icon: Minimize,
      keywords: ['density', 'compact', 'tight', 'dense'],
      action: () => setDensity('compact'),
      group: 'Preferences',
    },
  ];

  const fuse = new Fuse(actions, {
    keys: ['label', 'description', 'keywords'],
    threshold: 0.4,
    includeScore: true,
  });

  useEffect(() => {
    if (search) {
      const searchResults = fuse.search(search);
      setResults(searchResults.map((r) => r.item));
    } else {
      setResults(actions);
    }
  }, [search]);

  const handleSelect = useCallback((action: CommandAction) => {
    action.action();
    setCommandPaletteOpen(false);
    setSearch('');
  }, [setCommandPaletteOpen]);

  // Keyboard shortcut
  useEffect(() => {
    const down = (e: KeyboardEvent) => {
      if (e.key === 'k' && (e.metaKey || e.ctrlKey)) {
        e.preventDefault();
        setCommandPaletteOpen(!commandPaletteOpen);
      }
    };

    document.addEventListener('keydown', down);
    return () => document.removeEventListener('keydown', down);
  }, [commandPaletteOpen, setCommandPaletteOpen]);

  // Group results
  const groupedResults = results.reduce((acc, action) => {
    if (!acc[action.group]) {
      acc[action.group] = [];
    }
    acc[action.group].push(action);
    return acc;
  }, {} as Record<string, CommandAction[]>);

  return (
    <CommandDialog open={commandPaletteOpen} onOpenChange={setCommandPaletteOpen}>
      <CommandInput
        placeholder="Type a command or search..."
        value={search}
        onValueChange={setSearch}
      />
      <CommandList>
        <CommandEmpty>No results found.</CommandEmpty>
        {Object.entries(groupedResults).map(([group, items], index) => (
          <React.Fragment key={group}>
            {index > 0 && <CommandSeparator />}
            <CommandGroup heading={group}>
              {items.map((action) => {
                const Icon = action.icon;
                return (
                  <CommandItem
                    key={action.id}
                    value={action.id}
                    onSelect={() => handleSelect(action)}
                  >
                    <Icon className="mr-2 h-4 w-4" />
                    <div className="flex flex-1 flex-col">
                      <span>{action.label}</span>
                      {action.description && (
                        <span className="text-xs text-muted-foreground">
                          {action.description}
                        </span>
                      )}
                    </div>
                    {action.shortcut && (
                      <CommandShortcut>{action.shortcut}</CommandShortcut>
                    )}
                  </CommandItem>
                );
              })}
            </CommandGroup>
          </React.Fragment>
        ))}
      </CommandList>
    </CommandDialog>
  );
};
