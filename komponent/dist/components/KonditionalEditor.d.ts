import { Snapshot, Flag } from '../types/schema';

export interface KonditionalEditorProps {
    /** Initial snapshot to render and edit */
    snapshot: Snapshot;
    /** Called when user confirms save. Receives the modified snapshot. */
    onSave: (snapshot: Snapshot) => void | Promise<void>;
    /** Optional: called on every change for live preview or external validation */
    onChange?: (snapshot: Snapshot) => void;
    /** Optional: filter which flags are displayed/editable */
    filter?: (flag: Flag) => boolean;
    /** Optional: theme preference */
    theme?: 'light' | 'dark' | 'system';
    /** Optional: custom class name for the root element */
    className?: string;
}
export declare function KonditionalEditor({ snapshot, onSave, onChange, filter, theme, className, }: KonditionalEditorProps): JSX.Element;
export default KonditionalEditor;
