// src/components/AnnotatedCode/index.tsx
import React from "react";
import {Highlight} from "prism-react-renderer";
import {usePrismTheme} from "@docusaurus/theme-common";

type Annotation = {
    id: string;
    label: string | number;
    fromLine: number; // 1-based
    toLine?: number;  // 1-based inclusive
    title?: string;
    body: React.ReactNode;
};

export function AnnotatedCode(props: {
    language: string;
    code: string;
    annotations: Annotation[];
}) {
    const { language, code, annotations } = props;
    const theme = usePrismTheme();
    const [activeId, setActiveId] = React.useState<string | null>(null);

    const byLine = React.useMemo(() => {
        const m = new Map<number, Annotation[]>();
        for (const a of annotations) {
            const key = a.fromLine;
            m.set(key, [...(m.get(key) ?? []), a]);
        }
        return m;
    }, [annotations]);

    const active = activeId ? annotations.find(a => a.id === activeId) : undefined;
    const activeFrom = active?.fromLine ?? -1;
    const activeTo = active?.toLine ?? activeFrom;

    return (
        <div style={{ border: "1px solid var(--ifm-color-emphasis-300)", borderRadius: 8, overflow: "hidden" }}>
            <Highlight theme={theme} code={code} language={language as any}>
                {({ className, style, tokens, getLineProps, getTokenProps }) => (
                    <pre className={className} style={{ ...style, margin: 0, padding: 12, overflowX: "auto" }}>
            {tokens.map((line, i) => {
                const lineNo = i + 1;
                const lineAnnos = byLine.get(lineNo) ?? [];
                const isActiveLine = activeId != null && lineNo >= activeFrom && lineNo <= activeTo;

                return (
                    <div
                        key={i}
                        {...getLineProps({ line })}
                        style={{
                            display: "grid",
                            gridTemplateColumns: "48px 1fr",
                            gap: 12,
                            background: isActiveLine ? "var(--ifm-color-emphasis-100)" : "transparent",
                            borderRadius: 4,
                            padding: "0 4px",
                        }}
                    >
                        <div style={{ display: "flex", gap: 6, alignItems: "center", justifyContent: "flex-end" }}>
                            {lineAnnos.map(a => {
                                const isActive = a.id === activeId;
                                return (
                                    <button
                                        key={a.id}
                                        type="button"
                                        onClick={() => setActiveId(prev => (prev === a.id ? null : a.id))}
                                        onMouseEnter={() => setActiveId(a.id)}
                                        onMouseLeave={() => setActiveId(prev => (prev === a.id ? null : prev))}
                                        onFocus={() => setActiveId(a.id)}
                                        onBlur={() => setActiveId(prev => (prev === a.id ? null : prev))}
                                        aria-pressed={isActive}
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
                                        {a.label}
                                        {/* bubble */}
                                        {isActive && (
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
                              {a.title ? <div style={{ fontWeight: 800, marginBottom: 6 }}>{a.title}</div> : null}
                                                <div style={{ fontWeight: 500, lineHeight: 1.35 }}>{a.body}</div>
                            </span>
                                        )}
                                    </button>
                                );
                            })}
                            {/* optional: line number faint */}
                            <span style={{ opacity: 0.35, fontVariantNumeric: "tabular-nums", fontSize: 12 }}>
                      {String(lineNo).padStart(2, "0")}
                    </span>
                        </div>

                        <div style={{ minWidth: 0 }}>
                            {line.map((token, key) => (
                                <span key={key} {...getTokenProps({ token })} />
                            ))}
                        </div>
                    </div>
                );
            })}
          </pre>
                )}
            </Highlight>
        </div>
    );
}
