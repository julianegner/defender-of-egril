import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.localization)
}

val requestedTasks = gradle.startParameter.taskNames
val configureAndroid =
    requestedTasks.isEmpty() ||
        requestedTasks.any { requestedTask ->
            !requestedTask.contains("wasm", ignoreCase = true) &&
                requestedTask != "clean" &&
                !requestedTask.endsWith(":clean")
        }

if (configureAndroid) {
    apply(plugin = "com.android.application")
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
val generateBuildConfig = tasks.register("generateBuildConfig") {
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
                // Escape quotes, newlines, and dollar signs for Kotlin string literal
                message.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\$", "\\\$")
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
val generateWithImpressumConstant = tasks.register("generateWithImpressumConstant") {
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
val generateOfficialEditModeConstant = tasks.register("generateOfficialEditModeConstant") {
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
    if (configureAndroid) {
        androidTarget {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_11)
            }
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
        iosArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "DefenderOfEgril"
            isStatic = true
        }
    }
    
    sourceSets {
        val desktopMain = getByName("desktopMain")
        val desktopTest = getByName("desktopTest")
        
        // Create iosMain source set for iOS targets
        val iosMain = create("iosMain") {
            dependsOn(commonMain.get())
        }
        
        // Connect each iOS target's main compilation to iosMain
        val iosArm64Main = getByName("iosArm64Main")
        
        iosArm64Main.dependsOn(iosMain)
        
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
        val jvmMain = create("jvmMain") {
            dependsOn(commonMain.get())
        }
        
        // Configure desktopMain to depend on jvmMain
        desktopMain.dependsOn(jvmMain)

        if (configureAndroid) {
            val androidMain = getByName("androidMain")

            // Configure androidMain to depend on jvmMain
            androidMain.dependsOn(jvmMain)

            // Create androidUnitTest source set for Android-specific tests
            getByName("androidUnitTest").dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.test.junit)
                implementation(libs.junit)
                implementation(libs.mockk.android)
                implementation(libs.robolectric)
                implementation(libs.kotlinx.coroutines.test)
            }

            androidMain.dependencies {
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.oidc.appsupport)
            }
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.symbols)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.flagkit)
            implementation(libs.multiplatform.settings)
            // Compottie for Lottie animations
            implementation(libs.compottie)
            implementation(libs.compottie.dot)
            implementation(libs.compottie.network)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.compose.ui.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.jlayer)
            implementation(project(":png-encoder"))
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

// The compose-multiplatform-localize plugin's GenerateTranslationsTask only declares resourcesDir and
// projectDir as @Input path strings, never the actual XML file contents. Consequently the task stays
// UP-TO-DATE when a strings.xml file changes and regenerates a stale LocalizedStrings map that is missing
// newly added keys, which then render as "???" at runtime even though compilation succeeds (the JetBrains
// Res accessors are regenerated independently). Declare the localization string resource files as task
// inputs so any edit correctly invalidates the task and triggers regeneration.
tasks.named("generateTranslateFile").configure {
    inputs.files(
        fileTree(layout.projectDirectory.dir("src/commonMain/composeResources")) {
            include("values*/*.xml")
        },
    )
        .withPropertyName("localizationStringResources")
        .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE)
}


