import React from "react";
import useBaseUrl from "@docusaurus/useBaseUrl";
import type { SemanticProfile } from "@site/src/types/semantic-profile";

export type SemanticProfileLoadState = {
  profile: SemanticProfile | null;
  status: "idle" | "loading" | "ready" | "error";
  error: string | null;
};

const profileCache = new Map<string, SemanticProfile>();

export function useSemanticProfile(
  snippetId: string | undefined,
  inlineProfile?: SemanticProfile
): SemanticProfileLoadState {
  const baseUrl = useBaseUrl("semantic-profiles/");

  const resolvedUrl = React.useMemo(() => {
    if (inlineProfile != null || snippetId == null) return null;
    return `${baseUrl}${encodeURIComponent(snippetId)}.json`;
  }, [snippetId, inlineProfile, baseUrl]);

  const [state, setState] = React.useState<SemanticProfileLoadState>(() => ({
    profile: inlineProfile ?? null,
    status: inlineProfile != null ? "ready" : "idle",
    error: null,
  }));

  React.useEffect(() => {
    if (inlineProfile != null) {
      setState({ profile: inlineProfile, status: "ready", error: null });
      return;
    }

    if (resolvedUrl == null) {
      setState({ profile: null, status: "idle", error: null });
      return;
    }

    const cached = profileCache.get(resolvedUrl);
    if (cached != null) {
      setState({ profile: cached, status: "ready", error: null });
      return;
    }

    let cancelled = false;
    setState((prev) => ({ ...prev, status: "loading", error: null }));

    fetch(resolvedUrl)
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then((data: SemanticProfile) => {
        if (cancelled) return;
        profileCache.set(resolvedUrl, data);
        setState({ profile: data, status: "ready", error: null });
      })
      .catch((err) => {
        if (cancelled) return;
        setState({ profile: null, status: "error", error: String(err) });
      });

    return () => {
      cancelled = true;
    };
  }, [inlineProfile, resolvedUrl]);

  return state;
}
