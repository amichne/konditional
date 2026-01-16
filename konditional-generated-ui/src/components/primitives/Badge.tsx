import React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const badgeVariants = cva(
  'inline-flex items-center gap-1 rounded-full font-medium transition-colors',
  {
    variants: {
      variant: {
        default: 'bg-primary/10 text-primary border border-primary/20',
        secondary: 'bg-secondary text-secondary-foreground border border-border',
        success: 'bg-success/10 text-success border border-success/20',
        warning: 'bg-warning/10 text-warning border border-warning/20',
        error: 'bg-error/10 text-error border border-error/20',
        info: 'bg-info/10 text-info border border-info/20',
        outline: 'border border-border text-foreground',
        ghost: 'text-muted-foreground',
      },
      size: {
        sm: 'px-1.5 py-0.5 text-[10px]',
        md: 'px-2 py-0.5 text-xs',
        lg: 'px-2.5 py-1 text-sm',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'md',
    },
  }
);

interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {
  dot?: boolean;
  removable?: boolean;
  onRemove?: () => void;
}

export const Badge = React.forwardRef<HTMLSpanElement, BadgeProps>(
  ({ className, variant, size, dot, removable, onRemove, children, ...props }, ref) => {
    return (
      <span
        ref={ref}
        className={cn(badgeVariants({ variant, size }), className)}
        {...props}
      >
        {dot && (
          <span
            className={cn(
              'h-1.5 w-1.5 rounded-full',
              variant === 'success' && 'bg-success',
              variant === 'warning' && 'bg-warning',
              variant === 'error' && 'bg-error',
              variant === 'info' && 'bg-info',
              (!variant || variant === 'default') && 'bg-primary',
              variant === 'secondary' && 'bg-muted-foreground',
              variant === 'outline' && 'bg-foreground',
              variant === 'ghost' && 'bg-muted-foreground'
            )}
          />
        )}
        {children}
        {removable && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onRemove?.();
            }}
            className="ml-0.5 inline-flex h-3 w-3 items-center justify-center rounded-full hover:bg-foreground/10"
            aria-label="Remove"
          >
            <svg
              className="h-2 w-2"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        )}
      </span>
    );
  }
);
Badge.displayName = 'Badge';

// Status Badge with dot indicator
interface StatusBadgeProps {
  status: 'draft' | 'pending' | 'approved' | 'published' | 'rejected';
  className?: string;
}

const statusConfig: Record<StatusBadgeProps['status'], { label: string; variant: BadgeProps['variant'] }> = {
  draft: { label: 'Draft', variant: 'secondary' },
  pending: { label: 'Pending', variant: 'warning' },
  approved: { label: 'Approved', variant: 'info' },
  published: { label: 'Published', variant: 'success' },
  rejected: { label: 'Rejected', variant: 'error' },
};

export const StatusBadge = ({ status, className }: StatusBadgeProps) => {
  const config = statusConfig[status];
  return (
    <Badge variant={config.variant} dot className={className}>
      {config.label}
    </Badge>
  );
};

// Environment Badge
interface EnvironmentBadgeProps {
  environment: 'development' | 'staging' | 'production';
  className?: string;
}

const envConfig: Record<EnvironmentBadgeProps['environment'], { label: string; variant: BadgeProps['variant'] }> = {
  development: { label: 'Dev', variant: 'info' },
  staging: { label: 'Stage', variant: 'warning' },
  production: { label: 'Prod', variant: 'error' },
};

export const EnvironmentBadge = ({ environment, className }: EnvironmentBadgeProps) => {
  const config = envConfig[environment];
  return (
    <Badge variant={config.variant} size="sm" className={className}>
      {config.label}
    </Badge>
  );
};
