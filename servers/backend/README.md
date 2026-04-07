# Backend

Ktor-based backend server for Defender of Egril. Runs on **port 8080** by default.

## Run or Build

```bash
# Run backend directly (Gradle)
./gradlew :server:run

# Build the server distribution
./gradlew :server:build

# Start via Docker Compose (recommended – also starts PostgreSQL and Keycloak)
docker compose up -d --build backend
```

> **Port conflict**: The WASM/Web dev server (`./gradlew :composeApp:wasmJsBrowserDevelopmentRun`)
> also uses port 8080 by default. If both are needed simultaneously, configure one of them to use
> a different port (e.g. `SERVER_PORT=8090 ./gradlew :server:run`).
