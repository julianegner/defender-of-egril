# Multi-stage build for the Defender of Egril backend server.
# Stage 1: build the application distribution using Gradle.
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

# Copy Gradle wrapper and build files first (layer cache for dependencies)
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY servers/backend/build.gradle.kts servers/backend/build.gradle.kts
COPY shared/png-encoder/ shared/png-encoder/

RUN chmod +x gradlew

# settings.gradle.kts includes :composeApp, :assetPack, :png-encoder, and :server.
# The composeApp and assetPack modules are not needed for the server build, but Gradle
# requires all declared project directories to exist. Create minimal stubs so
# configuration succeeds without copying any unrelated source code.
RUN mkdir -p frontend/composeApp && echo "" > frontend/composeApp/build.gradle.kts \
 && mkdir -p frontend/assetPack    && echo "" > frontend/assetPack/build.gradle.kts

# Download dependencies (cached unless build files change)
RUN ./gradlew :server:dependencies --no-daemon -q || true

# Copy all source code and build the server distribution
COPY servers/backend/ servers/backend/
RUN ./gradlew :server:installDist --no-daemon -x test

# Stage 2: lean runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /workspace/servers/backend/build/install/server/ ./

EXPOSE 8080

CMD ["./bin/server"]
