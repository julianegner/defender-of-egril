package de.egril.defender.ui

import java.io.File
import java.net.URLDecoder

/**
 * Extracts credits information from live project assets (git history and README files).
 *
 * This extractor is the **source of truth** for what should appear in [FinalCreditsData].
 * The tests in [FinalCreditsDataTest] run this extractor and compare the results against
 * the static data in [FinalCreditsData]. If a test fails, update [FinalCreditsData] to
 * match the output of the corresponding extractor method.
 *
 * Trigger a re-run of the tests (and update [FinalCreditsData] if they fail) whenever:
 * - A new developer commits code with a non-noreply email address.
 * - New sound files with new Freesound.org author credits are added to
 *   `composeResources/files/sounds/README.md`.
 * - New background music from a new source is added to
 *   `composeResources/files/sounds/background/README.md`.
 */
object CreditsExtractor {

    /**
     * Extracts human developer names from git history using `git shortlog --summary --numbered --email`.
     *
     * Exclusion rules (applied in order):
     * 1. Entries whose email ends with `@users.noreply.github.com` are excluded —
     *    these are GitHub-generated no-reply addresses used by bots and automated tooling.
     *    Real developers using GitHub's privacy email setting must be added manually to
     *    [FinalCreditsData.developers].
     * 2. Names containing "copilot-swe-agent" are excluded.
     * 3. Names ending with "[bot]" are excluded.
     *
     * Returns a set of human-readable names (no email addresses).
     */
    fun extractDevelopers(projectRoot: File): Set<String> {
        return try {
            val process = ProcessBuilder(
                "git", "shortlog", "--summary", "--numbered", "--email"
            )
                .directory(projectRoot)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            // Line format: "   N\tName <email>"
            val nameEmailPattern = Regex("""^(.*?)\s*<([^>]+)>$""")
            output.lines()
                .mapNotNull { line ->
                    val afterTab = line.substringAfter("\t", "").trim()
                    val match = nameEmailPattern.find(afterTab) ?: return@mapNotNull null
                    val name = match.groupValues[1].trim()
                    val email = match.groupValues[2].trim()
                    when {
                        email.endsWith("@users.noreply.github.com") -> null
                        name.contains("copilot-swe-agent", ignoreCase = true) -> null
                        name.endsWith("[bot]") -> null
                        name.isEmpty() -> null
                        else -> name
                    }
                }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Extracts Freesound.org author names from the sound effects README.
     *
     * Scans for URLs matching `https://freesound.org/people/<Author>/sounds/...`
     * and URL-decodes the author segment (e.g. `Paul%20Sinnett` → `Paul Sinnett`).
     *
     * Returns a set of author names exactly as they appear in the Freesound URLs.
     */
    fun extractSoundEffectAuthors(projectRoot: File): Set<String> {
        val readmeFile = File(
            projectRoot,
            "composeApp/src/commonMain/composeResources/files/sounds/README.md"
        )
        if (!readmeFile.exists()) return emptySet()

        val pattern = Regex("""https://freesound\.org/people/([^/]+)/sounds/""")
        return readmeFile.readLines()
            .flatMap { line -> pattern.findAll(line).map { it.groupValues[1] } }
            .filter { it.isNotEmpty() }
            .map { URLDecoder.decode(it, "UTF-8") }
            .toSet()
    }

    /**
     * Extracts background music author names / source platforms from the background music README.
     *
     * The background README does not follow a strict machine-readable format, so this method
     * checks for the presence of a predefined list of known author or platform name strings.
     *
     * **Maintenance note**: When music from a **new** author or platform is added to the
     * background README, add an identifying string to [knownSources] below so the new
     * source is also validated.
     */
    fun extractBackgroundMusicAuthors(projectRoot: File): Set<String> {
        val readmeFile = File(
            projectRoot,
            "composeApp/src/commonMain/composeResources/files/sounds/background/README.md"
        )
        if (!readmeFile.exists()) return emptySet()

        val content = readmeFile.readText()
        val knownSources = listOf("David Fesliyan", "fesliyanstudios", "pixabay")
        return knownSources.filter { content.contains(it, ignoreCase = true) }.toSet()
    }
}
