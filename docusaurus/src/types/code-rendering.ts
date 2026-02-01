// Shared position types used by all code rendering components

export type Position = {
  line: number;  // 1-based
  col: number;   // 1-based
};

export type Range = {
  from: Position;
  to: Position;
};

export type TokenPosition = {
  lineIndex: number;    // 0-based (for array indexing)
  tokenIndex: number;   // 0-based within line
  charStart: number;    // 1-based column where token starts
  charEnd: number;      // 1-based column where token ends (inclusive)
};

export type LineAnnotation = {
  id: string;
  label: string | number;
  range: Range;
  title?: string;
  body: React.ReactNode;
};

export type HighlightRegion = {
  range: Range;
  className?: string;
  style?: React.CSSProperties;
};
