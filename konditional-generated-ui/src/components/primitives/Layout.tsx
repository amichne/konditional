import React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

// Box - Basic container with styling
const boxVariants = cva('', {
  variants: {
    padding: {
      none: '',
      xs: 'p-1',
      sm: 'p-2',
      md: 'p-4',
      lg: 'p-6',
      xl: 'p-8',
    },
    rounded: {
      none: '',
      sm: 'rounded-sm',
      md: 'rounded-md',
      lg: 'rounded-lg',
      xl: 'rounded-xl',
      full: 'rounded-full',
    },
    border: {
      none: '',
      default: 'border border-border',
      accent: 'border border-accent',
    },
    background: {
      none: '',
      surface0: 'bg-surface-0',
      surface1: 'bg-surface-1',
      surface2: 'bg-surface-2',
      surface3: 'bg-surface-3',
      card: 'bg-card',
      muted: 'bg-muted',
      accent: 'bg-accent/10',
    },
  },
  defaultVariants: {
    padding: 'none',
    rounded: 'none',
    border: 'none',
    background: 'none',
  },
});

interface BoxProps extends React.HTMLAttributes<HTMLDivElement>, VariantProps<typeof boxVariants> {
  as?: React.ElementType;
}

export const Box = React.forwardRef<HTMLDivElement, BoxProps>(
  ({ className, padding, rounded, border, background, as: Component = 'div', ...props }, ref) => {
    return (
      <Component
        ref={ref}
        className={cn(boxVariants({ padding, rounded, border, background }), className)}
        {...props}
      />
    );
  }
);
Box.displayName = 'Box';

// Stack - Vertical flex container
const stackVariants = cva('flex flex-col', {
  variants: {
    gap: {
      none: 'gap-0',
      xs: 'gap-1',
      sm: 'gap-2',
      md: 'gap-4',
      lg: 'gap-6',
      xl: 'gap-8',
    },
    align: {
      start: 'items-start',
      center: 'items-center',
      end: 'items-end',
      stretch: 'items-stretch',
    },
    justify: {
      start: 'justify-start',
      center: 'justify-center',
      end: 'justify-end',
      between: 'justify-between',
      around: 'justify-around',
    },
  },
  defaultVariants: {
    gap: 'md',
    align: 'stretch',
    justify: 'start',
  },
});

interface StackProps extends React.HTMLAttributes<HTMLDivElement>, VariantProps<typeof stackVariants> {}

export const Stack = React.forwardRef<HTMLDivElement, StackProps>(
  ({ className, gap, align, justify, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn(stackVariants({ gap, align, justify }), className)}
        {...props}
      />
    );
  }
);
Stack.displayName = 'Stack';

// Inline - Horizontal flex container
const inlineVariants = cva('flex flex-row flex-wrap', {
  variants: {
    gap: {
      none: 'gap-0',
      xs: 'gap-1',
      sm: 'gap-2',
      md: 'gap-4',
      lg: 'gap-6',
      xl: 'gap-8',
    },
    align: {
      start: 'items-start',
      center: 'items-center',
      end: 'items-end',
      baseline: 'items-baseline',
      stretch: 'items-stretch',
    },
    justify: {
      start: 'justify-start',
      center: 'justify-center',
      end: 'justify-end',
      between: 'justify-between',
      around: 'justify-around',
    },
    wrap: {
      wrap: 'flex-wrap',
      nowrap: 'flex-nowrap',
      reverse: 'flex-wrap-reverse',
    },
  },
  defaultVariants: {
    gap: 'sm',
    align: 'center',
    justify: 'start',
    wrap: 'wrap',
  },
});

interface InlineProps extends React.HTMLAttributes<HTMLDivElement>, VariantProps<typeof inlineVariants> {}

export const Inline = React.forwardRef<HTMLDivElement, InlineProps>(
  ({ className, gap, align, justify, wrap, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn(inlineVariants({ gap, align, justify, wrap }), className)}
        {...props}
      />
    );
  }
);
Inline.displayName = 'Inline';

// Grid - CSS Grid container
const gridVariants = cva('grid', {
  variants: {
    cols: {
      1: 'grid-cols-1',
      2: 'grid-cols-2',
      3: 'grid-cols-3',
      4: 'grid-cols-4',
      5: 'grid-cols-5',
      6: 'grid-cols-6',
      12: 'grid-cols-12',
      auto: 'grid-cols-[repeat(auto-fit,minmax(250px,1fr))]',
    },
    gap: {
      none: 'gap-0',
      xs: 'gap-1',
      sm: 'gap-2',
      md: 'gap-4',
      lg: 'gap-6',
      xl: 'gap-8',
    },
    align: {
      start: 'items-start',
      center: 'items-center',
      end: 'items-end',
      stretch: 'items-stretch',
    },
  },
  defaultVariants: {
    cols: 1,
    gap: 'md',
    align: 'stretch',
  },
});

interface GridProps extends React.HTMLAttributes<HTMLDivElement>, VariantProps<typeof gridVariants> {}

export const Grid = React.forwardRef<HTMLDivElement, GridProps>(
  ({ className, cols, gap, align, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn(gridVariants({ cols, gap, align }), className)}
        {...props}
      />
    );
  }
);
Grid.displayName = 'Grid';

// Center - Centers content both horizontally and vertically
interface CenterProps extends React.HTMLAttributes<HTMLDivElement> {
  inline?: boolean;
}

export const Center = React.forwardRef<HTMLDivElement, CenterProps>(
  ({ className, inline, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn(
          'flex items-center justify-center',
          inline ? 'inline-flex' : 'flex',
          className
        )}
        {...props}
      />
    );
  }
);
Center.displayName = 'Center';

// Spacer - Flexible spacer for flex layouts
export const Spacer = ({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) => (
  <div className={cn('flex-1', className)} {...props} />
);

// Divider - Visual separator
const dividerVariants = cva('shrink-0 bg-border', {
  variants: {
    orientation: {
      horizontal: 'h-px w-full',
      vertical: 'w-px h-full self-stretch',
    },
    spacing: {
      none: '',
      sm: '',
      md: '',
      lg: '',
    },
  },
  compoundVariants: [
    { orientation: 'horizontal', spacing: 'sm', className: 'my-2' },
    { orientation: 'horizontal', spacing: 'md', className: 'my-4' },
    { orientation: 'horizontal', spacing: 'lg', className: 'my-6' },
    { orientation: 'vertical', spacing: 'sm', className: 'mx-2' },
    { orientation: 'vertical', spacing: 'md', className: 'mx-4' },
    { orientation: 'vertical', spacing: 'lg', className: 'mx-6' },
  ],
  defaultVariants: {
    orientation: 'horizontal',
    spacing: 'none',
  },
});

interface DividerProps extends React.HTMLAttributes<HTMLDivElement>, VariantProps<typeof dividerVariants> {
  decorative?: boolean;
}

export const Divider = React.forwardRef<HTMLDivElement, DividerProps>(
  ({ className, orientation, spacing, decorative = true, ...props }, ref) => {
    return (
      <div
        ref={ref}
        role={decorative ? 'none' : 'separator'}
        aria-orientation={!decorative ? orientation || 'horizontal' : undefined}
        className={cn(dividerVariants({ orientation, spacing }), className)}
        {...props}
      />
    );
  }
);
Divider.displayName = 'Divider';
