// src/theme/MDXComponents.tsx
import MDXComponents from "@theme-original/MDXComponents";
import {CodeToggle} from "@site/src/components/CodeToggle";
import {AnnotatedCode} from "@site/src/components/AnnotatedCode";
import {SemanticViewer} from "@site/src/components/SemanticViewer";

export default {
    ...MDXComponents,
    CodeToggle,
    AnnotatedCode,
    SemanticViewer,
};
