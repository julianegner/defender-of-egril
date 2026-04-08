import java.util.Properties
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

// App version - resolved in this order:
//   1. Gradle property:  -PappVersion=1.2.3
//   2. VERSION file at the project root (written by the release GitHub Action)
//   3. Hard-coded default "0.0.0"
// Used for Android versionName, desktop packageVersion, and AppBuildInfo.VERSION_NAME
val appVersion: String = project.findProperty("appVersion")?.toString()
    ?: rootProject.file("VERSION").takeIf { it.exists() }?.readText()?.trim()?.ifBlank { null }
    ?: "0.0.0"

// Derive Android versionCode from version string (major * 10000 + minor * 100 + patch).
// Constraints: minor and patch must be 0–99; major must be 0–21474.
// These limits are validated by the release.yml workflow before passing the version here.
val appVersionCode: Int = run {
    val parts = appVersion.split(".").map { it.toIntOrNull() ?: 0 }
    val major = parts.getOrElse(0) { 0 }
    val minor = parts.getOrElse(1) { 0 }
    val patch = parts.getOrElse(2) { 0 }
    major * 10000 + minor * 100 + patch
}

// macOS DMG requires MAJOR >= 1. When the app version has MAJOR=0 (e.g. "0.8.0"),
// override the macOS-specific package version by bumping MAJOR to 1 so that only the
// DMG format validation is satisfied without affecting other platform builds.
val macOsPackageVersion: String = run {
    val parts = appVersion.split(".").map { it.toIntOrNull() ?: 0 }
    val major = parts.getOrElse(0) { 0 }
    if (major == 0) {
        "1.${parts.getOrElse(1) { 0 }}.${parts.getOrElse(2) { 0 }}"
    } else {
        appVersion
    }
}

