import React from 'react';
import { cn } from '@/lib/utils';
import { cva, type VariantProps } from 'class-variance-authority';

const skeletonVariants = cva(
  'relative overflow-hidden rounded bg-muted',
  {
    variants: {
      animation: {
        pulse: 'animate-pulse',
        shimmer: 'before:absolute before:inset-0 before:-translate-x-full before:bg-gradient-to-r before:from-transparent before:via-surface-0/40 before:to-transparent before:animate-shimmer',
        none: '',
      },
    },
    defaultVariants: {
      animation: 'shimmer',
    },
  }
);

interface SkeletonProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof skeletonVariants> {
  width?: string | number;
  height?: string | number;
}

export const Skeleton = React.forwardRef<HTMLDivElement, SkeletonProps>(
  ({ className, animation, width, height, style, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn(skeletonVariants({ animation }), className)}
        style={{
          width: typeof width === 'number' ? `${width}px` : width,
          height: typeof height === 'number' ? `${height}px` : height,
          ...style,
        }}
        {...props}
      />
    );
  }
);
Skeleton.displayName = 'Skeleton';

// Text skeleton
interface SkeletonTextProps extends SkeletonProps {
  lines?: number;
}

export const SkeletonText = React.forwardRef<HTMLDivElement, SkeletonTextProps>(
  ({ className, lines = 1, ...props }, ref) => {
    return (
      <div ref={ref} className={cn('space-y-2', className)}>
        {Array.from({ length: lines }).map((_, i) => (
          <Skeleton
            key={i}
            height={16}
            className={cn(
              'rounded',
              i === lines - 1 && lines > 1 && 'w-3/4'
            )}
            {...props}
          />
        ))}
      </div>
    );
  }
);
SkeletonText.displayName = 'SkeletonText';

// Avatar skeleton
export const SkeletonAvatar = React.forwardRef<HTMLDivElement, SkeletonProps>(
  ({ className, ...props }, ref) => {
    return (
      <Skeleton
        ref={ref}
        className={cn('h-10 w-10 rounded-full', className)}
        {...props}
      />
    );
  }
);
SkeletonAvatar.displayName = 'SkeletonAvatar';

// Card skeleton
export const SkeletonCard = React.forwardRef<HTMLDivElement, SkeletonProps>(
  ({ className, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn('space-y-3 rounded-lg border border-border bg-card p-4', className)}
        {...props}
      >
        <Skeleton height={20} className="w-1/2" />
        <SkeletonText lines={3} />
        <div className="flex gap-2 pt-2">
          <Skeleton height={32} className="w-20 rounded-md" />
          <Skeleton height={32} className="w-20 rounded-md" />
        </div>
      </div>
    );
  }
);
SkeletonCard.displayName = 'SkeletonCard';

// Table row skeleton
export const SkeletonTableRow = React.forwardRef<HTMLDivElement, SkeletonProps & { columns?: number }>(
  ({ className, columns = 4, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn('flex items-center gap-4 py-3', className)}
        {...props}
      >
        {Array.from({ length: columns }).map((_, i) => (
          <Skeleton
            key={i}
            height={16}
            className={cn(
              'flex-1',
              i === 0 && 'max-w-[200px]'
            )}
          />
        ))}
      </div>
    );
  }
);
SkeletonTableRow.displayName = 'SkeletonTableRow';
