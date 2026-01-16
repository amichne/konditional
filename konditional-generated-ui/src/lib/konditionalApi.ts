import { SerializablePatch, SerializableSnapshot } from '@/types/konditional';

const apiBase = import.meta.env.VITE_KONDITIONAL_API_BASE ?? '/ui/api';

export async function fetchSnapshot(): Promise<SerializableSnapshot> {
  const response = await fetch(`${apiBase}/snapshot`, {
    headers: { Accept: 'application/json' },
  });

  if (!response.ok) {
    throw new Error(`Snapshot fetch failed: ${response.status}`);
  }

  return response.json() as Promise<SerializableSnapshot>;
}

export async function applySnapshotPatch(
  patch: SerializablePatch,
): Promise<SerializableSnapshot> {
  const response = await fetch(`${apiBase}/patch`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(patch),
  });

  if (!response.ok) {
    throw new Error(`Snapshot patch failed: ${response.status}`);
  }

  return response.json() as Promise<SerializableSnapshot>;
}
