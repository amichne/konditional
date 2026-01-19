import { Flag } from '../types/schema';
import { SnapshotDiff } from '../types/state';
import { ValidationResult } from '../types/validation';

interface FlagDiffInlineProps {
    before: Flag;
    after: Flag;
}
export declare function FlagDiffInline({ before, after }: FlagDiffInlineProps): JSX.Element;
interface SaveModalProps {
    diff: SnapshotDiff;
    validation: ValidationResult;
    isSaving: boolean;
    onConfirm: () => void;
    onCancel: () => void;
}
export declare function SaveModal({ diff, validation, isSaving, onConfirm, onCancel, }: SaveModalProps): JSX.Element;
export {};
