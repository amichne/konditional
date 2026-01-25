// src/components/AnnotatedCode/index.tsx
import React from "react";
import {Highlight} from "prism-react-renderer";
import {usePrismTheme} from "@docusaurus/theme-common";
import useBaseUrl from "@docusaurus/useBaseUrl";

type HoverPosition = {
    line: number;
    col: number;
};

type HoverEntry = {
    id: string;
    from: HoverPosition;
    to: HoverPosition;
    title?: string | null;
    body: string;
};

export type HoverMap = {
    schemaVersion: number;
    snippetId: string;
    codeHash: string;
    language: string;
    code: string;
    hovers: HoverEntry[];
};

export type Annotation = {
    id: string;
    label: string | number;
    fromLine: number; // 1-based
    toLine?: number;  // 1-based inclusive
    fromCol?: number; // 1-based inclusive
    toCol?: number;   // 1-based inclusive
    title?: string;
    body: React.ReactNode;
};

type HoverLoadState = {
    map: HoverMap | null;
    status: "idle" | "loading" | "ready" | "error";
    error: string | null;
    url: string | null;
};

const hoverMapCache = new Map<string, HoverMap>();

function renderInlineMarkdown(text: string, keyPrefix: string): React.ReactNode[] {
    const parts: React.ReactNode[] = [];
    const codeChunks = text.split(/(`[^`]+`)/g);
    codeChunks.forEach((chunk, codeIndex) => {
        if (chunk.startsWith("`") && chunk.endsWith("`")) {
            parts.push(
                <code key={`${keyPrefix}-code-${codeIndex}`}>{chunk.slice(1, -1)}</code>
            );
            return;
        }
        const italicsChunks = chunk.split(/(_[^_]+_)/g);
        italicsChunks.forEach((piece, italicsIndex) => {
            if (piece.startsWith("_") && piece.endsWith("_")) {
                parts.push(
                    <em key={`${keyPrefix}-em-${codeIndex}-${italicsIndex}`}>{piece.slice(1, -1)}</em>
                );
                return;
            }
            if (piece.length > 0) {
                parts.push(
                    <React.Fragment key={`${keyPrefix}-text-${codeIndex}-${italicsIndex}`}>{piece}</React.Fragment>
                );
            }
        });
    });
    return parts;
}

function HoverBody(props: { content: string }) {
    const lines = React.useMemo(() => props.content.split("\n"), [props.content]);
    return (
        <div>
            {lines.map((line, index) => {
                if (line.trim().length === 0) {
                    return <div key={`line-${index}`} style={{ height: 8 }} />;
                }
                return (
                    <div key={`line-${index}`} style={{ marginTop: index === 0 ? 0 : 6 }}>
                        {renderInlineMarkdown(line, `line-${index}`)}
                    </div>
                );
            })}
        </div>
    );
}

function useHoverMap(
    snippetId: string | undefined,
    hoverMap: HoverMap | undefined,
    hoverMapUrl: string | undefined
): HoverLoadState {
    const baseHoverMapUrl = useBaseUrl("hovermaps/");
    const resolvedUrl = React.useMemo(() => {
        if (hoverMapUrl != null) return hoverMapUrl;
        if (snippetId == null) return null;
        return `${baseHoverMapUrl}${encodeURIComponent(snippetId)}.json`;
    }, [hoverMapUrl, snippetId, baseHoverMapUrl]);

    const [state, setState] = React.useState<HoverLoadState>({
        map: hoverMap ?? null,
        status: hoverMap != null ? "ready" : "idle",
        error: null,
        url: resolvedUrl,
    });

    React.useEffect(() => {
        if (hoverMap != null) {
            setState({ map: hoverMap, status: "ready", error: null, url: resolvedUrl });
            return;
        }
        if (resolvedUrl == null) {
            setState({ map: null, status: "idle", error: null, url: null });
            return;
        }
        const cached = hoverMapCache.get(resolvedUrl);
        if (cached != null) {
            setState({ map: cached, status: "ready", error: null, url: resolvedUrl });
            return;
        }
        let cancelled = false;
        setState(prev => ({ ...prev, status: "loading", error: null, url: resolvedUrl }));
        fetch(resolvedUrl)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status} loading ${resolvedUrl}`);
                }
                return response.json();
            })
            .then(data => {
                if (cancelled) return;
                hoverMapCache.set(resolvedUrl, data as HoverMap);
                setState({ map: data as HoverMap, status: "ready", error: null, url: resolvedUrl });
            })
            .catch(error => {
                if (cancelled) return;
                const message = error instanceof Error ? error.message : String(error);
                setState({ map: null, status: "error", error: message, url: resolvedUrl });
            });

        return () => {
            cancelled = true;
        };
    }, [hoverMap, resolvedUrl]);

    return state;
}

