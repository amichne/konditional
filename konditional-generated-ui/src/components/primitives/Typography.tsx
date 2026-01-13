import React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

// Text component with typography variants
const textVariants = cva('', {
  variants: {
    variant: {
      body: 'text-sm leading-relaxed',
      'body-lg': 'text-base leading-relaxed',
      small: 'text-xs leading-normal',
      caption: 'text-xs leading-tight text-muted-foreground',
      label: 'text-sm font-medium leading-none',
      mono: 'font-mono text-sm',
    },
    weight: {
      normal: 'font-normal',
      medium: 'font-medium',
      semibold: 'font-semibold',
      bold: 'font-bold',
    },
    textColor: {
      default: 'text-foreground',
      muted: 'text-muted-foreground',
      accent: 'text-accent',
      success: 'text-success',
      warning: 'text-warning',
      error: 'text-error',
      info: 'text-info',
    },
    align: {
      left: 'text-left',
      center: 'text-center',
      right: 'text-right',
    },
  },
  defaultVariants: {
    variant: 'body',
    weight: 'normal',
    textColor: 'default',
    align: 'left',
  },
});

interface TextProps extends Omit<React.HTMLAttributes<HTMLSpanElement>, 'color'>, VariantProps<typeof textVariants> {
  as?: 'span' | 'p' | 'div' | 'label';
  truncate?: boolean;
}

export const Text = React.forwardRef<HTMLSpanElement, TextProps>(
  ({ className, variant, weight, textColor, align, as: Component = 'span', truncate, ...props }, ref) => {
    return (
      <Component
        ref={ref as any}
        className={cn(
          textVariants({ variant, weight, textColor, align }),
          truncate && 'truncate',
          className
        )}
        {...props}
      />
    );
  }
);
Text.displayName = 'Text';

// Heading component
const headingVariants = cva('font-semibold tracking-tight', {
  variants: {
    level: {
      1: 'text-4xl lg:text-5xl',
      2: 'text-3xl lg:text-4xl',
      3: 'text-2xl lg:text-3xl',
      4: 'text-xl lg:text-2xl',
      5: 'text-lg lg:text-xl',
      6: 'text-base lg:text-lg',
    },
    headingColor: {
      default: 'text-foreground',
      muted: 'text-muted-foreground',
      accent: 'text-accent',
      gradient: 'gradient-text',
    },
  },
  defaultVariants: {
    level: 2,
    headingColor: 'default',
  },
});

interface HeadingProps extends Omit<React.HTMLAttributes<HTMLHeadingElement>, 'color'>, VariantProps<typeof headingVariants> {
  as?: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
}

export const Heading = React.forwardRef<HTMLHeadingElement, HeadingProps>(
  ({ className, level = 2, headingColor, as, ...props }, ref) => {
    const Component = as || (`h${level}` as 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6');
    return (
      <Component
        ref={ref}
        className={cn(headingVariants({ level, headingColor }), className)}
        {...props}
      />
    );
  }
);
Heading.displayName = 'Heading';

// Code component for inline code
interface CodeProps extends React.HTMLAttributes<HTMLElement> {
  variant?: 'inline' | 'block';
}

export const Code = React.forwardRef<HTMLElement, CodeProps>(
  ({ className, variant = 'inline', children, ...props }, ref) => {
    if (variant === 'block') {
      return (
        <pre
          ref={ref as any}
          className={cn(
            'overflow-x-auto rounded-lg bg-surface-2 p-4 font-mono text-sm',
            className
          )}
          {...props}
        >
          <code>{children}</code>
        </pre>
      );
    }

    return (
      <code
        ref={ref}
        className={cn(
          'relative rounded bg-surface-2 px-[0.4em] py-[0.2em] font-mono text-sm',
          className
        )}
        {...props}
      >
        {children}
      </code>
    );
  }
);
Code.displayName = 'Code';

// Kbd for keyboard shortcuts
interface KbdProps extends React.HTMLAttributes<HTMLElement> {
  children: React.ReactNode;
}

export const Kbd = React.forwardRef<HTMLElement, KbdProps>(
  ({ className, children, ...props }, ref) => {
    return (
      <kbd
        ref={ref}
        className={cn(
          'inline-flex h-5 items-center justify-center rounded border border-border bg-surface-2 px-1.5 font-mono text-[10px] font-medium text-muted-foreground',
          className
        )}
        {...props}
      >
        {children}
      </kbd>
    );
  }
);
Kbd.displayName = 'Kbd';
