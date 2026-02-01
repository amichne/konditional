import React from "react";

export type InsightBadgeProps = {
  label: string | number;
  isActive: boolean;
  onClick: () => void;
  onMouseEnter: () => void;
  onMouseLeave: () => void;
  children?: React.ReactNode; // Tooltip content when active
};

export function InsightBadge({
  label,
  isActive,
  onClick,
  onMouseEnter,
  onMouseLeave,
  children,
}: InsightBadgeProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
      onFocus={onMouseEnter}
      onBlur={onMouseLeave}
      aria-pressed={isActive}
      aria-haspopup={children != null}
      style={{
        width: 22,
        height: 22,
        borderRadius: 999,
        border: "1px solid var(--ifm-color-emphasis-400)",
        background: isActive ? "var(--ifm-color-emphasis-200)" : "transparent",
        fontSize: 12,
        fontWeight: 700,
        cursor: "pointer",
        position: "relative",
      }}
    >
      {label}
      {isActive && children && (
        <TooltipOverlay>{children}</TooltipOverlay>
      )}
    </button>
  );
}

function TooltipOverlay({ children }: { children: React.ReactNode }) {
  return (
    <span
      role="tooltip"
      style={{
        position: "absolute",
        left: "110%",
        top: "50%",
        transform: "translateY(-50%)",
        minWidth: 260,
        maxWidth: 420,
        whiteSpace: "normal",
        textAlign: "left",
        padding: 10,
        borderRadius: 8,
        border: "1px solid var(--ifm-color-emphasis-300)",
        background: "var(--ifm-background-surface-color)",
        color: "var(--ifm-font-color-base)",
        boxShadow: "0 6px 18px rgba(0,0,0,0.12)",
        zIndex: 10,
      }}
    >
      {children}
    </span>
  );
}
