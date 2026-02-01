import React from "react";
import CodeBlock from "@theme-original/CodeBlock";
import {AnnotatedCode} from "@site/src/components/AnnotatedCode";
import {SemanticViewer} from "@site/src/components/SemanticViewer";

type CodeBlockProps = React.ComponentProps<typeof CodeBlock>;

function extractLanguage(className?: string): string | null {
    if (!className) return null;
    const match = className.match(/language-([^\s]+)/);
    return match ? match[1] : null;
}

function extractMeta(metastring?: string): { id?: string; semantic?: boolean } {
    if (!metastring) return {};
    const idMatch = metastring.match(/\bid=(?:"([^"]+)"|'([^']+)'|([^\s]+))/);
    const id = idMatch ? (idMatch[1] ?? idMatch[2] ?? idMatch[3]) : undefined;
    const semantic = /\bsemantic\b/.test(metastring);
    return { id, semantic };
}

function extractCode(children: CodeBlockProps["children"]): string {
    return React.Children.toArray(children)
        .map(child => (typeof child === "string" ? child : ""))
        .join("");
}

export default function CodeBlockWrapper(props: CodeBlockProps) {
    const language = extractLanguage(props.className);
    const { id, semantic } = extractMeta(props.metastring);

    if (language === "kotlin" && id != null) {
        if (semantic) {
            return <SemanticViewer snippetId={id} />;
        }
        return (
            <AnnotatedCode
                snippetId={id}
                language={language}
                code={extractCode(props.children)}
            />
        );
    }

    return <CodeBlock {...props} />;
}
