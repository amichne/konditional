import type { Range, Position } from "./code-rendering";

// Re-export for convenience
export type { Range, Position };

// ─────────────────────────────────────────────────────────────
// Insight Categories & Levels
// ─────────────────────────────────────────────────────────────

export type InsightCategory =
  | "TYPE_INFERENCE"
  | "NULLABILITY"
  | "SMART_CASTS"
  | "SCOPING"
  | "EXTENSIONS"
  | "LAMBDAS"
  | "OVERLOADS";

export const INSIGHT_CATEGORIES: InsightCategory[] = [
  "TYPE_INFERENCE",
  "NULLABILITY",
  "SMART_CASTS",
  "SCOPING",
  "EXTENSIONS",
  "LAMBDAS",
  "OVERLOADS",
];

export type InsightLevel = "OFF" | "HIGHLIGHTS" | "ALL";

// ─────────────────────────────────────────────────────────────
// Insight Kinds (per category)
// ─────────────────────────────────────────────────────────────

export type InsightKind =
  // TypeInference
  | "INFERRED_TYPE"
  | "EXPLICIT_TYPE"
  | "GENERIC_ARGUMENT_INFERRED"
  // Nullability
  | "NULLABLE_TYPE"
  | "PLATFORM_TYPE"
  | "NULL_SAFE_CALL"
  | "ELVIS_OPERATOR"
  | "NOT_NULL_ASSERTION"
  // SmartCasts
  | "IS_CHECK_CAST"
  | "WHEN_BRANCH_CAST"
  | "NEGATED_CHECK_EXIT"
  | "NULL_CHECK_CAST"
  // Scoping
  | "RECEIVER_CHANGE"
  | "IMPLICIT_THIS"
  | "SCOPE_FUNCTION_ENTRY"
  // Extensions
  | "EXTENSION_FUNCTION_CALL"
  | "EXTENSION_PROPERTY_ACCESS"
  | "MEMBER_VS_EXTENSION_RESOLUTION"
  // Lambdas
  | "LAMBDA_PARAMETER_INFERRED"
  | "LAMBDA_RETURN_INFERRED"
  | "SAM_CONVERSION"
  | "TRAILING_LAMBDA"
  // Overloads
  | "OVERLOAD_RESOLVED"
  | "DEFAULT_ARGUMENT_USED"
  | "NAMED_ARGUMENT_REORDER";

// ─────────────────────────────────────────────────────────────
// Scope Model
// ─────────────────────────────────────────────────────────────

export type ScopeKind =
  | "FILE"
  | "CLASS"
  | "FUNCTION"
  | "LAMBDA"
  | "SCOPE_FUNCTION"
  | "WHEN_BRANCH"
  | "IF_BRANCH"
  | "TRY_BLOCK"
  | "CATCH_BLOCK";

export type ScopeRef = {
  scopeId: string;
  kind: ScopeKind;
  receiverType?: string;
  position: Range;
};

export type ScopeNode = {
  ref: ScopeRef;
  children: ScopeNode[];
  insights: string[]; // insight IDs contained in this scope
};

// ─────────────────────────────────────────────────────────────
// Insight Data (discriminated union)
// ─────────────────────────────────────────────────────────────

export type TypeInferenceData = {
  type: "TypeInference";
  inferredType: string;
  declaredType?: string;
  typeArguments?: string[];
};

export type NullabilityData = {
  type: "Nullability";
  nullableType: string;
  isNullable: boolean;
  isPlatformType: boolean;
  narrowedToNonNull: boolean;
};

export type SmartCastData = {
  type: "SmartCast";
  originalType: string;
  narrowedType: string;
  evidencePosition: Range;
  evidenceKind: string;
};

export type ScopingData = {
  type: "Scoping";
  scopeFunction?: string;
  outerReceiver?: string;
  innerReceiver?: string;
  itParameterType?: string;
};

export type ExtensionData = {
  type: "Extension";
  functionOrProperty: string;
  extensionReceiverType: string;
  dispatchReceiverType?: string;
  resolvedFrom: string;
  competingMember: boolean;
};

export type LambdaParam = {
  name?: string;
  type: string;
};

export type LambdaData = {
  type: "Lambda";
  parameterTypes: LambdaParam[];
  returnType: string;
  inferredFromContext?: string;
  samInterface?: string;
};

export type OverloadData = {
  type: "Overload";
  selectedSignature: string;
  candidateCount: number;
  resolutionFactors: string[];
  defaultArgumentsUsed?: string[];
};

export type InsightData =
  | TypeInferenceData
  | NullabilityData
  | SmartCastData
  | ScopingData
  | ExtensionData
  | LambdaData
  | OverloadData;

// ─────────────────────────────────────────────────────────────
// Semantic Insight
// ─────────────────────────────────────────────────────────────

export type SemanticInsight = {
  id: string;
  position: Range;
  category: InsightCategory;
  level: InsightLevel;
  kind: InsightKind;
  scopeChain: ScopeRef[];
  data: InsightData;
  tokenText: string;
};

// ─────────────────────────────────────────────────────────────
// Semantic Profile (top-level)
// ─────────────────────────────────────────────────────────────

export type SemanticProfile = {
  snippetId: string;
  codeHash: string;
  code: string;
  insights: SemanticInsight[];
  rootScopes: ScopeNode[];
};
