# Kotlin JVM Command Playbook

## Environment Checks

```bash
java -version
./gradlew -v
./gradlew tasks --all
```

## Build and Test Baseline

```bash
./gradlew clean test
./gradlew build
```

## Run Application

```bash
./gradlew run
```

If `run` does not exist, inspect tasks and use the project-specific `JavaExec` task.

## Debug Entry Points

```bash
./gradlew run --debug-jvm
./gradlew test --debug-jvm
```

Default JDWP attach target:
- host: `127.0.0.1`
- port: `5005`

## VS Code Attach Example (`launch.json`)

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Attach Kotlin JVM (5005)",
      "request": "attach",
      "hostName": "127.0.0.1",
      "port": 5005
    }
  ]
}
```

## Recovery Sequence

```bash
./gradlew --stop
./gradlew clean build --refresh-dependencies
```

If failure remains, capture:
- failing command,
- full stack trace,
- affected module,
- and minimal reproduction steps.
