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
