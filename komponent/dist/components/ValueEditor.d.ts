import { FlagValue, BooleanFlagValue, StringFlagValue, IntFlagValue, DoubleFlagValue, EnumFlagValue, DataClassFlagValue, SchemaMetadata } from '../types/schema';

interface ValueEditorProps {
    value: FlagValue;
    onChange: (value: FlagValue) => void;
    schema?: SchemaMetadata;
    /** Optional: render in compact mode for inline display */
    compact?: boolean;
}
export declare function ValueEditor({ value, onChange, schema, compact, }: ValueEditorProps): JSX.Element;
interface BooleanEditorProps {
    value: BooleanFlagValue;
    onChange: (value: BooleanFlagValue) => void;
    compact?: boolean;
}
declare function BooleanEditor({ value, onChange, compact }: BooleanEditorProps): JSX.Element;
interface StringEditorProps {
    value: StringFlagValue;
    onChange: (value: StringFlagValue) => void;
    compact?: boolean;
}
declare function StringEditor({ value, onChange, compact }: StringEditorProps): JSX.Element;
interface IntEditorProps {
    value: IntFlagValue;
    onChange: (value: IntFlagValue) => void;
    compact?: boolean;
}
declare function IntEditor({ value, onChange, compact }: IntEditorProps): JSX.Element;
interface DoubleEditorProps {
    value: DoubleFlagValue;
    onChange: (value: DoubleFlagValue) => void;
    compact?: boolean;
}
declare function DoubleEditor({ value, onChange, compact }: DoubleEditorProps): JSX.Element;
interface EnumEditorProps {
    value: EnumFlagValue;
    onChange: (value: EnumFlagValue) => void;
    schema?: SchemaMetadata;
    compact?: boolean;
}
declare function EnumEditor({ value, onChange, schema, compact }: EnumEditorProps): JSX.Element;
interface DataClassEditorProps {
    value: DataClassFlagValue;
    onChange: (value: DataClassFlagValue) => void;
    schema?: SchemaMetadata;
    compact?: boolean;
}
declare function DataClassEditor({ value, onChange, schema, compact, }: DataClassEditorProps): JSX.Element;
export { BooleanEditor, StringEditor, IntEditor, DoubleEditor, EnumEditor, DataClassEditor };
