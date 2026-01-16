import React, { useEffect } from 'react';
import { cn } from '@/lib/utils';
import { useAppStore, applyTheme, applyDensity, applyReducedMotion } from '@/lib/store';
import { TopBar } from './TopBar';
import { CommandPalette } from '../command/CommandPalette';

interface AppShellProps {
  children: React.ReactNode;
}

export const AppShell: React.FC<AppShellProps> = ({ children }) => {
  const { theme, density, reducedMotion, sidebarCollapsed } = useAppStore();

  // Apply theme, density, and reduced motion on mount and changes
  useEffect(() => {
    applyTheme(theme);
  }, [theme]);

  useEffect(() => {
    applyDensity(density);
  }, [density]);

  useEffect(() => {
    applyReducedMotion(reducedMotion);
  }, [reducedMotion]);

  // Listen for system theme changes
  useEffect(() => {
    if (theme === 'system') {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      const handleChange = () => applyTheme('system');
      mediaQuery.addEventListener('change', handleChange);
      return () => mediaQuery.removeEventListener('change', handleChange);
    }
  }, [theme]);

  return (
    <div className="flex min-h-screen w-full bg-background">
      {/* Command Palette */}
      <CommandPalette />

      {/* Main Content Area */}
      <div
        className={cn(
          'flex flex-1 flex-col transition-all duration-normal',
          sidebarCollapsed ? 'ml-16' : 'ml-64'
        )}
      >
        {/* Top Bar */}
        <TopBar />

        {/* Page Content */}
        <main className="flex-1 overflow-auto">
          <div className="container max-w-7xl py-6">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
};