if (configureAndroid) {
    extensions.configure<ApplicationExtension> {
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

        // ---------------------------------------------------------------------------
        // Android App Bundle (AAB) asset-pack configuration
        //
        // The `:assetPack` module is an install-time asset pack that holds the
        // Compose Multiplatform resources (~220 MB).  Including it in the bundle
        // moves those assets out of the base module, which would otherwise exceed
        // Google Play's 200 MB base-module limit.
        //
        // NOTE: This only affects `bundle*` (AAB) builds.  Plain `assemble*` APK
        //       builds remain self-contained so sideloading still works.
        // ---------------------------------------------------------------------------
        bundle {
            assetPacks += listOf(":assetPack")
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
                    "\"${productionProps.getProperty("iam.base.url") ?: "https://sso.julianegner.de"}\""
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
    }

    extensions.configure<ApplicationAndroidComponentsExtension> {
        onVariants(selector().all()) { variant ->
            variant.outputs.forEach { output ->
                output.outputFileName.set("de.egril.defender-${variant.name}.apk")
            }

            val variantNameCapped = variant.name.replaceFirstChar { it.uppercase() }
            val mergeAssetsTask = tasks.named("merge${variantNameCapped}Assets")
            tasks.matching { it.name == "build${variantNameCapped}PreBundle" }.configureEach {
                doFirst {
                    mergeAssetsTask.get().outputs.files.files.forEach { mergeOutputDir ->
                        // Remove the app's own CMP resources (served from the asset pack).
                        val appComposeResDir = File(
                            mergeOutputDir,
                            "composeResources/defender_of_egril.composeapp.generated.resources"
                        )
                        if (appComposeResDir.exists()) {
                            appComposeResDir.deleteRecursively()
                            logger.lifecycle(
                                "Asset-pack split: removed app composeResources package from base AAB " +
                                    "module (app resources are served from the install-time asset pack)"
                            )
                        }

                        // Remove the compose-material-symbols library CMP resources from the base
                        // module; they are now served from the install-time asset pack alongside
                        // the app's resources, ensuring they are reachable via AssetManager in
                        // AAB installs.
                        val libComposeResDir = File(
                            mergeOutputDir,
                            "composeResources/$materialSymbolsCmpPackage"
                        )
                        if (libComposeResDir.exists()) {
                            libComposeResDir.deleteRecursively()
                            logger.lifecycle(
                                "Asset-pack split: removed compose-material-symbols fonts from base " +
                                    "AAB module (fonts are served from the install-time asset pack)"
                            )
                        }
                    }
                }
            }
        }
    }
}

if (configureAndroid) {
    dependencies {
        add("debugImplementation", libs.compose.ui.tooling)
    }
}

// Shared path to the repository maps directory used by mapgen tasks
val repositoryMapsDir = layout.projectDirectory.dir("src/commonMain/composeResources/files/repository/maps").asFile.absolutePath
// Debug overlay output — outside composeResources so images are tracked in git but not compiled into the app
val mapDebugImagesDir = layout.projectDirectory.dir("map-debug-images").asFile.absolutePath

val sanitizeWasmImportObjects = tasks.register("sanitizeWasmImportObjects") {
    doLast {
        val nodeInteropBlock =
            Regex(
                """'kotlinx\.io\.node\.persistModule'\s*:\s*.*?'kotlinx\.io\.node\.requireModule'\s*:\s*\(require, mod\)\s*=>\s*\{.*?\}""",
                setOf(RegexOption.DOT_MATCHES_ALL),
            )
        fileTree(layout.buildDirectory.dir("compileSync/wasmJs")) {
            include("**/*.import-object.mjs")
        }.files.forEach { importObjectFile ->
            val original = importObjectFile.readText()
            val sanitized =
                original.replace(
                    nodeInteropBlock,
                    """
                    'kotlinx.io.node.persistModule' :
                        (() => {})
                    ,
                    'kotlinx.io.node.getRequire' : () => null
                    ,
                    'kotlinx.io.node.requireModule' :
                        (_require, _mod) => null
                    """.trimIndent(),
                )
            if (sanitized != original) {
                importObjectFile.writeText(sanitized)
            }
        }
    }
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
    args = listOf(repositoryMapsDir)
    // After regenerating map images, also regenerate the hex-grid debug overlays
    finalizedBy("generateHexGridDebugImages")
}

// Task to generate hex-grid debug overlay images on top of map background PNGs.
// Outputs go to map-debug-images/ (committed to git, outside composeResources so not compiled into the app).
// Runs automatically after generateMapImages and can also be invoked standalone.
tasks.register<JavaExec>("generateHexGridDebugImages") {
    group = "mapgen"
    description = "Generate hex-grid debug overlay images for all map background PNGs (committed to git)"
    dependsOn("compileKotlinDesktop")
    classpath = files(
        kotlin.targets.named("desktop").map { target ->
            (target as org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget)
                .compilations["main"].output.classesDirs
        },
        configurations.named("desktopRuntimeClasspath")
    )
    mainClass.set("de.egril.defender.mapgen.GenerateHexGridDebugImagesKt")
    workingDir = rootDir
    args = listOf(repositoryMapsDir, mapDebugImagesDir)
}

// Workaround for Gradle 9.x: Compose resource tasks declare output files that may not exist
// yet on a fresh build/clean, causing "Cannot access output property" errors. Marking them
// as untracked forces them to always run (still fast) and avoids the spurious failure.
tasks.matching { it.name.startsWith("copyNonXmlValueResourcesFor") }.configureEach {
    doNotTrackState("Gradle 9.x output-property validation workaround for Compose resource tasks")
}

// ---------------------------------------------------------------------------
// Asset-pack integration for AAB builds
//
// For Android App Bundle (`bundle*`) builds only:
//  1. The `assetPack*PreBundleTask` tasks (generated by AGP for each variant) are
//     made to depend on `:assetPack:syncComposeResources` and
//     `:assetPack:syncMaterialSymbolsFonts` so that the asset pack directory is fully
//     populated before AGP packages the asset pack.  These tasks are chosen over the
//     broader `bundle*` pattern to avoid adding large file-syncs as a dependency of
//     Kotlin class-bundling tasks (e.g. `bundleXxxClassesToCompileJar`).
//  2. A `doFirst` action on every `build*PreBundle` task removes both:
//       a) The app's own CMP package
//          (`composeResources/defender_of_egril.composeapp.generated.resources`) and
//       b) The compose-material-symbols library CMP package
//          (`composeResources/dev.vicart.compose.material.symbols.resources`)
//     from the base-module merged-assets directory before the base module is assembled.
//     Both packages are served at runtime from the install-time asset pack.  Removing
//     them from the base module keeps it under Google Play's 200 MB limit and avoids
//     the "Both modules contain asset entry" bundletool error that occurs when the same
//     file exists in both the base module and an asset pack.
//     The `build<Variant>PreBundle` task reads the merged-assets output and packages it
//     into the base module proto; deleting here ensures the large assets are gone
//     before that packaging step. Using doFirst (rather than doLast on the merge task)
//     guarantees the deletion always runs even when merge*Assets is UP-TO-DATE – which
//     happens when a prior APK build already executed the merge task in the same Gradle
//     user-home cache.
//
// For plain APK builds (`assemble*`) the deletion step is never triggered because
// the `build*PreBundle` task is not part of the task graph, so the APK remains
// self-contained and suitable for sideloading or local testing.
// ---------------------------------------------------------------------------

// CMP resource package for the compose-material-symbols library (must match the constant
// in the library's generated Font0.commonMain.kt:
//   private const val MD = "composeResources/dev.vicart.compose.material.symbols.resources/")
val materialSymbolsCmpPackage = "dev.vicart.compose.material.symbols.resources"

afterEvaluate {
    // 1. Make the AGP-generated asset-pack pre-bundle tasks wait for both syncs.
    //    These tasks are named `assetPack<Variant>PreBundleTask` and are the
    //    earliest point at which AGP reads the asset pack's src/main/assets directory.
    tasks.matching { it.name.startsWith("assetPack") && it.name.contains("PreBundle") }.configureEach {
        dependsOn(":assetPack:syncComposeResources")
        dependsOn(":assetPack:syncMaterialSymbolsFonts")
    }

    sanitizeWasmImportObjects.configure {
        dependsOn(
            tasks.matching {
                it.name.contains("wasmJs", ignoreCase = true) &&
                    it.name.contains("CompileSync", ignoreCase = true)
            },
        )
    }

    tasks.matching { it.name.contains("wasmJsBrowser", ignoreCase = true) }.configureEach {
        dependsOn(sanitizeWasmImportObjects)
    }
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
                menuGroup = "Game"
                iconFile.set(project.file("src/commonMain/composeResources/drawable/black-shield.png"))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// fixPackageDeb – post-processes the Compose Desktop DEB package so that
// the desktop entry and application icon are installed to the standard
// system paths (/usr/share/applications, /usr/share/pixmaps, and
// /usr/share/icons/hicolor).  Compose Desktop relies on xdg-desktop-menu in
// the generated postinst script, which silently fails during apt/dpkg
// installation because no desktop-environment context is present at that
// point.  This task replaces that approach with the Debian-standard method:
//   • /usr/share/applications/defender-of-egril.desktop  (tracked by dpkg)
//   • /usr/share/pixmaps/defender-of-egril.png           (icon by name)
//   • /usr/share/icons/hicolor/256x256/apps/defender-of-egril.png
//   • updated postinst that calls update-desktop-database and
//     gtk-update-icon-cache
// ---------------------------------------------------------------------------
tasks.register("fixPackageDeb") {
    dependsOn("packageDeb")

    doLast {
        // These names come from the linux { packageName } setting in nativeDistributions and
        // from jpackage's convention of deriving the application-name from the package name
        // (stripping hyphens and capitalising each word: defender-of-egril → DefenderOfEgril).
        // Update these constants if linux.packageName ever changes.
        val linuxPackageName = "defender-of-egril" // linux { packageName }
        val jpackageAppName = "DefenderOfEgril" // jpackage-derived launcher/icon name

        val debDir = layout.buildDirectory.dir("compose/binaries/main/deb").get().asFile
        val debFile =
            debDir.listFiles { f -> f.extension == "deb" }?.firstOrNull()
                ?: run {
                    logger.lifecycle("fixPackageDeb: no .deb file found in $debDir – skipping.")
                    return@doLast
                }

        val workDir = layout.buildDirectory.dir("deb-fix-staging").get().asFile
        workDir.deleteRecursively()
        workDir.mkdirs()

        try {
            // Unpack the DEB (data + DEBIAN/ control files)
            fun run(vararg cmd: String) {
                val exitCode = ProcessBuilder(*cmd).inheritIO().start().waitFor()
                check(exitCode == 0) { "Command failed (exit $exitCode): ${cmd.joinToString(" ")}" }
            }
            run("dpkg-deb", "-R", debFile.absolutePath, workDir.absolutePath)

            // 1. Install .desktop file directly to /usr/share/applications/ so that
            //    dpkg tracks it and desktop environments find it without needing
            //    xdg-desktop-menu.
            val appsDir = File(workDir, "usr/share/applications").also { it.mkdirs() }
            File(appsDir, "$linuxPackageName.desktop").writeText(
                "[Desktop Entry]\n" +
                    "Name=Defender of Egril\n" +
                    "Comment=Turn-based Tower Defense\n" +
                    "Exec=/opt/$linuxPackageName/bin/$jpackageAppName\n" +
                    "Icon=$linuxPackageName\n" +
                    "Terminal=false\n" +
                    "Type=Application\n" +
                    "Categories=Game;StrategyGame;\n",
            )

            // 2. Install the icon to /usr/share/pixmaps/ and the hicolor icon theme so
            //    desktop environments can look it up by name.
            // jpackage places the icon at /opt/{packageName}/lib/{AppName}.png
            val iconSrc = File(workDir, "opt/$linuxPackageName/lib/$jpackageAppName.png")
            if (iconSrc.exists()) {
                val pixmapsDir = File(workDir, "usr/share/pixmaps").also { it.mkdirs() }
                iconSrc.copyTo(File(pixmapsDir, "$linuxPackageName.png"), overwrite = true)

                val hicolorDir =
                    File(workDir, "usr/share/icons/hicolor/256x256/apps").also { it.mkdirs() }
                iconSrc.copyTo(File(hicolorDir, "$linuxPackageName.png"), overwrite = true)
            } else {
                logger.warn("fixPackageDeb: icon not found at ${iconSrc.absolutePath}")
            }

            // 3. Replace the postinst script with one that uses update-desktop-database
            //    and gtk-update-icon-cache instead of xdg-desktop-menu install.
            val postinstFile = File(workDir, "DEBIAN/postinst")
            postinstFile.writeText(
                "#!/bin/sh\n" +
                    "set -e\n" +
                    "case \"\$1\" in\n" +
                    "    configure)\n" +
                    "        if command -v update-desktop-database > /dev/null 2>&1; then\n" +
                    "            update-desktop-database -q /usr/share/applications\n" +
                    "        fi\n" +
                    "        if command -v gtk-update-icon-cache > /dev/null 2>&1; then\n" +
                    "            gtk-update-icon-cache -q -f /usr/share/icons/hicolor || true\n" +
                    "        fi\n" +
                    "    ;;\n" +
                    "    abort-upgrade|abort-remove|abort-deconfigure)\n" +
                    "    ;;\n" +
                    "    *)\n" +
                    "        echo \"postinst called with unknown argument '\$1'\" >&2\n" +
                    "        exit 1\n" +
                    "    ;;\n" +
                    "esac\n" +
                    "exit 0\n",
            )
            postinstFile.setExecutable(true)

            // 4. Repack the DEB in-place, overwriting the original file.
            run("dpkg-deb", "--build", "--root-owner-group", workDir.absolutePath, debFile.absolutePath)

            logger.lifecycle("fixPackageDeb: fixed DEB written to ${debFile.absolutePath}")
        } finally {
            workDir.deleteRecursively()
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
//   ./gradlew :composeApp:hotRunDesktop -Pprofile=local
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

    // ── Desktop: configure the built-in `run` task and hot reload runners ───
    tasks.named<JavaExec>("run") {
        jvmArgs(profileJvmArgs(profile))
        logger.lifecycle("Desktop 'run' task configured with profile '$profile'")
    }

    tasks.named<JavaExec>("hotRunDesktop") {
        val taskName = name
        jvmArgs(profileJvmArgs(profile))
        logger.lifecycle("Desktop '$taskName' task configured with profile '$profile'")
    }

    // ── Web/WASM: inject profile URLs into the production distribution ────────
    // The distribution task copies resources from src/wasmJsMain/resources/ into
    // the output directory (build/dist/wasmJs/productionExecutable/).  We patch
    // the Keycloak URL in the *output* index.html after the task completes so
    // that deployed builds point at the correct Keycloak instance.
    tasks.matching { it.name == "wasmJsBrowserDistribution" }.configureEach {
        val profileProps = loadProfileProperties(profile)
        val iamUrl = profileProps.getProperty("iam.base.url")

        doLast {
            if (iamUrl != null) {
                val outputIndexHtml = project.layout.buildDirectory.get().asFile
                    .resolve("dist/wasmJs/productionExecutable/index.html")
                if (outputIndexHtml.exists()) {
                    val original = outputIndexHtml.readText()
                    val urlLinePattern = Regex("""(window\.keycloakConfig\s*=\s*window\.keycloakConfig\s*\|\|\s*\{[^}]*\burl:\s*')[^']*""")
                    val modified = urlLinePattern.find(original)?.let { match ->
                        val replacement = "${match.groupValues[1]}$iamUrl"
                        original.substring(0, match.range.first) + replacement + original.substring(match.range.last + 1)
                    } ?: original
                    outputIndexHtml.writeText(modified)
                    logger.lifecycle("WASM distribution configured with IAM URL: $iamUrl (profile: $profile)")
                }
            }
        }
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
    if (configureAndroid) {
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
