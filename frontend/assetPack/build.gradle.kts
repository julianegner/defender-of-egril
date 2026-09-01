// Android install-time asset pack for Defender of Egril.
//
// This module holds all Compose Multiplatform resources (drawables, files, fonts, strings)
// that would otherwise bloat the base APK split of the AAB above Google Play's 200 MB limit.
//
// How it works:
//  1. The `syncComposeResources` task copies every file from
//     `composeApp/src/commonMain/composeResources/` into this module's
//     `src/main/assets/composeResources/<cmpPackage>/` at build time.
//  2. The `syncMaterialSymbolsFonts` task extracts the Material Symbols TTF fonts from the
//     `compose-material-symbols` library AAR and places them here under their expected CMP
//     resource path (`composeResources/dev.vicart.compose.material.symbols.resources/font/`).
//     This is necessary because in an AAB install the base-module assets (where library AAR
//     assets normally land) are not reliably reachable via AssetManager when the app's own
//     CMP resources live in this asset pack.  Keeping all CMP resources in one place avoids
//     the issue entirely.  The corresponding entry is also removed from the base-module
//     merged-assets in `:composeApp`'s `build*PreBundle` doFirst hook so that bundletool
//     does not complain about duplicate asset entries.
//  3. The main `:composeApp` module references this asset pack via
//     `android { bundle { assetPacks += [":assetPack"] } }`.
//  4. When building an AAB (`bundleRelease`), `:composeApp`'s `merge*Assets` task
//     removes the app's and the library's CMP resource trees from the base module, leaving
//     only DEX and metadata in the base split.
//  5. At runtime on Android 7.0+ the `AssetManager` transparently serves files
//     from install-time asset pack splits, so Compose Multiplatform's `Res` API
//     continues to work without any source-code changes.
//
// When building a plain APK (`assembleDebug` / `assembleRelease`) the deletion
// step is skipped, so the APK remains self-contained and suitable for sideloading.
//
// The `src/main/assets/composeResources/` directory is auto-generated during every
// bundle build and is listed in `.gitignore` — do not commit its contents.
plugins {
    alias(libs.plugins.androidAssetPack)
}

assetPack {
    packName = "main_install_assets"
    dynamicDelivery {
        deliveryType = "install-time"
    }
}

// ---------------------------------------------------------------------------
// Sync task – copies Compose Multiplatform resources from the source tree
// into this module's asset directory using the path prefix that CMP expects
// at runtime:  composeResources/<cmpPackage>/<type>/<file>
// ---------------------------------------------------------------------------

// The Compose Multiplatform resource-class package for composeApp, as seen in
// the generated import statements (e.g. defender_of_egril.composeapp.generated.resources.Res).
val cmpPackage = "defender_of_egril.composeapp.generated.resources"

val syncComposeResources = tasks.register<Sync>("syncComposeResources") {
    description = "Copies composeResources into the asset pack before bundling."
    group = "build"

    from(rootProject.file("frontend/composeApp/src/commonMain/composeResources"))
    into(layout.projectDirectory.dir("src/main/assets/composeResources/$cmpPackage"))
}

// ---------------------------------------------------------------------------
// Material Symbols font sync – extracts TTF font files from the library AAR
// ---------------------------------------------------------------------------
//
// The compose-material-symbols library bundles its variable-font TTF files
// inside its AAR under:
//   assets/composeResources/dev.vicart.compose.material.symbols.resources/font/
//
// In a plain APK build these files end up in the merged APK assets and are
// found by CMP's AssetManager-backed ResourceReader.  In an AAB build they
// land in the *base* module assets, while the app's CMP resources (and now
// also the library fonts) live in this install-time asset pack.  Having them
// in only the base module makes them unreachable in practice, so we copy them
// here explicitly.
//
// A dedicated resolvable configuration is used so that Gradle can download
// the AAR without pulling in transitive dependencies.  The repositories are
// inherited from the root project's `dependencyResolutionManagement` block
// (mavenCentral, google, JetBrains Space), so no extra repository declaration
// is needed here.
val materialSymbolsFontDeps = configurations.create("materialSymbolsFontDeps") {
    isTransitive = false
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    // Request the Android-specific AAR artifact directly so we can unzip its assets.
    materialSymbolsFontDeps(
        "dev.vicart:compose-material-symbols-android:${libs.versions.compose.material.symbols.get()}@aar",
    )
}

// CMP resource package path used by the library (matches the prefix constant in
// the library's generated Font0.commonMain.kt source):
//   private const val MD = "composeResources/dev.vicart.compose.material.symbols.resources/"
val materialSymbolsCmpPackage = "dev.vicart.compose.material.symbols.resources"

// Copy (not Sync) so we do not accidentally delete the app resources written by
// `syncComposeResources` which targets a sibling subdirectory of `src/main/assets`.
val syncMaterialSymbolsFonts = tasks.register<Copy>("syncMaterialSymbolsFonts") {
    description = "Extracts Material Symbols TTF fonts from the library AAR into the asset pack."
    group = "build"

    from({
        zipTree(materialSymbolsFontDeps.singleFile)
    }) {
        // Only include the font assets; other AAR entries (classes.jar, etc.) are not needed.
        include("assets/composeResources/$materialSymbolsCmpPackage/**")
        eachFile {
            // Strip the leading "assets/" segment so the file lands at
            // src/main/assets/composeResources/dev.vicart.../font/material_symbols_*.ttf
            relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
        }
        includeEmptyDirs = false
    }
    into(layout.projectDirectory.dir("src/main/assets"))
}

// Only make tasks that actually generate or package the asset pack depend on the syncs.
// Using targeted matching avoids unnecessary dependencies for help, clean, and other
// unrelated tasks that would otherwise trigger large file copies on every invocation.
tasks
    .matching {
        it.name.startsWith("generate") || it.name.startsWith("package")
    }.configureEach {
        dependsOn(syncComposeResources)
        dependsOn(syncMaterialSymbolsFonts)
    }
