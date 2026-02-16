# Local HTTP server request samples

This directory contains sample HTTP requests for the local Docker-based
Konditional HTTP server instance.

These requests target `http://localhost:8080`, which matches the default
mapping in `docker-compose.http-server.yml`.

## Prerequisite

Start the local server first:

```bash
docker compose -f docker-compose.http-server.yml up --build
```

## Files

- `01-health.http`: Liveness request.
- `02-namespace-crud.http`: Create, read, and delete a namespace payload.
- `03-list-and-errors.http`: List namespaces and exercise common error paths.
