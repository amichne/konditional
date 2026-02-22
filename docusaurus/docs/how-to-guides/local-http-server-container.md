# How-to: run the local HTTP server container

Use this page to run a local snapshot service for deterministic integration
and failure testing.

## Read this page when

- You need a local endpoint for namespace snapshot CRUD operations.
- You want persistent snapshot storage across container restarts.
- You are validating refresh and rollback behavior before production.

## Prerequisites

- Docker Desktop or an equivalent Docker runtime.
- Repository root as your working directory.
- `curl` or the request files under `requests/`.

## Deterministic steps

1. Start the local stack.

```bash
docker compose -f docker-compose.http-server.yml up --build
```

2. Verify liveness.

```bash
curl http://localhost:8080/health
```

Expected result: `ok`.

3. Store and read one namespace snapshot.

```bash
curl -X PUT http://localhost:8080/v1/namespaces/app \
  -H 'content-type: application/json' \
  --data '{"flags":{"checkout":true}}'

curl http://localhost:8080/v1/namespaces/app
```

4. Verify deterministic namespace listing.

```bash
curl http://localhost:8080/v1/namespaces
```

5. Clean up or reset local state when needed.

```bash
docker compose -f docker-compose.http-server.yml down
# Optional hard reset
# docker volume rm konditional-http-storage
```

## Endpoint checklist

- [ ] `GET /health` returns `200` and body `ok`.
- [ ] `PUT /v1/namespaces/{namespace}` returns `204`.
- [ ] `GET /v1/namespaces/{namespace}` returns stored raw JSON.
- [ ] `DELETE /v1/namespaces/{namespace}` removes the snapshot.

## Next steps

- [Safe remote config](/how-to-guides/safe-remote-config)
- [Refresh patterns](/production-operations/refresh-patterns)
- [Handling failures](/how-to-guides/handling-failures)
