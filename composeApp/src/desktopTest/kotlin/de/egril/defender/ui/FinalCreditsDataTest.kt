package de.egril.defender.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Validates that [FinalCreditsData] stays in sync with the actual project assets.
 *
 * All extraction logic lives in [CreditsExtractor]. These tests simply compare the
 * extractor's output against the static lists in [FinalCreditsData]. When a test fails,
 * update [FinalCreditsData] to match what [CreditsExtractor] reports.
 *
 * Tests:
 *
 * 1. **testDevelopersAreUpToDate** – Every developer found by [CreditsExtractor.extractDevelopers]
 *    must be present in [FinalCreditsData.developers].
 *    Developers who commit with a GitHub no-reply address must be added **manually**.
 *
 * 2. **testSoundEffectCreditsAreUpToDate** – Every Freesound.org author found by
 *    [CreditsExtractor.extractSoundEffectAuthors] must appear in
 *    [FinalCreditsData.soundEffectsCredits].
 *
 * 3. **testBackgroundMusicCreditsAreUpToDate** – Every music author/source found by
 *    [CreditsExtractor.extractBackgroundMusicAuthors] must appear in
 *    [FinalCreditsData.backgroundMusicCredits].
 *
 * 4. **testBackgroundImagesExcludeEmojiAndTile** – No entry in
 *    [FinalCreditsData.backgroundImageNames] may start with "emoji_" or "tile_".
 *
 * 5. **testAllNonEmojiNonTileDrawablesAccountedFor** – Every PNG in
 *    `composeResources/drawable` that does NOT start with "emoji_" or "tile_" must
 *    appear in either [FinalCreditsData.backgroundImageNames] or
 *    [FinalCreditsData.backgroundImageExclusions].
 */
class FinalCreditsDataTest {

    private val projectRoot: File = run {
        val currentDir = File(System.getProperty("user.dir"))
        if (currentDir.name == "composeApp") currentDir.parentFile else currentDir
    }

    // ─── Developer coverage ───────────────────────────────────────────────────

    @Test
    fun testDevelopersAreUpToDate() {
        val extracted = CreditsExtractor.extractDevelopers(projectRoot)
        if (extracted.isEmpty()) {
            // Cannot reach git history (e.g. shallow CI clone without noreply-filtered commits) – skip.
            return
        }

        val credited = FinalCreditsData.developers.toSet()
        val missing = extracted - credited

        if (missing.isNotEmpty()) {
            fail(
                "The following developers committed with a non-noreply email but are missing " +
                "from FinalCreditsData.developers.\n" +
                "Run CreditsExtractor.extractDevelopers() and add them to FinalCreditsData.kt:\n" +
                missing.joinToString("\n") { "  - \"$it\"" }
            )
        }
    }

    // ─── Sound effects author coverage ───────────────────────────────────────

    @Test
    fun testSoundEffectCreditsAreUpToDate() {
        val readmeFile = File(
            projectRoot,
            "composeApp/src/commonMain/composeResources/files/sounds/README.md"
        )
        assertTrue(readmeFile.exists(), "Sound effects README not found: ${readmeFile.absolutePath}")

        val extracted = CreditsExtractor.extractSoundEffectAuthors(projectRoot)
        if (extracted.isEmpty()) {
            fail(
                "CreditsExtractor could not extract any Freesound author names from " +
                "${readmeFile.absolutePath}. Check that the README contains freesound.org URLs."
            )
        }

        val credited = FinalCreditsData.soundEffectsCredits.map { it.author }.toSet()
        val missing = extracted.filter { author ->
            credited.none { it.equals(author, ignoreCase = true) }
        }

        if (missing.isNotEmpty()) {
            fail(
                "The following Freesound.org authors appear in sounds/README.md but are " +
                "missing from FinalCreditsData.soundEffectsCredits.\n" +
                "Run CreditsExtractor.extractSoundEffectAuthors() and update FinalCreditsData.kt:\n" +
                missing.joinToString("\n") { "  - \"$it\"" }
            )
        }
    }

    // ─── Background music author coverage ────────────────────────────────────

    @Test
    fun testBackgroundMusicCreditsAreUpToDate() {
        val readmeFile = File(
            projectRoot,
            "composeApp/src/commonMain/composeResources/files/sounds/background/README.md"
        )
        assertTrue(
            readmeFile.exists(),
            "Background music README not found: ${readmeFile.absolutePath}"
        )

        val extracted = CreditsExtractor.extractBackgroundMusicAuthors(projectRoot)
        val creditedText = FinalCreditsData.backgroundMusicCredits
            .joinToString(" ") { "${it.author} ${it.description}" }
            .lowercase()

        val missing = extracted.filter { !creditedText.contains(it.lowercase()) }

        if (missing.isNotEmpty()) {
            fail(
                "The following music authors/sources appear in sounds/background/README.md " +
                "but are missing from FinalCreditsData.backgroundMusicCredits.\n" +
                "Run CreditsExtractor.extractBackgroundMusicAuthors() and update FinalCreditsData.kt:\n" +
                missing.joinToString("\n") { "  - \"$it\"" }
            )
        }
    }

    // ─── Background image coverage ────────────────────────────────────────────

    @Test
    fun testBackgroundImagesExcludeEmojiAndTile() {
        val violations = FinalCreditsData.backgroundImageNames.filter { name ->
            name.startsWith("emoji_") || name.startsWith("tile_")
        }
        if (violations.isNotEmpty()) {
            fail(
                "FinalCreditsData.backgroundImageNames contains entries with forbidden " +
                "prefixes (emoji_ or tile_):\n" +
                violations.joinToString("\n") { "  - $it" }
            )
        }
    }

    @Test
    fun testAllNonEmojiNonTileDrawablesAccountedFor() {
        val drawableDir = File(
            projectRoot,
            "composeApp/src/commonMain/composeResources/drawable"
        )
        assertTrue(drawableDir.exists(), "Drawable directory not found: ${drawableDir.absolutePath}")

        val actualImages = drawableDir.listFiles()
            ?.filter { file ->
                file.isFile &&
                file.extension.lowercase() == "png" &&
                !file.name.startsWith("emoji_") &&
                !file.name.startsWith("tile_")
            }
            ?.map { it.nameWithoutExtension.replace("-", "_") }
            ?.toSet()
            ?: emptySet()

        val accounted = FinalCreditsData.backgroundImageNames.toSet() +
                        FinalCreditsData.backgroundImageExclusions
        val missing = actualImages - accounted

        if (missing.isNotEmpty()) {
            fail(
                "The following drawable files (excluding emoji_* and tile_*) are not " +
                "accounted for in FinalCreditsData. Add each to either " +
                "backgroundImageNames (to show it) or backgroundImageExclusions (to skip it):\n" +
                missing.sorted().joinToString("\n") { "  - $it" }
            )
        }
    }
}

