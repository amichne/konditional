# How-To: Run the Local HTTP Server Container

This guide explains the new local HTTP server resources that Konditional adds,
and shows how you can use them for local integration testing, snapshot fixture
management, and persistent development workflows.

## What this adds

This change adds a complete local integration surface that runs without cloud
infrastructure.

- `konditional-http-server/`: A Ktor-based HTTP server module that serves
  namespace snapshot operations.
- `docker/http-server/Dockerfile`: A container image build that packages the
  runnable `konditional-http-server` distribution.
- `docker-compose.http-server.yml`: A ready-to-run local stack that exposes
  port `8080` and mounts persistent storage.
- `requests/`: Editor-friendly `.http` request samples for quick interaction
  with the local containerized server.
- Persistent storage volume: A named Docker volume,
  `konditional-http-storage`, mounted at `/data`.

## What you can leverage now

You can now test runtime snapshot workflows against a local HTTP service that
persists state across restarts.

- Build local integration tests that `PUT` and `GET` raw namespace snapshot
  payloads.
- Simulate snapshot lifecycle operations (`put`, `read`, `delete`) before
  wiring a production config backend.
- Keep fake snapshot state between container restarts with the mounted Docker
  volume.
- Enumerate stored namespaces in deterministic key order via
  `GET /v1/namespaces`.

## Run the server with Docker Compose

Use this flow when you want a local endpoint quickly.

1. From the repository root, start the service:
   ```bash
   docker compose -f docker-compose.http-server.yml up --build
   ```
2. In another terminal, verify liveness:
   ```bash
   curl http://localhost:8080/health
   ```
3. Confirm that the service returns `ok`.

## Use the included sample requests

Konditional includes ready-to-run request files in `requests/` that target the
local Docker server on `http://localhost:8080`.

- `requests/01-health.http`
- `requests/02-namespace-crud.http`
- `requests/03-list-and-errors.http`

## Endpoint operations

The local server exposes a namespace-scoped snapshot API.

| Method | Path | Behavior |
| --- | --- | --- |
| `GET` | `/health` | Returns `200` with body `ok`. |
| `GET` | `/v1/namespaces` | Returns `200` and a JSON array of stored namespace keys. |
| `GET` | `/v1/namespaces/{namespace}` | Returns `200` with raw snapshot JSON, or `404` if missing. |
| `PUT` | `/v1/namespaces/{namespace}` | Stores raw snapshot JSON and returns `204`. |
| `DELETE` | `/v1/namespaces/{namespace}` | Deletes stored snapshot and returns `204`, or `404` if missing. |

## Example workflow

Use this sequence to write, read, and delete a namespace snapshot.

1. Store a snapshot payload:
   ```bash
   curl -X PUT http://localhost:8080/v1/namespaces/app \
     -H 'content-type: application/json' \
     --data '{"flags":{"checkout":true}}'
   ```
2. Read the stored payload:
   ```bash
   curl http://localhost:8080/v1/namespaces/app
   ```
3. List all stored namespaces:
   ```bash
   curl http://localhost:8080/v1/namespaces
   ```
4. Delete the namespace payload:
   ```bash
   curl -X DELETE http://localhost:8080/v1/namespaces/app
   ```

## Persistence behavior

The local container writes snapshots to `STORAGE_PATH=/data/snapshots.json`,
and Docker Compose mounts `/data` to the named volume
`konditional-http-storage`.

After you stop and restart the container, the previously written namespace
snapshots remain available until you remove the volume.

## Next steps

- Review [How-To: Load Configuration Safely from Remote](/how-to-guides/safe-remote-config)
  to connect loaded JSON to typed namespace parsing.
- Review [Runtime lifecycle](/runtime/lifecycle) for atomic load and rollback
  behavior inside namespaces.
