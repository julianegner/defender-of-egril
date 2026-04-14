/**
 * Android install-time asset pack for Defender of Egril.
 *
 * This module holds all Compose Multiplatform resources (drawables, files, fonts, strings)
 * that would otherwise bloat the base APK split of the AAB above Google Play's 200 MB limit.
 *
 * How it works:
 *  1. The `syncComposeResources` task (defined below) copies every file from
 *     `composeApp/src/commonMain/composeResources/` into this module's
 *     `src/main/assets/composeResources/<cmpPackage>/` at build time.
 *  2. The main `:composeApp` module references this asset pack via
 *     `android { bundle { assetPacks += [":assetPack"] } }`.
 *  3. When building an AAB (`bundleRelease`), `:composeApp`'s `merge*Assets` task
 *     removes the `composeResources/` tree from the base module, leaving only code
 *     (DEX) and a handful of small metadata files in the base split.
 *  4. At runtime on Android 7.0+ the `AssetManager` transparently serves files
 *     from install-time asset pack splits, so Compose Multiplatform's `Res` API
 *     continues to work without any source-code changes.
 *
 * When building a plain APK (`assembleDebug` / `assembleRelease`) the deletion
 * step is skipped, so the APK remains self-contained and suitable for sideloading.
 *
 * The `src/main/assets/composeResources/` directory is auto-generated during every
 * bundle build and is listed in `.gitignore` — do not commit its contents.
 */
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

val syncComposeResources by tasks.registering(Sync::class) {
    description = "Copies composeResources into the asset pack before bundling."
    group = "build"

    from(rootProject.file("frontend/composeApp/src/commonMain/composeResources"))
    into(layout.projectDirectory.dir("src/main/assets/composeResources/$cmpPackage"))
}

// Every task in this module (manifest generation, packaging, etc.) must run
// after the sync so that the assets are in place when the pack is assembled.
tasks.configureEach {
    if (name != syncComposeResources.name) {
        dependsOn(syncComposeResources)
    }
}
