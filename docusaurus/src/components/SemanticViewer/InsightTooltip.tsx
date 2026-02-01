import React from "react";
import type { SemanticInsight, InsightData } from "@site/src/types/semantic-profile";

export function InsightTooltip({ insight }: { insight: SemanticInsight }) {
  return (
    <div style={{ minWidth: 240 }}>
      <div style={{ fontWeight: 700, marginBottom: 6, fontSize: 11, textTransform: "uppercase", opacity: 0.7 }}>
        {formatCategory(insight.category)}
      </div>
      <InsightDataRenderer data={insight.data} />
      <InsightNarrative insight={insight} />
    </div>
  );
}

function InsightDataRenderer({ data }: { data: InsightData }) {
  switch (data.type) {
    case "TypeInference":
      return (
        <div>
          <div>
            <strong>Inferred:</strong> <code>{data.inferredType}</code>
          </div>
          {data.declaredType && (
            <div style={{ marginTop: 4, opacity: 0.8 }}>
              Declared: <code>{data.declaredType}</code>
            </div>
          )}
          {data.typeArguments && data.typeArguments.length > 0 && (
            <div style={{ marginTop: 4 }}>
              Type args: {data.typeArguments.map((t, i) => <code key={i}>{t}</code>).reduce((a, b) => <>{a}, {b}</>)}
            </div>
          )}
        </div>
      );

    case "SmartCast":
      return (
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <code>{data.originalType}</code>
            <span style={{ opacity: 0.5 }}>→</span>
            <code style={{ color: "var(--ifm-color-success)" }}>{data.narrowedType}</code>
          </div>
          <div style={{ marginTop: 4, fontSize: 12, opacity: 0.8 }}>
            Evidence: {data.evidenceKind} at line {data.evidencePosition.from.line}
          </div>
        </div>
      );

    case "Scoping":
      return (
        <div>
          {data.scopeFunction && <div>Scope function: <code>{data.scopeFunction}</code></div>}
          {data.innerReceiver && (
            <div style={{ marginTop: 4 }}>
              <code>this</code> is now <code>{data.innerReceiver}</code>
            </div>
          )}
          {data.itParameterType && (
            <div style={{ marginTop: 4 }}>
              <code>it</code> has type <code>{data.itParameterType}</code>
            </div>
          )}
        </div>
      );

    case "Extension":
      return (
        <div>
          <div>
            <code>{data.functionOrProperty}</code> on <code>{data.extensionReceiverType}</code>
          </div>
          <div style={{ marginTop: 4, fontSize: 12, opacity: 0.8 }}>
            From: {data.resolvedFrom}
          </div>
          {data.competingMember && (
            <div style={{ marginTop: 4, fontSize: 12, color: "var(--ifm-color-warning)" }}>
              Shadowed member function exists
            </div>
          )}
        </div>
      );

    case "Lambda":
      return (
        <div>
          <div>
            ({data.parameterTypes.map((p, i) => (
              <span key={i}>
                {p.name ? `${p.name}: ` : ""}<code>{p.type}</code>
                {i < data.parameterTypes.length - 1 && ", "}
              </span>
            ))}) → <code>{data.returnType}</code>
          </div>
          {data.samInterface && (
            <div style={{ marginTop: 4 }}>SAM: <code>{data.samInterface}</code></div>
          )}
        </div>
      );

    case "Overload":
      return (
        <div>
          <div><code>{data.selectedSignature}</code></div>
          <div style={{ marginTop: 4, fontSize: 12, opacity: 0.8 }}>
            Selected from {data.candidateCount} candidates
          </div>
          {data.resolutionFactors.length > 0 && (
            <div style={{ marginTop: 4, fontSize: 12 }}>
              Factors: {data.resolutionFactors.join(", ")}
            </div>
          )}
        </div>
      );

    case "Nullability":
      return (
        <div>
          <div><code>{data.nullableType}</code></div>
          {data.isPlatformType && (
            <div style={{ marginTop: 4, color: "var(--ifm-color-warning)" }}>
              Platform type (nullability unknown)
            </div>
          )}
          {data.narrowedToNonNull && (
            <div style={{ marginTop: 4, color: "var(--ifm-color-success)" }}>
              Narrowed to non-null in this scope
            </div>
          )}
        </div>
      );

    default:
      return <div>Unknown insight type</div>;
  }
}

function InsightNarrative({ insight }: { insight: SemanticInsight }) {
  const narrative = getNarrative(insight);
  if (!narrative) return null;

  return (
    <div style={{ marginTop: 8, paddingTop: 8, borderTop: "1px solid var(--ifm-color-emphasis-200)", fontSize: 13, opacity: 0.9 }}>
      {narrative}
    </div>
  );
}

function getNarrative(insight: SemanticInsight): string | null {
  switch (insight.data.type) {
    case "TypeInference":
      return "The compiler infers this type from the expression — no annotation needed.";
    case "SmartCast":
      return `After the ${insight.data.evidenceKind}, the compiler knows this is ${insight.data.narrowedType}.`;
    case "Scoping":
      return insight.data.scopeFunction
        ? `Inside ${insight.data.scopeFunction}, the receiver changes for cleaner syntax.`
        : null;
    case "Extension":
      return insight.data.competingMember
        ? "This extension is called because it's more specific than the member."
        : "This extension function feels native but comes from a library.";
    default:
      return null;
  }
}

function formatCategory(category: string): string {
  return category.replace(/_/g, " ");
}
