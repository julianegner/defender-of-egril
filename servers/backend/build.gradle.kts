plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

group = "de.egril.defender"
// App version - resolved in this order:
//   1. Gradle property:  -PappVersion=1.2.3
//   2. VERSION file at the project root (written by the release GitHub Action)
//   3. Hard-coded default "0.0.0"
version = project.findProperty("appVersion")?.toString()
    ?: rootProject.file("VERSION").takeIf { it.exists() }?.readText()?.trim()?.ifBlank { null }
    ?: "0.0.0"

application {
    mainClass.set("de.egril.defender.ApplicationKt")
}

dependencies {
    implementation(project(":png-encoder"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.cors)
    implementation(libs.logback)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.liquibase.core)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.postgresql)
}

// Exclude end-to-end tests from the regular test task so that the normal CI build
// does not require Docker / a running Keycloak container.
tasks.named<Test>("test") {
    filter {
        excludeTestsMatching("*EndToEnd*")
    }
}

/**
 * Runs the full end-to-end test suite against containerized Keycloak + PostgreSQL.
 *
 * Usage:
 *   ./gradlew :server:e2eTest
 *
 * Requirements:
 *   - Docker must be available on the host (Testcontainers pulls the images automatically)
 *   - Internet access to pull quay.io/keycloak/keycloak:24.0 (first run only)
 *
 * Keycloak startup takes ~60-90 s, so the task timeout is set generously.
 */
tasks.register<Test>("e2eTest") {
    description = "Runs end-to-end tests with containerised Keycloak and PostgreSQL via Testcontainers"
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter {
        includeTestsMatching("*EndToEnd*")
    }
    // Allow up to 10 minutes for Keycloak to start and all tests to complete
    jvmArgs("-Djunit.platform.execution.timeout=600s")
}
