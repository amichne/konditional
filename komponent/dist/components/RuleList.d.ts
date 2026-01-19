import { Rule, FlagValueType, FeatureId } from '../types/schema';

interface RuleListProps {
    flagKey: FeatureId;
    rules: Rule[];
    flagType: FlagValueType;
}
export declare function RuleList({ flagKey, rules, flagType }: RuleListProps): JSX.Element;
interface RuleEditorProps {
    flagKey: FeatureId;
    ruleIndex: number;
    flagType: FlagValueType;
    specificity: number;
    evaluationOrder: number;
}
export declare const RuleEditor: import('react').NamedExoticComponent<RuleEditorProps>;
export {};
