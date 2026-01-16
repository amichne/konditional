import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import Fuse from 'fuse.js';
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
  const [actions, setActions] = useState<CommandAction[]>([]); // Dynamically load actions

  useEffect(() => {
    const loadActions = async () => {
      const config = fetchSnapshot() ; // Fetch the UI config dynamically
        config.then((data) => {
            const dynamicActions = data.flags.map((feature: any) => ({
                id: feature.id,
                label: feature.name,
                description: feature.description,
                icon: FileCode, // Default icon for dynamic features
                keywords: feature.keywords || [],
                action: () => todo("Implement action for " + feature.name),
                group: feature.group || 'Dynamic Features',
            }));
            setActions(dynamicActions);
        });

    }


    loadActions();
  }, [navigate]);

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
  }, [search, actions]);

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
