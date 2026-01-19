import { Flag, FeatureId, FlagValueType } from '../types/schema';

interface FlagListProps {
    flags: Flag[];
}
export declare function FlagList({ flags }: FlagListProps): JSX.Element;
interface FlagCardProps {
    flagKey: FeatureId;
}
export declare const FlagCard: import('react').NamedExoticComponent<FlagCardProps>;
interface TypeBadgeProps {
    type: FlagValueType;
}
declare function TypeBadge({ type }: TypeBadgeProps): JSX.Element;
interface ToggleProps {
    checked: boolean;
    onChange: (checked: boolean) => void;
    label?: string;
}
declare function Toggle({ checked, onChange, label }: ToggleProps): JSX.Element;
interface AllowlistEditorProps {
    value: string[];
    onChange: (value: string[]) => void;
}
declare function AllowlistEditor({ value, onChange }: AllowlistEditorProps): JSX.Element;
export { Toggle, AllowlistEditor, TypeBadge };
