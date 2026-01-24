// src/components/CodeToggle/index.tsx
import React from "react";
import CodeBlock from "@theme/CodeBlock";

type Option = {
    label: string;
    language: string;
    code: string;
    badge?: string;
};

export function CodeToggle(props: { id: string; options: Option[] }) {
    const { id, options } = props;
    const storageKey = `codeToggle:${id}`;

    const [idx, setIdx] = React.useState(0);

    React.useEffect(() => {
        if (typeof window === "undefined") return;
        const raw = window.localStorage.getItem(storageKey);
        if (raw != null) {
            const n = Number(raw);
            if (!Number.isNaN(n) && n >= 0 && n < options.length) setIdx(n);
        }
    }, [storageKey, options.length]);

    React.useEffect(() => {
        if (typeof window === "undefined") return;
        window.localStorage.setItem(storageKey, String(idx));
    }, [storageKey, idx]);

    const current = options[idx];

    return (
        <div style={{ margin: "16px 0" }}>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 8 }}>
                {options.map((o, i) => (
                    <button
                        key={o.label}
                        type="button"
                        onClick={() => setIdx(i)}
                        aria-pressed={i === idx}
                        style={{
                            padding: "6px 10px",
                            borderRadius: 999,
                            border: "1px solid var(--ifm-color-emphasis-300)",
                            background: i === idx ? "var(--ifm-color-emphasis-200)" : "transparent",
                            cursor: "pointer",
                            fontWeight: 600,
                        }}
                    >
                        {o.label}{o.badge ? ` • ${o.badge}` : ""}
                    </button>
                ))}
            </div>

            <CodeBlock language={current.language}>{current.code}</CodeBlock>
        </div>
    );
}