export function AnnotatedCode(props: {
    language?: string;
    code?: string;
    annotations?: Annotation[];
    snippetId?: string;
    hoverMap?: HoverMap;
    hoverMapUrl?: string;
}) {
    const { language, code, annotations, snippetId, hoverMap, hoverMapUrl } = props;
    const theme = usePrismTheme();
    const [activeId, setActiveId] = React.useState<string | null>(null);
    const hoverLoadState = useHoverMap(snippetId, hoverMap, hoverMapUrl);
    const resolvedHoverMap = hoverMap ?? hoverLoadState.map;

    const resolvedAnnotations = React.useMemo<Annotation[]>(() => {
        if (annotations != null) return annotations;
        if (resolvedHoverMap == null) return [];
        return resolvedHoverMap.hovers
            .map((hover, index) => ({
                id: hover.id,
                label: index + 1,
                fromLine: hover.from.line,
                toLine: hover.to.line,
                fromCol: hover.from.col,
                toCol: hover.to.col,
                title: hover.title ?? undefined,
                body: <HoverBody content={hover.body} />,
            }))
            .sort((a, b) => (a.fromLine - b.fromLine) || ((a.fromCol ?? 0) - (b.fromCol ?? 0)));
    }, [annotations, resolvedHoverMap]);

    const resolvedCode = resolvedHoverMap?.code ?? code ?? "";
    const resolvedLanguage = resolvedHoverMap?.language ?? language ?? "text";

    const byLine = React.useMemo(() => {
        const m = new Map<number, Annotation[]>();
        for (const a of resolvedAnnotations) {
            const key = a.fromLine;
            m.set(key, [...(m.get(key) ?? []), a]);
        }
        return m;
    }, [resolvedAnnotations]);

    const active = activeId ? resolvedAnnotations.find(a => a.id === activeId) : undefined;
    const activeFrom = active?.fromLine ?? -1;
    const activeTo = active?.toLine ?? activeFrom;
    const activeFromCol = active?.fromCol ?? -1;
    const activeToCol = active?.toCol ?? activeFromCol;
    const hasActiveCols = active?.fromCol != null && active?.toCol != null;

    const showStatus = hoverLoadState.status === "loading" || hoverLoadState.status === "error";
    const statusLabel = hoverLoadState.status === "loading"
        ? "Loading annotations..."
        : hoverLoadState.status === "error"
            ? `Unable to load hover map${hoverLoadState.error != null ? `: ${hoverLoadState.error}` : ""}`
            : null;

    const canRender = resolvedCode.trim().length > 0 || resolvedCode.length > 0;

    return (
        <div style={{ border: "1px solid var(--ifm-color-emphasis-300)", borderRadius: 8, overflow: "hidden" }}>
            {showStatus && statusLabel != null && (
                <div
                    style={{
                        padding: "8px 12px",
                        fontSize: 12,
                        borderBottom: "1px solid var(--ifm-color-emphasis-200)",
                        background: "var(--ifm-color-emphasis-100)",
                    }}
                >
                    {statusLabel}
                </div>
            )}
            {canRender ? (
                <Highlight theme={theme} code={resolvedCode} language={resolvedLanguage as any}>
                {({ className, style, tokens, getLineProps, getTokenProps }) => (
                    <pre className={className} style={{ ...style, margin: 0, padding: 12, overflowX: "auto" }}>
            {tokens.map((line, i) => {
                const lineNo = i + 1;
                const lineAnnos = byLine.get(lineNo) ?? [];
                const isActiveLine = activeId != null && lineNo >= activeFrom && lineNo <= activeTo;
                const shouldHighlightTokens = activeId != null && hasActiveCols && lineNo >= activeFrom && lineNo <= activeTo;

                let runningCol = 1;

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
                            {line.map((token, key) => {
                                const tokenStart = runningCol;
                                const tokenEnd = runningCol + token.content.length - 1;
                                runningCol += token.content.length;

                                const overlapsRange = (() => {
                                    if (!shouldHighlightTokens) return false;
                                    if (activeFrom === activeTo) {
                                        return tokenEnd >= activeFromCol && tokenStart <= activeToCol;
                                    }
                                    if (lineNo === activeFrom) {
                                        return tokenEnd >= activeFromCol;
                                    }
                                    if (lineNo === activeTo) {
                                        return tokenStart <= activeToCol;
                                    }
                                    return true;
                                })();

                                const tokenProps = getTokenProps({ token });
                                const tokenStyle = overlapsRange
                                    ? {
                                        ...tokenProps.style,
                                        background: "rgba(250, 204, 21, 0.35)",
                                        borderRadius: 2,
                                    }
                                    : tokenProps.style;
                                return (
                                    <span key={key} {...tokenProps} style={tokenStyle} />
                                );
                            })}
                        </div>
                    </div>
                );
            })}
          </pre>
                )}
            </Highlight>
            ) : (
                <div style={{ padding: 12, fontSize: 13, color: "var(--ifm-color-emphasis-600)" }}>
                    No code available to render.
                </div>
            )}
        </div>
    );
}
