# Konditional Server Ktor

Ktor integration for serving the Konditional REST OpenAPI specification.

## Usage

### Install into an existing Ktor application

```kotlin
fun Application.module() {
    installKonditionalRestSpec()
}
```

### Mount as a route extension

```kotlin
routing {
    konditionalRestSpecRoute(
        KonditionalRestSpecRouteConfig(routePath = "/internal/spec/openapi.json"),
    )
}
```

### Run as a standalone server

```bash
./gradlew :server:ktor:run
```

Or launch main class:

```text
io.amichne.konditional.server.ktor.spec.KonditionalRestSpecStandaloneServer
```
