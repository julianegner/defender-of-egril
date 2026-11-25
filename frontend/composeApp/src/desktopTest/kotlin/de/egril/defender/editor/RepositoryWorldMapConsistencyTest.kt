package de.egril.defender.editor

import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Runs the world map consistency check against the official repository data.
 * Prints the full report, and fails only on ERROR-level findings.
 */
class RepositoryWorldMapConsistencyTest {
    private val repositoryDir = File("src/commonMain/composeResources/files/repository")

    @Test
    fun officialRepositoryHasNoWorldMapErrors() {
        val sequence = EditorJsonSerializer.deserializeSequence(File(repositoryDir, "sequence.json").readText())
        val worldMap = EditorJsonSerializer.deserializeWorldMapData(File(repositoryDir, "worldmap.json").readText())
        assertNotNull(sequence)
        assertNotNull(worldMap)

        val issues =
            WorldMapConsistencyChecker.check(
                sequence.sequence,
                worldMap,
                ignoredLocationIds = setOf("ewhads_challege"), // Test level, intentionally hidden
            )
        println("World map consistency report (${issues.size} findings):")
        issues.forEach { println("  [${it.severity}] ${it.message}") }

        val errors = issues.filter { it.severity == ConsistencySeverity.ERROR }
        assertTrue(errors.isEmpty(), "Errors found:\n" + errors.joinToString("\n") { it.message })
    }
}
