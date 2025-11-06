import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinSerialization)
}

// Build configuration output directory
val buildConfigOutputDir = layout.buildDirectory.dir("generated/source/buildConfig/commonMain/kotlin")

// Task to generate BuildConfig with current commit hash
val generateBuildConfig by tasks.registering {
    val outputFile = buildConfigOutputDir.get().file("com/defenderofegril/BuildConfig.kt")
    
    outputs.dir(buildConfigOutputDir)
    outputs.upToDateWhen { false } // Always regenerate to ensure latest commit hash
    
    doLast {
        val commitHash = try {
            val process = Runtime.getRuntime().exec("git rev-parse --short HEAD")
            // Read output before waiting to prevent potential deadlock
            val hash = process.inputStream.bufferedReader().use { it.readText().trim() }
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                logger.warn("git command exited with code $exitCode")
                "unknown"
            } else {
                hash
            }
        } catch (e: Exception) {
            logger.warn("Failed to get git commit hash: ${e.message}")
            "unknown"
        }
        
        val versionName = "1.0"
        
        val buildConfigContent = """
            |package com.defenderofegril
            |
            |/**
            | * Build configuration with version and commit information
            | * This file is auto-generated during build
            | */
            |object BuildConfig {
            |    const val VERSION_NAME = "$versionName"
            |    const val COMMIT_HASH = "$commitHash"
            |}
            |""".trimMargin()
        
        outputFile.asFile.apply {
            parentFile.mkdirs()
            writeText(buildConfigContent)
        }
        
        logger.info("Generated BuildConfig with commit hash: $commitHash")
    }
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    jvm("desktop")
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "defenderOfEgril"
        browser {
            commonWebpackConfig {
                outputFileName = "defenderOfEgril.js"
            }
        }
        binaries.executable()
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        val desktopMain by getting
        val wasmJsMain by getting
        
        // Add generated source directory to commonMain
        commonMain {
            kotlin.srcDir(buildConfigOutputDir)
        }
        
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
        wasmJsMain.dependencies {
        }
        
        iosMain.dependencies {
        }
    }
}

// Make all Kotlin compilation tasks depend on generateBuildConfig
tasks.withType<org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile<*>> {
    dependsOn(generateBuildConfig)
}

android {
    namespace = "com.defenderofegril"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.defenderofegril"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.defenderofegril.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.defenderofegril"
            packageVersion = "1.0.0"
        }
    }
}
