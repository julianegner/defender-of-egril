rootProject.name = "defender-of-egril"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

include(":composeApp")
project(":composeApp").projectDir = File("frontend/composeApp")

include(":assetPack")
project(":assetPack").projectDir = File("frontend/assetPack")

include(":png-encoder")
project(":png-encoder").projectDir = File("shared/png-encoder")

include(":server")
project(":server").projectDir = File("servers/backend")
