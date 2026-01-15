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
    alias(libs.plugins.localization)
}

// Build configuration output directory
val buildConfigOutputDir = layout.buildDirectory.dir("generated/source/buildConfig/commonMain/kotlin")

// Task to generate BuildConfig with current commit hash
val generateBuildConfig by tasks.registering {
    val outputFile = buildConfigOutputDir.get().file("de/egril/defender/BuildConfig.kt")
    
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
        
        val commitDate = try {
            val process = Runtime.getRuntime().exec("git show -s --format=%ci HEAD")
            val date = process.inputStream.bufferedReader().use { it.readText().trim() }
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                logger.warn("git command exited with code $exitCode")
                "unknown"
            } else {
                date
            }
        } catch (e: Exception) {
            logger.warn("Failed to get git commit date: ${e.message}")
            "unknown"
        }
        
        val commitMessage = try {
            val process = Runtime.getRuntime().exec(arrayOf("git", "log", "-1", "--pretty=%B"))
            val message = process.inputStream.bufferedReader().use { it.readText().trim() }
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                logger.warn("git command exited with code $exitCode")
                "unknown"
            } else {
                // Escape quotes and newlines for Kotlin string
                message.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
            }
        } catch (e: Exception) {
            logger.warn("Failed to get git commit message: ${e.message}")
            "unknown"
        }
        
        val versionName = "1.0"
        
        val buildConfigContent = """
            |package de.egril.defender
            |
            |/**
            | * Build configuration with version and commit information
            | * This file is auto-generated during build
            | */
            |object BuildConfig {
            |    const val VERSION_NAME = "$versionName"
            |    const val COMMIT_HASH = "$commitHash"
            |    const val COMMIT_DATE = "$commitDate"
            |    const val COMMIT_MESSAGE = "$commitMessage"
            |}
            |""".trimMargin()
        
        outputFile.asFile.apply {
            parentFile.mkdirs()
            writeText(buildConfigContent)
        }
        
        logger.info("Generated BuildConfig with commit hash: $commitHash, date: $commitDate")
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
            baseName = "DefenderOfEgril"
            isStatic = true
        }
    }
    
    sourceSets {
        val desktopMain by getting
        val desktopTest by getting
        val wasmJsMain by getting
        val androidMain by getting
        
        // Create iosMain source set for iOS targets
        val iosMain by creating {
            dependsOn(commonMain.get())
        }
        
        // Connect each iOS target's main compilation to iosMain
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        
        iosX64Main.dependsOn(iosMain)
        iosArm64Main.dependsOn(iosMain)
        iosSimulatorArm64Main.dependsOn(iosMain)
        
        // Add generated source directory to commonMain
        commonMain {
            kotlin.srcDir(buildConfigOutputDir)
            kotlin.srcDirs(
                File(
                    layout.buildDirectory.get().asFile.path,
                    "generated/compose/resourceGenerator/kotlin/commonCustomResClass"
                )
            )
        }
        
        // Create jvmMain as intermediate source set shared by Android and Desktop
        val jvmMain by creating {
            dependsOn(commonMain.get())
        }
        
        // Configure androidMain to depend on jvmMain
        androidMain.apply {
            dependsOn(jvmMain)
        }
        
        // Configure desktopMain to depend on jvmMain
        desktopMain.apply {
            dependsOn(jvmMain)
        }
        
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.flagkit)
            implementation(libs.multiplatform.settings)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.jlayer)
        }
        desktopTest.dependencies {
            implementation(compose.desktop.uiTestJUnit4)
            implementation(compose.desktop.currentOs)
        }
        wasmJsMain.dependencies {
        }
        
        iosMain.dependencies {
        }
    }
}

// Make all Kotlin compilation tasks depend on generateBuildConfig
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>> {
    dependsOn(generateBuildConfig)
}

android {
    namespace = "de.egril.defender"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "de.egril.defender"
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
    
    // Configure output file naming
    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "de.egril.defender-${name}.apk"
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "de.egril.defender.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageVersion = "1.0.0"
            
            macOS {
                bundleID = "de.egril.defender"
                packageName = "defender-of-egril"
            }
            
            windows {
                menuGroup = "Defender of Egril"
                packageName = "DefenderOfEgril"
                perUserInstall = true
                dirChooser = true
                upgradeUuid = "D5F5E5C5-B5A5-95A5-85A5-75A565A555A5"
            }
            
            linux {
                packageName = "defender-of-egril"
            }
        }
    }
}
