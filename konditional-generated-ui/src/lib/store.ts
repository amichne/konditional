import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type Theme = 'light' | 'dark' | 'system';
export type Density = 'comfortable' | 'compact';
export type Environment = 'development' | 'staging' | 'production';
export type Role = 'viewer' | 'editor' | 'admin';

interface AppState {
  // Theme
  theme: Theme;
  setTheme: (theme: Theme) => void;
  
  // Density
  density: Density;
  setDensity: (density: Density) => void;
  
  // Reduced motion
  reducedMotion: boolean;
  setReducedMotion: (reduced: boolean) => void;
  
  // Environment context
  environment: Environment;
  setEnvironment: (env: Environment) => void;
  
  // Role context
  role: Role;
  setRole: (role: Role) => void;
  
  // Sidebar
  sidebarCollapsed: boolean;
  setSidebarCollapsed: (collapsed: boolean) => void;
  
  // Command palette
  commandPaletteOpen: boolean;
  setCommandPaletteOpen: (open: boolean) => void;
}

export const useAppStore = create<AppState>()(
  persist(
    (set) => ({
      theme: 'dark',
      setTheme: (theme) => set({ theme }),
      
      density: 'comfortable',
      setDensity: (density) => set({ density }),
      
      reducedMotion: false,
      setReducedMotion: (reducedMotion) => set({ reducedMotion }),
      
      environment: 'development',
      setEnvironment: (environment) => set({ environment }),
      
      role: 'admin',
      setRole: (role) => set({ role }),
      
      sidebarCollapsed: false,
      setSidebarCollapsed: (sidebarCollapsed) => set({ sidebarCollapsed }),
      
      commandPaletteOpen: false,
      setCommandPaletteOpen: (commandPaletteOpen) => set({ commandPaletteOpen }),
    }),
    {
      name: 'config-ui-storage',
      partialize: (state) => ({
        theme: state.theme,
        density: state.density,
        reducedMotion: state.reducedMotion,
        sidebarCollapsed: state.sidebarCollapsed,
      }),
    }
  )
);

// Apply theme to document
export const applyTheme = (theme: Theme) => {
  const root = document.documentElement;
  const systemDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  
  if (theme === 'dark' || (theme === 'system' && systemDark)) {
    root.classList.add('dark');
  } else {
    root.classList.remove('dark');
  }
};

// Apply density to document
export const applyDensity = (density: Density) => {
  const root = document.documentElement;
  root.classList.remove('density-comfortable', 'density-compact');
  root.classList.add(`density-${density}`);
};

// Apply reduced motion to document
export const applyReducedMotion = (reduced: boolean) => {
  const root = document.documentElement;
  if (reduced) {
    root.classList.add('reduce-motion');
  } else {
    root.classList.remove('reduce-motion');
  }
};
