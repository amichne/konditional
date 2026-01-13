import React from 'react';
import { cn } from '@/lib/utils';
import { LucideIcon } from 'lucide-react';

interface IconProps extends React.SVGAttributes<SVGSVGElement> {
  icon: LucideIcon;
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
  color?: 'default' | 'muted' | 'accent' | 'success' | 'warning' | 'error' | 'info';
}

const sizeClasses = {
  xs: 'h-3 w-3',
  sm: 'h-4 w-4',
  md: 'h-5 w-5',
  lg: 'h-6 w-6',
  xl: 'h-8 w-8',
};

const colorClasses = {
  default: 'text-foreground',
  muted: 'text-muted-foreground',
  accent: 'text-accent',
  success: 'text-success',
  warning: 'text-warning',
  error: 'text-error',
  info: 'text-info',
};

export const Icon = React.forwardRef<SVGSVGElement, IconProps>(
  ({ icon: IconComponent, size = 'md', color = 'default', className, ...props }, ref) => {
    return (
      <IconComponent
        ref={ref}
        className={cn(sizeClasses[size], colorClasses[color], className)}
        {...props}
      />
    );
  }
);
Icon.displayName = 'Icon';

// Icon Button wrapper for consistent icon button styling
interface IconWrapperProps extends React.HTMLAttributes<HTMLSpanElement> {
  children: React.ReactNode;
}

export const IconWrapper = React.forwardRef<HTMLSpanElement, IconWrapperProps>(
  ({ className, children, ...props }, ref) => {
    return (
      <span
        ref={ref}
        className={cn(
          'inline-flex items-center justify-center shrink-0',
          className
        )}
        {...props}
      >
        {children}
      </span>
    );
  }
);
IconWrapper.displayName = 'IconWrapper';
