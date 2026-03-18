package de.egril.defender.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Validates that [FinalCreditsData] stays in sync with the actual project assets.
 *
 * This test suite enforces the following invariants:
 *
 * 1. **testAllDevelopersAreListed** – Every human developer (non-bot committer) found
 *    in `git log` must be present in [FinalCreditsData.developers].
 *    Update [FinalCreditsData.developers] whenever a new human developer commits.
 *
 * 2. **testAllSoundEffectsAuthorsAreListed** – Every Freesound.org author referenced in
 *    `composeResources/files/sounds/README.md` must appear in
 *    [FinalCreditsData.soundEffectsCredits].
 *    Update the list whenever new sound files (with new authors) are added.
 *
 * 3. **testAllBackgroundMusicAuthorsAreListed** – Every music author mentioned in
 *    `composeResources/files/sounds/background/README.md` must appear in
 *    [FinalCreditsData.backgroundMusicCredits].
 *    Update the list whenever new music files are added.
 *
 * 4. **testBackgroundImagesExcludeEmojiAndTile** – All names in
 *    [FinalCreditsData.backgroundImageNames] must not start with "emoji_" or "tile_".
 *
 * 5. **testAllNonEmojiNonTileDrawablesAreListed** – Every PNG file in
 *    `composeResources/drawable` that does NOT start with "emoji_" or "tile_" must have
 *    a corresponding entry in [FinalCreditsData.backgroundImageNames].
 *    Update the list whenever new drawable images (without those prefixes) are added.
 */
class FinalCreditsDataTest {

    private val projectRoot: File = run {
        val currentDir = File(System.getProperty("user.dir"))
        if (currentDir.name == "composeApp") currentDir.parentFile else currentDir
    }

    // ─── Developer coverage ───────────────────────────────────────────────────

    /**
     * Returns the set of unique human committer names from git history.
     * Bot accounts are identified by names ending with "[bot]".
     */
    private fun humanDevelopersFromGit(): Set<String> {
        return try {
            val process = ProcessBuilder("git", "log", "--format=%an")
                .directory(projectRoot)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.endsWith("[bot]") }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    @Test
    fun testAllDevelopersAreListed() {
        val gitDevelopers = humanDevelopersFromGit()
        if (gitDevelopers.isEmpty()) {
            // Cannot read git history (e.g. shallow clone in CI) – skip gracefully
            return
        }

        val creditedDevelopers = FinalCreditsData.developers.toSet()
        val missing = gitDevelopers - creditedDevelopers

        if (missing.isNotEmpty()) {
            fail(
                "The following developers committed code but are missing from " +
                "FinalCreditsData.developers:\n" +
                missing.joinToString("\n") { "  - \"$it\"" } +
                "\nPlease add them to FinalCreditsData.kt."
            )
        }
    }

    // ─── Sound effects author coverage ───────────────────────────────────────

    /**
     * Extracts Freesound.org author names from a README.md.
     * Looks for lines matching `https://freesound.org/people/<Author>/sounds/...`.
     * URL-decodes percent-encoded characters (e.g. %20 → space).
     */
    private fun freesoundAuthorsFromReadme(readmeFile: File): Set<String> {
        if (!readmeFile.exists()) return emptySet()
        val pattern = Regex("""https://freesound\.org/people/([^/]+)/sounds/""")
        return readmeFile.readLines()
            .flatMap { pattern.findAll(it).map { m -> m.groupValues[1] } }
            .filter { it.isNotEmpty() }
            .map { java.net.URLDecoder.decode(it, "UTF-8") }
            .toSet()
    }

    @Test
    fun testAllSoundEffectsAuthorsAreListed() {
        val readmeFile = File(
            projectRoot,
            "composeApp/src/commonMain/composeResources/files/sounds/README.md"
        )
        assertTrue(readmeFile.exists(), "Sound effects README not found: ${readmeFile.absolutePath}")

        val readmeAuthors = freesoundAuthorsFromReadme(readmeFile)
        if (readmeAuthors.isEmpty()) {
            fail("Could not extract any Freesound author names from ${readmeFile.absolutePath}. " +
                 "Check the README format.")
        }

        val creditedAuthors = FinalCreditsData.soundEffectsCredits.map { it.author }.toSet()

        // Each README author must appear in at least one credit entry (case-insensitive)
        val missing = readmeAuthors.filter { readmeAuthor ->
            creditedAuthors.none { it.equals(readmeAuthor, ignoreCase = true) }
        }

        if (missing.isNotEmpty()) {
            fail(
                "The following Freesound.org authors appear in sounds/README.md but are " +
                "missing from FinalCreditsData.soundEffectsCredits:\n" +
                missing.joinToString("\n") { "  - \"$it\"" } +
                "\nPlease add them to FinalCreditsData.kt."
            )
        }
    }

    // ─── Background music author coverage ────────────────────────────────────

    /**
     * Extracts named authors from the background music README.
     * Looks for known author names/sources that appear in the file content.
     *
     * **Maintenance note**: When new music sources are added to the background README,
     * add the new author/source name to [knownAuthors] below AND to
     * [FinalCreditsData.backgroundMusicCredits]. This ensures the test continues to
     * verify coverage for all music sources.
     */
    private fun backgroundMusicAuthorsFromReadme(readmeFile: File): Set<String> {
        if (!readmeFile.exists()) return emptySet()
        val content = readmeFile.readText()
        val knownAuthors = listOf("David Fesliyan", "fesliyanstudios", "pixabay")
        return knownAuthors.filter { content.contains(it, ignoreCase = true) }.toSet()
    }

    @Test
    fun testAllBackgroundMusicAuthorsAreListed() {
        val readmeFile = File(
            projectRoot,
            "composeApp/src/commonMain/composeResources/files/sounds/background/README.md"
        )
        assertTrue(
            readmeFile.exists(),
            "Background music README not found: ${readmeFile.absolutePath}"
        )

        val readmeAuthors = backgroundMusicAuthorsFromReadme(readmeFile)
        val creditedText = FinalCreditsData.backgroundMusicCredits
            .joinToString(" ") { "${it.author} ${it.description}" }
            .lowercase()

        val missing = readmeAuthors.filter { author ->
            !creditedText.contains(author.lowercase())
        }

        if (missing.isNotEmpty()) {
            fail(
                "The following music authors/sources appear in sounds/background/README.md " +
                "but are missing from FinalCreditsData.backgroundMusicCredits:\n" +
                missing.joinToString("\n") { "  - \"$it\"" } +
                "\nPlease add them to FinalCreditsData.kt."
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
    fun testAllNonEmojiNonTileDrawablesAreListed() {
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

        val listedImages = FinalCreditsData.backgroundImageNames.toSet()
        val missing = actualImages - listedImages

        if (missing.isNotEmpty()) {
            fail(
                "The following drawable files (excluding emoji_* and tile_*) exist in " +
                "composeResources/drawable but are NOT listed in " +
                "FinalCreditsData.backgroundImageNames:\n" +
                missing.sorted().joinToString("\n") { "  - $it" } +
                "\nPlease add them to FinalCreditsData.kt."
            )
        }
    }
}
