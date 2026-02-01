import React from "react";
import {Highlight, Token} from "prism-react-renderer";
import {usePrismTheme} from "@docusaurus/theme-common";
import type {HighlightRegion, Range, TokenPosition} from "@site/src/types/code-rendering";

export type CodeRendererProps = {
  code: string;
  language: string;
  highlights?: HighlightRegion[];
  renderGutter?: (lineNumber: number) => React.ReactNode;
  renderToken?: (token: Token, defaultRender: React.ReactNode, position: TokenPosition) => React.ReactNode;
  className?: string;
};

export function CodeRenderer({
  code,
  language,
  highlights = [],
  renderGutter,
  renderToken,
  className,
}: CodeRendererProps) {
  const theme = usePrismTheme();

  return (
    <Highlight theme={theme} code={code} language={language as any}>
      {({ className: hlClassName, style, tokens, getLineProps, getTokenProps }) => (
        <pre
          className={`${hlClassName} ${className ?? ""}`}
          style={{ ...style, margin: 0, padding: 12, overflowX: "auto" }}
        >
          {tokens.map((line, lineIndex) => {
            const lineNumber = lineIndex + 1;
            let runningCol = 1;

            return (
              <div
                key={lineIndex}
                {...getLineProps({ line })}
                style={{
                  display: "grid",
                  gridTemplateColumns: renderGutter ? "48px 1fr" : "1fr",
                  gap: renderGutter ? 12 : 0,
                }}
              >
                {renderGutter && (
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "flex-end" }}>
                    {renderGutter(lineNumber)}
                  </div>
                )}
                <div style={{ minWidth: 0 }}>
                  {line.map((token, tokenIndex) => {
                    const charStart = runningCol;
                    const charEnd = runningCol + token.content.length - 1;
                    runningCol += token.content.length;

                    const tokenProps = getTokenProps({ token });
                    const position: TokenPosition = { lineIndex, tokenIndex, charStart, charEnd };

                    // Check if token overlaps any highlight region
                    const overlappingHighlight = highlights.find((h) =>
                      rangeOverlapsToken(h.range, lineNumber, charStart, charEnd)
                    );

                    const baseStyle = overlappingHighlight
                      ? { ...tokenProps.style, ...overlappingHighlight.style }
                      : tokenProps.style;

                    const defaultRender = (
                      <span
                        key={tokenIndex}
                        {...tokenProps}
                        className={`${tokenProps.className ?? ""} ${overlappingHighlight?.className ?? ""}`}
                        style={baseStyle}
                      />
                    );

                    return renderToken
                      ? renderToken(token, defaultRender, position)
                      : defaultRender;
                  })}
                </div>
              </div>
            );
          })}
        </pre>
      )}
    </Highlight>
  );
}

function rangeOverlapsToken(
  range: Range,
  lineNumber: number,
  charStart: number,
  charEnd: number
): boolean {
  const { from, to } = range;
  if (lineNumber < from.line || lineNumber > to.line) return false;
  if (from.line === to.line) {
    return charEnd >= from.col && charStart <= to.col;
  }
  if (lineNumber === from.line) return charEnd >= from.col;
  if (lineNumber === to.line) return charStart <= to.col;
  return true;
}