// Task to generate BuildConfig with current commit hash
val generateBuildConfig by tasks.registering {
    val outputFile = buildConfigOutputDir.get().file("de/egril/defender/AppBuildInfo.kt")
    
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
        
        val versionName = appVersion
        
        val buildConfigContent = """
            |package de.egril.defender
            |
            |/**
            | * Application build information with version and commit details.
            | * This file is auto-generated during build.
            | * Named AppBuildInfo (not BuildConfig) to avoid clashing with the
            | * Android-generated BuildConfig that holds flavor-specific URLs.
            | */
            |object AppBuildInfo {
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
        
        logger.info("Generated AppBuildInfo with commit hash: $commitHash, date: $commitDate")
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

// ---------------------------------------------------------------------------
// Profile helpers – defined early so they can be used in the android {} block
// ---------------------------------------------------------------------------

/**
 * Loads properties from a named profile file.
 *
 * Profile files live in the `frontend/profiles/` directory at the repository root.
 * Each non-comment line follows the `key=value` format.
 *
 * Available profiles:
 *   - `local`      – local Docker Compose stack (localhost:8081 / localhost:8080)
 *   - `production` – production server (configure URLs in frontend/profiles/production.properties)
 *   - `remote`     – alias for production (backward compatibility)
 *
 * @param profileName the name of the profile (e.g. "local" or "production")
 * @return the loaded [Properties], or an empty [Properties] if the file is missing
 */
fun loadProfileProperties(profileName: String): Properties {
    val profileFile = rootProject.file("frontend/profiles/$profileName.properties")
    if (!profileFile.exists()) {
        logger.warn(
            "Profile file not found: frontend/profiles/$profileName.properties. " +
                "Available profiles: local, production. " +
                "To create a custom profile, add a properties file in the frontend/profiles/ directory."
        )
        return Properties()
    }
    val props = Properties()
    profileFile.reader().use { props.load(it) }
    return props
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
            implementation(libs.oidc.appsupport)
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
            implementation("io.github.alexzhirkevich:compottie:2.1.0")
            implementation("io.github.alexzhirkevich:compottie-dot:2.1.0")
            implementation("io.github.alexzhirkevich:compottie-network:2.1.0")
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
            implementation(libs.oidc.appsupport)
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
        versionCode = appVersionCode
        versionName = appVersion
        
        // Redirect scheme for OIDC (kotlin-multiplatform-oidc library)
        addManifestPlaceholders(mapOf("oidcRedirectScheme" to "egril"))
        
        // Configure test instrumentation runner
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Enable BuildConfig generation so flavors can inject URLs
    buildFeatures {
        buildConfig = true
    }

    // Product flavors bake the Keycloak and backend URLs into the APK at build
    // time, since Android apps cannot read JVM system properties at runtime.
    //
    //   local      – points at the local Docker Compose stack (localhost URLs)
    //   production – points at the production server (configure frontend/profiles/production.properties)
    //
    // Generated tasks (install on connected device):
    //   installLocalDebug        → ./gradlew :composeApp:installLocal
    //   installProductionDebug   → ./gradlew :composeApp:installDebug / installProduction
    flavorDimensions += "env"
    productFlavors {
        val localProps = loadProfileProperties("local")
        val productionProps = loadProfileProperties("production")

        create("local") {
            dimension = "env"
            buildConfigField(
                "String", "IAM_BASE_URL",
                "\"${localProps.getProperty("iam.base.url") ?: "http://localhost:8081"}\""
            )
            buildConfigField(
                "String", "BACKEND_URL",
                "\"${localProps.getProperty("defender.backend.url") ?: "http://localhost:8080"}\""
            )
        }
        create("production") {
            dimension = "env"
            buildConfigField(
                "String", "IAM_BASE_URL",
                "\"${productionProps.getProperty("iam.base.url") ?: "https://keycloak.your-server.com"}\""
            )
            buildConfigField(
                "String", "BACKEND_URL",
                "\"${productionProps.getProperty("defender.backend.url") ?: "https://backend.your-server.com"}\""
            )
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

// Workaround for Gradle 9.x: Compose resource tasks declare output files that may not exist
// yet on a fresh build/clean, causing "Cannot access output property" errors. Marking them
// as untracked forces them to always run (still fast) and avoids the spurious failure.
tasks.matching { it.name.startsWith("copyNonXmlValueResourcesFor") }.configureEach {
    doNotTrackState("Gradle 9.x output-property validation workaround for Compose resource tasks")
}

compose.desktop {
    application {
        mainClass = "de.egril.defender.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageVersion = appVersion
            
            macOS {
                bundleID = "de.egril.defender"
                packageName = "defender-of-egril"
                // DMG requires MAJOR >= 1; use the adjusted version that satisfies this
                // constraint while keeping the real version on all other platforms.
                packageVersion = macOsPackageVersion
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

/**
 * Converts a profile's properties into JVM `-D` arguments suitable for
 * passing to a [JavaExec] task.
 *
 * @param profileName the name of the profile (e.g. "local" or "production")
 * @return a list of JVM args like `["-Diam.base.url=http://localhost:8081", …]`
 */
fun profileJvmArgs(profileName: String): List<String> {
    val props = loadProfileProperties(profileName)
    return props.stringPropertyNames().map { key -> "-D$key=${props.getProperty(key)}" }
}

// ---------------------------------------------------------------------------
// Profile-aware frontend task configuration
//
// The DEFAULT profile is "production". Pass -Pprofile=local to use the local
// Docker Compose stack instead.
//
// Desktop (JVM):
//   ./gradlew :composeApp:run -Pprofile=local
//   ./gradlew :composeApp:runLocal
//   ./gradlew :composeApp:runProduction   (default)
//
// Web/WASM:
//   ./gradlew :composeApp:wasmJsBrowserDevelopmentRun -Pprofile=local
//
// Android:
//   ./gradlew :composeApp:installLocal         (local flavor, debug build)
//   ./gradlew :composeApp:installDebug         (production flavor, debug build – default)
//   ./gradlew :composeApp:installProduction    (production flavor, debug build)
// ---------------------------------------------------------------------------

afterEvaluate {
    val profile = project.findProperty("profile")?.toString() ?: "production"

    // ── Desktop: configure the built-in `run` task ──────────────────────────
    tasks.named<JavaExec>("run") {
        jvmArgs(profileJvmArgs(profile))
        logger.lifecycle("Desktop 'run' task configured with profile '$profile'")
    }

    // ── Web/WASM: temporarily substitute profile URLs for the dev server ─────
    // The Kotlin/WASM webpack dev server serves resources from
    // src/wasmJsMain/resources/. We modify index.html and dev-server-proxy.js
    // in doFirst and restore them in doLast so source files are not permanently
    // changed.
    tasks.matching { it.name == "wasmJsBrowserDevelopmentRun" }.configureEach {
        val profileProps = loadProfileProperties(profile)
        val iamUrl = profileProps.getProperty("iam.base.url")
        val backendUrl = profileProps.getProperty("defender.backend.url")
        val indexHtml = project.file("src/wasmJsMain/resources/index.html")
        val proxyJs = project.file("webpack.config.d/dev-server-proxy.js")
        val backupDir = project.layout.buildDirectory.get().asFile.resolve("tmp/profile-backup")

        doFirst {
            backupDir.mkdirs()

            // Replace the Keycloak URL in the window.keycloakConfig default block.
            // The block in index.html always looks like:
            //   window.keycloakConfig = window.keycloakConfig || {
            //       url: 'http://...',
            //   };
            // We target only the url value between single quotes on that specific line.
            if (iamUrl != null) {
                val original = indexHtml.readText()
                backupDir.resolve("index.html").writeText(original)
                val urlLinePattern = Regex("""(window\.keycloakConfig\s*=\s*window\.keycloakConfig\s*\|\|\s*\{[^}]*\burl:\s*')[^']*""")
                val modified = urlLinePattern.find(original)?.let { match ->
                    val replacement = "${match.groupValues[1]}$iamUrl"
                    original.substring(0, match.range.first) + replacement + original.substring(match.range.last + 1)
                } ?: original
                indexHtml.writeText(modified)
                logger.lifecycle("WASM dev server configured with IAM URL: $iamUrl (profile: $profile)")
            }

            // Replace the backend proxy target URL in dev-server-proxy.js.
            // The relevant line always looks like:
            //   target: "http://localhost:8080",
            // We target only the URL value between double quotes after "target:".
            if (backendUrl != null) {
                val originalProxy = proxyJs.readText()
                backupDir.resolve("dev-server-proxy.js").writeText(originalProxy)
                val proxyTargetPattern = Regex("""(target:\s*")[^"]*""")
                val modifiedProxy = proxyTargetPattern.find(originalProxy)?.let { match ->
                    val replacement = "${match.groupValues[1]}$backendUrl"
                    originalProxy.substring(0, match.range.first) + replacement + originalProxy.substring(match.range.last + 1)
                } ?: originalProxy
                proxyJs.writeText(modifiedProxy)
                logger.lifecycle("WASM proxy configured with backend URL: $backendUrl (profile: $profile)")
            }
        }

        doLast {
            val indexHtmlBackup = backupDir.resolve("index.html")
            if (indexHtmlBackup.exists()) {
                indexHtml.writeText(indexHtmlBackup.readText())
                indexHtmlBackup.delete()
                logger.lifecycle("Restored index.html after WASM dev run")
            }
            val proxyJsBackup = backupDir.resolve("dev-server-proxy.js")
            if (proxyJsBackup.exists()) {
                proxyJs.writeText(proxyJsBackup.readText())
                proxyJsBackup.delete()
                logger.lifecycle("Restored dev-server-proxy.js after WASM dev run")
            }
        }
    }

    // ── Android: create convenience alias tasks ──────────────────────────────
    // Product flavors (local / production) are declared in the android {} block
    // below.  When flavors exist, the generic installDebug task is no longer
    // generated; it is replaced by installLocalDebug / installProductionDebug.
    // We register new tasks under the familiar names so existing workflows
    // continue to work and the default (installDebug → production) is clear.
    tasks.register("installDebug") {
        group = "install"
        description = "Installs the Production Debug build on a connected device (default profile). " +
            "Use installLocal for the local-stack flavor."
        dependsOn("installProductionDebug")
    }

    tasks.register("installProduction") {
        group = "install"
        description = "Installs the Production Debug build on a connected device."
        dependsOn("installProductionDebug")
    }

    tasks.register("installLocal") {
        group = "install"
        description = "Installs the Local Debug build on a connected device " +
            "(connects to the local Docker Compose stack)."
        dependsOn("installLocalDebug")
    }
}

// ── Desktop convenience tasks (profile baked in) ────────────────────────────
// Equivalent to: ./gradlew run -Pprofile=<profileName>
listOf("local", "production", "remote").forEach { profileName ->
    tasks.register<JavaExec>("run${profileName.replaceFirstChar { it.uppercase() }}") {
        group = "application"
        description = "Runs the desktop application with the '$profileName' profile. " +
            "Equivalent to: ./gradlew :composeApp:run -Pprofile=$profileName"
        dependsOn("compileKotlinDesktop")
        classpath = files(
            kotlin.targets.named("desktop").map { target ->
                (target as org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget)
                    .compilations["main"].output.classesDirs
            },
            configurations.named("desktopRuntimeClasspath")
        )
        mainClass.set("de.egril.defender.MainKt")
        jvmArgs(profileJvmArgs(profileName))
    }
}
