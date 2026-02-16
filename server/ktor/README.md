# Konditional Server Ktor

Ktor integration module for serving the Konditional REST OpenAPI specification.

## Usage

### Install into an existing Ktor `Application`

```kotlin
fun Application.module() {
    installKonditionalSpecRoute()
}
```

### Mount into an existing Ktor `Route`

```kotlin
routing {
    route("/internal") {
        konditionalSpecRoute(KonditionalSpecRouteConfig(path = "/spec/openapi.json"))
    }
}
```

### Run standalone server

```bash
./gradlew :server:ktor:run
# optional args: <port> <path>
./gradlew :server:ktor:run --args="8081 /spec.json"
```
