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

// Impressum flag - can be set via gradle.properties or command line: -PwithImpressum=true
val withImpressum: Boolean = project.findProperty("withImpressum")?.toString()?.toBoolean() ?: false

// Official editing flag - can be set via gradle.properties or command line: -Pofficial=true
val official: Boolean = project.findProperty("official")?.toString()?.toBoolean() ?: false

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

// Task to generate WithImpressum constant based on project property
val generateWithImpressumConstant by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/source/buildConfig/commonMain/kotlin").get().asFile
    outputs.dir(outputDir)
    
    doLast {
        val file = File(outputDir, "de/egril/defender/WithImpressum.kt")
        logger.info("Generating WithImpressum.kt with withImpressum: $withImpressum")
        logger.info("Output file: $file")
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package de.egril.defender
            |
            |/**
            | * Impressum configuration flag
            | * This file is auto-generated during build
            | * Set via gradle property: -PwithImpressum=true
            | */
            |object WithImpressum {
            |    const val withImpressum: Boolean = $withImpressum
            |}
            |""".trimMargin()
        )
    }
}

// Task to generate OfficialEditMode constant based on project property
val generateOfficialEditModeConstant by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/source/buildConfig/commonMain/kotlin").get().asFile
    outputs.dir(outputDir)
    outputs.upToDateWhen { false } // Always regenerate to ensure latest property value
    
    doLast {
        val file = File(outputDir, "de/egril/defender/OfficialEditMode.kt")
        logger.info("Generating OfficialEditMode.kt with official: $official")
        logger.info("Output file: $file")
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package de.egril.defender
            |
            |/**
            | * Official editing mode configuration flag
            | * This file is auto-generated during build
            | * Set via gradle property: -Pofficial=true
            | * 
            | * When enabled, allows editing of official game data (maps and levels) directly.
            | * A warning will be shown on game close if official data has been modified.
            | */
            |object OfficialEditMode {
            |    const val enabled: Boolean = $official
            |}
            |""".trimMargin()
        )
    }
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
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
        
        // Create androidUnitTest source set for Android-specific tests
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.test.junit)
                implementation(libs.junit)
                implementation(libs.mockk.android)
                implementation(libs.robolectric)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.flagkit)
            implementation(libs.multiplatform.settings)
            // Compottie for Lottie animations
            implementation("io.github.alexzhirkevich:compottie:2.0.2")
            implementation("io.github.alexzhirkevich:compottie-dot:2.0.2")
            implementation("io.github.alexzhirkevich:compottie-network:2.0.2")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.compose.ui.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.jlayer)
        }
        desktopTest.dependencies {
            implementation(libs.compose.ui.test.junit4)
            implementation(compose.desktop.currentOs)
        }
        wasmJsMain.dependencies {
        }
        
        iosMain.dependencies {
        }
    }
}

// Make all Kotlin compilation tasks depend on generateBuildConfig, generateWithImpressumConstant, and generateOfficialEditModeConstant
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>> {
    dependsOn(generateBuildConfig)
    dependsOn(generateWithImpressumConstant)
    dependsOn(generateOfficialEditModeConstant)
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
        
        // Configure test instrumentation runner
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    debugImplementation(libs.compose.ui.tooling)
}

// Task to generate map PNG images from map JSON files using the Kotlin MapImageGenerator
tasks.register<JavaExec>("generateMapImages") {
    group = "mapgen"
    description = "Generate PNG map images for all map JSON files in the repository"
    dependsOn("compileKotlinDesktop")
    classpath = files(
        kotlin.targets.named("desktop").map { target ->
            (target as org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget)
                .compilations["main"].output.classesDirs
        },
        configurations.named("desktopRuntimeClasspath")
    )
    mainClass.set("de.egril.defender.mapgen.GenerateMapImagesKt")
    workingDir = rootDir
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
