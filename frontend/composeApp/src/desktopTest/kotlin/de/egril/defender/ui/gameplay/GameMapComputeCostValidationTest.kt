package de.egril.defender.ui.gameplay

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

class GameMapComputeCostValidationTest {
    private val projectRoot: File =
        run {
            val currentDir = File(System.getProperty("user.dir"))
            if (currentDir.name == "composeApp") {
                currentDir.parentFile
            } else {
                currentDir
            }
        }

    private val gameMapFile =
        File(
            projectRoot,
            "composeApp/src/commonMain/kotlin/de/egril/defender/ui/gameplay/GameMap.kt",
        )

    @Test
    fun hexagonalMapViewTileLambdaDoesNotContainAccumulatingComputeCost() {
        if (!gameMapFile.exists()) {
            fail("GameMap source file not found: ${gameMapFile.absolutePath}")
        }

        val tileLambda = extractTileLambda(gameMapFile.readText())
        val violations = mutableListOf<String>()
        val forbiddenCollectionScanPattern =
            Regex(
                """gameState\.(attackers|defenders|healingEffects|damageEffects|defeatedEnemyEffects|coinGainEffects|towerAttackEffects|fieldEffects|traps|barricades|constructionCompleteEffects|enemySpawnEffects|trapTriggerEffects|enemyMoveEffects|dragonLevelChangeEffects|mineDigEffects|fiefs|mushrooms|arrowAttackEffects|ballistaAttackEffects|bowAttackEffects|spearAttackEffects|pikeAttackEffects|wizardAttackEffects|alchemyAttackEffects)\s*\.\s*(any|count|filter|find|firstOrNull|flatMap|forEach|groupBy|lastOrNull|map|none|singleOrNull)\b""",
            )
        val forbiddenLoopPattern = Regex("""\b(for|while|repeat)\b""")

        tileLambda.lines().forEachIndexed { index, rawLine ->
            val codeLine = rawLine.substringBefore("//").trim()
            if (codeLine.isEmpty()) {
                return@forEachIndexed
            }

            if (forbiddenCollectionScanPattern.containsMatchIn(codeLine)) {
                violations += "Line ${index + 1}: $codeLine"
            }
            if (forbiddenLoopPattern.containsMatchIn(codeLine)) {
                violations += "Line ${index + 1}: $codeLine"
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                buildString {
                    appendLine("GameMap's HexagonalMapView tile lambda must stay O(1) per tile.")
                    appendLine("Move loops and list scans outside the per-cell lambda and use precomputed maps/sets instead.")
                    appendLine()
                    violations.forEach { appendLine(it) }
                },
            )
        }
    }

    private fun extractTileLambda(content: String): String {
        val startMarker = ") { position ->"
        val endMarker = "\n\n            MapControls("
        val startIndex = content.indexOf(startMarker)
        if (startIndex == -1) {
            fail("Could not find HexagonalMapView tile lambda start in GameMap.kt")
        }

        val endIndex = content.indexOf(endMarker, startIndex)
        if (endIndex == -1) {
            fail("Could not find HexagonalMapView tile lambda end in GameMap.kt")
        }

        return content.substring(startIndex + startMarker.length, endIndex)
    }
}
