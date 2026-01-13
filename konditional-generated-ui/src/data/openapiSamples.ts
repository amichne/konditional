import { SerializableSnapshot } from '@/types/konditional';
import openApiYaml from '../../../docusaurus/openapi/openapi.yaml?raw';

const sampleParentKey = 'x-konditional-samples';
const sampleChildKey = 'snapshotJson';
const storageKey = 'konditional.schemaForms.snapshot';

export interface OpenApiSampleResult {
  snapshot: SerializableSnapshot;
  error?: string;
}

const openApiSampleResult = parseOpenApiSampleSnapshot(openApiYaml);

export function loadSchemaFormsSnapshot(): OpenApiSampleResult {
  if (typeof window === 'undefined') {
    return openApiSampleResult;
  }

  const stored = window.localStorage.getItem(storageKey);
  if (!stored) {
    return openApiSampleResult;
  }

  try {
    const parsed = JSON.parse(stored) as SerializableSnapshot;
    return { snapshot: parsed, error: openApiSampleResult.error };
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    const combinedError = openApiSampleResult.error
      ? `${openApiSampleResult.error} (Storage read failed: ${message})`
      : `Stored snapshot is invalid: ${message}`;
    return { snapshot: openApiSampleResult.snapshot, error: combinedError };
  }
}

export function saveSchemaFormsSnapshot(snapshot: SerializableSnapshot): string | null {
  if (typeof window === 'undefined') {
    return 'Snapshot storage is only available in the browser.';
  }

  try {
    window.localStorage.setItem(storageKey, JSON.stringify(snapshot));
    return null;
  } catch (error) {
    return error instanceof Error ? error.message : 'Failed to save snapshot.';
  }
}

function parseOpenApiSampleSnapshot(raw: string): OpenApiSampleResult {
  const sampleJson = extractYamlBlock(raw, sampleParentKey, sampleChildKey);
  if (!sampleJson) {
    return {
      snapshot: { meta: { version: 'openapi-sample', generatedAtEpochMillis: 0, source: 'openapi' }, flags: [] },
      error: 'OpenAPI sample snapshot missing in docusaurus/openapi/openapi.yaml.',
    };
  }

  try {
    const parsed = JSON.parse(sampleJson) as SerializableSnapshot;
    if (!parsed.flags || !Array.isArray(parsed.flags)) {
      return {
        snapshot: { meta: parsed.meta, flags: [] },
        error: 'OpenAPI sample snapshot is missing the flags array.',
      };
    }
    return { snapshot: parsed };
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return {
      snapshot: { meta: { version: 'openapi-sample', generatedAtEpochMillis: 0, source: 'openapi' }, flags: [] },
      error: `OpenAPI sample snapshot could not be parsed: ${message}`,
    };
  }
}

function extractYamlBlock(raw: string, parentKey: string, childKey: string): string | null {
  const lines = raw.split(/\r?\n/);
  const parentIndex = lines.findIndex((line) => line.trim() === `${parentKey}:`);
  if (parentIndex === -1) {
    return null;
  }

  const parentIndent = leadingWhitespace(lines[parentIndex]);

  for (let index = parentIndex + 1; index < lines.length; index += 1) {
    const line = lines[index];
    const indent = leadingWhitespace(line);
    if (indent <= parentIndent) {
      break;
    }

    const childMatch = line.match(new RegExp(`^(\\s*)${childKey}:\\s*\\|\\s*$`));
    if (!childMatch) {
      continue;
    }

    const blockIndent = childMatch[1].length;
    const blockLines: string[] = [];
    let contentIndent: number | null = null;

    for (let nextIndex = index + 1; nextIndex < lines.length; nextIndex += 1) {
      const blockLine = lines[nextIndex];
      const blockLineIndent = leadingWhitespace(blockLine);
      if (blockLineIndent <= blockIndent) {
        break;
      }

      if (blockLine.trim().length === 0) {
        if (contentIndent !== null) {
          blockLines.push('');
        }
        continue;
      }

      if (contentIndent === null) {
        contentIndent = blockLineIndent;
      }

      if (blockLineIndent < contentIndent) {
        break;
      }

      blockLines.push(blockLine.slice(contentIndent));
    }

    return blockLines.join('\n').trim();
  }

  return null;
}

function leadingWhitespace(value: string): number {
  const match = value.match(/^\s*/);
  return match ? match[0].length : 0;
}
