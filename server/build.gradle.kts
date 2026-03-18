plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

group = "de.egril.defender"
version = "1.0.0"

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
