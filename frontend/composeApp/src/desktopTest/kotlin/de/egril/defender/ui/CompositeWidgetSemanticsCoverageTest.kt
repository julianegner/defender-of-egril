package de.egril.defender.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Guards Phase 1 V2 semantics on key composite widgets.
 */
class CompositeWidgetSemanticsCoverageTest {
    private val projectRoot: File =
        run {
            val currentDir = File(System.getProperty("user.dir"))
            if (currentDir.name == "composeApp") currentDir.parentFile else currentDir
        }

    @Test
    fun testCompositeWidgetsUseAccessibilitySemanticsSummaries() {
        assertFileContains(
            relativePath = "composeApp/src/commonMain/kotlin/de/egril/defender/ui/worldmap/LevelCard.kt",
            requiredTokens = listOf("levelCardLabel", "a11ySemantics("),
        )
        assertFileContains(
            relativePath = "composeApp/src/commonMain/kotlin/de/egril/defender/ui/loadgame/SavedGameCard.kt",
            requiredTokens = listOf("saveCardLabel", "a11ySemantics("),
        )
        assertFileContains(
            relativePath = "composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/AttackerInfo.kt",
            requiredTokens = listOf("attackerCardLabel", "a11ySemantics("),
        )
        assertFileContains(
            relativePath = "composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/DefenderInfo.kt",
            requiredTokens = listOf("defenderCardLabel", "a11ySemantics("),
        )
        assertFileContains(
            relativePath = "composeApp/src/commonMain/kotlin/de/egril/defender/ui/worldmap/ImageWorldMapView.kt",
            requiredTokens = listOf("locationMarkerLabel", "a11ySemantics(role = Role.Button"),
        )
    }

    private fun assertFileContains(
        relativePath: String,
        requiredTokens: List<String>,
    ) {
        val file = File(projectRoot, relativePath)
        if (!file.exists()) {
            fail("Expected file not found: ${file.absolutePath}")
        }
        val content = file.readText()
        requiredTokens.forEach { token ->
            assertTrue(
                content.contains(token),
                "Expected '$relativePath' to contain token: $token",
            )
        }
    }
}
