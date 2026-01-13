import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { cn } from '@/lib/utils';
import { useAppStore } from '@/lib/store';
import {
  Palette,
  Box,
  Puzzle,
  PlayCircle,
  Settings2,
  ChevronLeft,
  ChevronRight,
  ChevronDown,
  Layers,
  FormInput,
  LayoutDashboard,
  Table2,
  Menu,
  MousePointer2,
  Bell,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible';

interface NavItem {
  label: string;
  href: string;
  icon: React.ElementType;
  children?: { label: string; href: string }[];
}

const navigation: NavItem[] = [
  {
    label: 'Design Primitives',
    href: '/primitives',
    icon: Palette,
    children: [
      { label: 'Colors', href: '/primitives/colors' },
      { label: 'Typography', href: '/primitives/typography' },
      { label: 'Spacing', href: '/primitives/spacing' },
      { label: 'Motion', href: '/primitives/motion' },
    ],
  },
  {
    label: 'Core Components',
    href: '/components',
    icon: Box,
    children: [
      { label: 'Layout', href: '/components/layout' },
      { label: 'Navigation', href: '/components/navigation' },
      { label: 'Overlays', href: '/components/overlays' },
      { label: 'Inputs', href: '/components/inputs' },
      { label: 'Data Display', href: '/components/data-display' },
    ],
  },
  {
    label: 'Patterns & Recipes',
    href: '/patterns',
    icon: Puzzle,
    children: [
      { label: 'Schema Forms', href: '/patterns/schema-forms' },
      { label: 'Safe Publishing', href: '/patterns/safe-publishing' },
      { label: 'Large Datasets', href: '/patterns/large-datasets' },
      { label: 'Accessibility', href: '/patterns/accessibility' },
    ],
  },
  {
    label: 'Playground',
    href: '/playground',
    icon: PlayCircle,
  },
  {
    label: 'Config Demo',
    href: '/demo',
    icon: Settings2,
  },
];

const NavItemComponent: React.FC<{
  item: NavItem;
  collapsed: boolean;
}> = ({ item, collapsed }) => {
  const location = useLocation();
  const isActive = location.pathname.startsWith(item.href);
  const hasChildren = item.children && item.children.length > 0;
  const [open, setOpen] = React.useState(isActive);

  const Icon = item.icon;

  if (collapsed) {
    return (
      <Link
        to={item.href}
        className={cn(
          'flex h-10 w-10 items-center justify-center rounded-lg transition-colors',
          isActive
            ? 'bg-accent text-accent-foreground'
            : 'text-muted-foreground hover:bg-muted hover:text-foreground'
        )}
        title={item.label}
      >
        <Icon className="h-5 w-5" />
      </Link>
    );
  }

  if (!hasChildren) {
    return (
      <Link
        to={item.href}
        className={cn(
          'flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors',
          isActive
            ? 'bg-accent text-accent-foreground font-medium'
            : 'text-muted-foreground hover:bg-muted hover:text-foreground'
        )}
      >
        <Icon className="h-4 w-4 shrink-0" />
        <span>{item.label}</span>
      </Link>
    );
  }

  return (
    <Collapsible open={open} onOpenChange={setOpen}>
      <CollapsibleTrigger asChild>
        <button
          className={cn(
            'flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors',
            isActive
              ? 'text-foreground font-medium'
              : 'text-muted-foreground hover:bg-muted hover:text-foreground'
          )}
        >
          <Icon className="h-4 w-4 shrink-0" />
          <span className="flex-1 text-left">{item.label}</span>
          <ChevronDown
            className={cn(
              'h-4 w-4 transition-transform',
              open && 'rotate-180'
            )}
          />
        </button>
      </CollapsibleTrigger>
      <CollapsibleContent className="ml-6 mt-1 space-y-1 border-l border-border pl-3">
        {item.children?.map((child) => (
          <Link
            key={child.href}
            to={child.href}
            className={cn(
              'block rounded-md px-3 py-1.5 text-sm transition-colors',
              location.pathname === child.href
                ? 'bg-accent/50 text-accent-foreground font-medium'
                : 'text-muted-foreground hover:bg-muted hover:text-foreground'
            )}
          >
            {child.label}
          </Link>
        ))}
      </CollapsibleContent>
    </Collapsible>
  );
};

export const SideNav: React.FC = () => {
  const { sidebarCollapsed, setSidebarCollapsed } = useAppStore();

  return (
    <aside
      className={cn(
        'fixed left-0 top-0 z-30 flex h-screen flex-col border-r border-border bg-sidebar transition-all duration-normal',
        sidebarCollapsed ? 'w-16' : 'w-64'
      )}
    >
      {/* Logo */}
      <div
        className={cn(
          'flex h-14 items-center border-b border-border px-4',
          sidebarCollapsed ? 'justify-center' : 'gap-3'
        )}
      >
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent">
          <Settings2 className="h-4 w-4 text-accent-foreground" />
        </div>
        {!sidebarCollapsed && (
          <div className="flex flex-col">
            <span className="text-sm font-semibold text-foreground">ConfigUI</span>
            <span className="text-[10px] text-muted-foreground">Component Library</span>
          </div>
        )}
      </div>

      {/* Navigation */}
      <nav className="flex-1 space-y-1 overflow-y-auto p-3">
        {navigation.map((item) => (
          <NavItemComponent
            key={item.href}
            item={item}
            collapsed={sidebarCollapsed}
          />
        ))}
      </nav>

      {/* Collapse Toggle */}
      <div className="border-t border-border p-3">
        <Button
          variant="ghost"
          size={sidebarCollapsed ? 'icon' : 'default'}
          onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
          className={cn(
            'w-full',
            sidebarCollapsed && 'h-10 w-10'
          )}
        >
          {sidebarCollapsed ? (
            <ChevronRight className="h-4 w-4" />
          ) : (
            <>
              <ChevronLeft className="h-4 w-4" />
              <span>Collapse</span>
            </>
          )}
        </Button>
      </div>
    </aside>
  );
};
