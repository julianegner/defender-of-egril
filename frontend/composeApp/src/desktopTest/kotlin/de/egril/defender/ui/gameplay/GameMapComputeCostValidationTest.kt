package de.egril.defender.ui.gameplay

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class GameMapComputeCostValidationTest {
    private companion object {
        private val guardedGameStateCollections =
            listOf(
                "attackers",
                "defenders",
                "healingEffects",
                "damageEffects",
                "defeatedEnemyEffects",
                "coinGainEffects",
                "towerAttackEffects",
                "fieldEffects",
                "traps",
                "barricades",
                "constructionCompleteEffects",
                "enemySpawnEffects",
                "trapTriggerEffects",
                "enemyMoveEffects",
                "dragonLevelChangeEffects",
                "mineDigEffects",
                "fiefs",
                "mushrooms",
                "arrowAttackEffects",
                "ballistaAttackEffects",
                "bowAttackEffects",
                "spearAttackEffects",
                "pikeAttackEffects",
                "wizardAttackEffects",
                "alchemyAttackEffects",
            )
        private val forbiddenCollectionOperations =
            listOf(
                "any",
                "associate",
                "associateBy",
                "associateWith",
                "count",
                "filter",
                "find",
                "firstOrNull",
                "flatMap",
                "fold",
                "forEach",
                "groupBy",
                "lastOrNull",
                "map",
                "maxOf",
                "maxOfOrNull",
                "minOf",
                "minOfOrNull",
                "none",
                "reduce",
                "reduceOrNull",
                "singleOrNull",
                "sumOf",
            )
        private val guardedCollectionsPattern = guardedGameStateCollections.joinToString(separator = "|")
        private val forbiddenOperationsPattern = forbiddenCollectionOperations.joinToString(separator = "|")
        private val guardedCollectionReferencePattern = Regex("""gameState\.($guardedCollectionsPattern)\b""")
        private val forbiddenCollectionScanPattern =
            Regex(
                """gameState\.($guardedCollectionsPattern)\s*\.\s*($forbiddenOperationsPattern)\b""",
            )
        private val forbiddenLoopPatterns =
            listOf(
                Regex("""\bfor\s*\("""),
                Regex("""\bwhile\s*\("""),
                Regex("""\brepeat\s*\("""),
            )
        private val gameGridPattern = Regex("""@Composable\s+fun\s+GameGrid\s*\(""")
        private val hexagonalMapViewPattern = Regex("""HexagonalMapView\s*\(""")
        private const val gameMapModuleRelativePath =
            "src/commonMain/kotlin/de/egril/defender/ui/gameplay/GameMap.kt"
    }

    private val gameMapFile = findGameMapFile()

    @Test
    fun hexagonalMapViewTileLambdaDoesNotContainAccumulatingComputeCost() {
        if (!gameMapFile.exists()) {
            fail("GameMap source file not found: ${gameMapFile.absolutePath}")
        }

        val tileLambda = extractHexagonalMapViewTileLambda(gameMapFile.readText())
        val violations = findViolations(tileLambda)
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

    @Test
    fun validatorIgnoresCommentsAndStrings() {
        val tileLambda =
            listOf(
                "val description = \"repeat for gameState.attackers.any should stay in this string\"",
                "val rawDescription = \"\"\"",
                "repeat(gameState.attackers.size)",
                "for (attacker in gameState.attackers) { }",
                "gameState.attackers.any { true }",
                "\"\"\".trimIndent()",
                "/* for (attacker in gameState.attackers) { } */",
                "// while gameState.defenders.any { true }",
                "val isHovering = hoveredPosition == position",
            ).joinToString(separator = "\n")

        assertTrue(findViolations(tileLambda).isEmpty(), "Comments and strings must not trigger violations")
    }

    @Test
    fun validatorDetectsLoopsAndCollectionScans() {
        val tileLambda =
            """
            for (attacker in gameState.attackers) {
                println(attacker)
            }
            val hasEnemy = gameState.attackers.any { it.position.value == position }
            """.trimIndent()

        val violations = findViolations(tileLambda)
        assertEquals(2, violations.size)
        assertContains(violations[0], "for (attacker in gameState.attackers)")
        assertContains(violations[1], "gameState.attackers.any")
    }

    @Test
    fun validatorAllowsLoopsThatDoNotScanGuardedCollections() {
        val tileLambda =
            """
            for (neighbor in position.getHexNeighbors()) {
                println(neighbor)
            }
            """.trimIndent()

        assertTrue(findViolations(tileLambda).isEmpty(), "Constant-size loops unrelated to guarded gameState collections should be allowed")
    }

    @Test
    fun tileLambdaExtractionHandlesNestedArgumentLambdas() {
        val source =
            """
            @Composable
            fun GameGrid() {
                HexagonalMapView(
                    backgroundContent = { size ->
                        println(size)
                    },
                    overlayContent = { measuredContentSize ->
                        listOf(1, 2, 3).forEach { println(it + measuredContentSize.width) }
                    },
                ) { position ->
                    val isHovering = hoveredPosition == position
                    GridCell(position = position, isHovering = isHovering)
                }
            }
            """.trimIndent()

        val extracted = extractHexagonalMapViewTileLambda(source)
        assertContains(extracted, "val isHovering = hoveredPosition == position")
        assertContains(extracted, "GridCell(position = position, isHovering = isHovering)")
    }

    private fun findViolations(tileLambda: String): List<String> {
        val sanitizedLines = stripCommentsAndStrings(tileLambda).lines()
        val originalLines = tileLambda.lines()
        val violations = mutableListOf<String>()

        sanitizedLines.forEachIndexed { index, sanitizedLine ->
            val codeLine = sanitizedLine.trim()
            if (codeLine.isEmpty()) {
                return@forEachIndexed
            }

            if (
                forbiddenCollectionScanPattern.containsMatchIn(codeLine) ||
                (
                    guardedCollectionReferencePattern.containsMatchIn(codeLine) &&
                        forbiddenLoopPatterns.any { it.containsMatchIn(codeLine) }
                )
            ) {
                violations += "Line ${index + 1}: ${originalLines[index].trim()}"
            }
        }
        return violations
    }

    private fun extractHexagonalMapViewTileLambda(content: String): String {
        val sanitizedContent = stripCommentsAndStrings(content)
        val gameGridMatch = gameGridPattern.find(sanitizedContent)
        if (gameGridMatch == null) {
            fail("Could not find GameGrid composable in GameMap.kt")
        }
        val gameGridStart = gameGridMatch.range.first

        val gameGridOpeningBrace = content.indexOf('{', gameGridStart)
        if (gameGridOpeningBrace == -1) {
            fail("Could not find GameGrid body start in GameMap.kt")
        }

        val gameGridClosingBrace = findMatchingClosingDelimiter(content, gameGridOpeningBrace, '{', '}')
        if (gameGridClosingBrace == -1) {
            fail("Could not find GameGrid body end in GameMap.kt")
        }

        val gameGridBody = sanitizedContent.substring(gameGridOpeningBrace + 1, gameGridClosingBrace)
        val localInvocationMatch = hexagonalMapViewPattern.find(gameGridBody)
        if (localInvocationMatch == null) {
            fail("Could not find HexagonalMapView call inside GameGrid in GameMap.kt")
        }
        val localInvocationStart = localInvocationMatch.range.first

        val invocationStart = gameGridOpeningBrace + 1 + localInvocationStart
        val openingParenthesisIndex = content.indexOf('(', invocationStart)
        val closingParenthesisIndex = findMatchingClosingDelimiter(content, openingParenthesisIndex, '(', ')')
        if (closingParenthesisIndex == -1) {
            fail("Could not find HexagonalMapView argument list end in GameMap.kt")
        }

        var lambdaStart = closingParenthesisIndex + 1
        while (lambdaStart < content.length && content[lambdaStart].isWhitespace()) {
            lambdaStart++
        }
        if (lambdaStart >= content.length || content[lambdaStart] != '{') {
            fail("Could not find HexagonalMapView trailing lambda in GameMap.kt")
        }

        val lambdaEnd = findMatchingClosingDelimiter(content, lambdaStart, '{', '}')
        if (lambdaEnd == -1) {
            fail("Could not find HexagonalMapView trailing lambda end in GameMap.kt")
        }

        return content.substring(lambdaStart + 1, lambdaEnd)
    }

    private fun findGameMapFile(): File {
        val codeSourceLocation =
            runCatching {
                File(GameMapComputeCostValidationTest::class.java.protectionDomain.codeSource.location.toURI()).absoluteFile
            }.getOrElse { error ->
                fail("Could not resolve test class location for GameMap lookup: ${error.message}")
            }
        var currentDir = if (codeSourceLocation.isDirectory) codeSourceLocation else codeSourceLocation.parentFile

        while (true) {
            val moduleRootCandidate = File(currentDir, gameMapModuleRelativePath)
            if (moduleRootCandidate.exists()) {
                return moduleRootCandidate
            }

            currentDir = currentDir.parentFile ?: break
        }

        fail("Could not locate GameMap.kt from classpath location=${codeSourceLocation.absolutePath}")
    }

    private fun findMatchingClosingDelimiter(
        source: String,
        openingIndex: Int,
        opening: Char,
        closing: Char,
    ): Int {
        var depth = 0
        var index = openingIndex
        var inLineComment = false
        var inBlockComment = false
        var inString = false
        var inRawString = false
        var inChar = false
        var escaping = false

        while (index < source.length) {
            val current = source[index]
            val next = source.getOrNull(index + 1)
            val third = source.getOrNull(index + 2)

            if (inLineComment) {
                if (current == '\n') {
                    inLineComment = false
                }
                index++
                continue
            }

            if (inBlockComment) {
                if (current == '*' && next == '/') {
                    inBlockComment = false
                    index += 2
                } else {
                    index++
                }
                continue
            }

            if (inRawString) {
                if (current == '"' && next == '"' && third == '"') {
                    inRawString = false
                    index += 3
                } else {
                    index++
                }
                continue
            }

            if (inString) {
                if (!escaping && current == '"') {
                    inString = false
                }
                escaping = !escaping && current == '\\'
                index++
                continue
            }

            if (inChar) {
                if (!escaping && current == '\'') {
                    inChar = false
                }
                escaping = !escaping && current == '\\'
                index++
                continue
            }

            escaping = false

            if (current == '/' && next == '/') {
                inLineComment = true
                index += 2
                continue
            }
            if (current == '/' && next == '*') {
                inBlockComment = true
                index += 2
                continue
            }
            if (current == '"' && next == '"' && third == '"') {
                inRawString = true
                index += 3
                continue
            }
            if (current == '"') {
                inString = true
                index++
                continue
            }
            if (current == '\'') {
                inChar = true
                index++
                continue
            }
            if (current == opening) {
                depth++
            } else if (current == closing) {
                depth--
                if (depth == 0) {
                    return index
                }
            }
            index++
        }

        return -1
    }

    private fun stripCommentsAndStrings(source: String): String {
        val result = StringBuilder(source.length)
        var index = 0
        var inLineComment = false
        var inBlockComment = false
        var inString = false
        var inRawString = false
        var inChar = false
        var escaping = false

        while (index < source.length) {
            val current = source[index]
            val next = source.getOrNull(index + 1)
            val third = source.getOrNull(index + 2)

            if (inLineComment) {
                if (current == '\n') {
                    inLineComment = false
                    result.append('\n')
                } else {
                    result.append(' ')
                }
                index++
                continue
            }

            if (inBlockComment) {
                if (current == '*' && next == '/') {
                    result.append("  ")
                    inBlockComment = false
                    index += 2
                } else {
                    if (current == '\n') {
                        result.append('\n')
                    } else {
                        result.append(' ')
                    }
                    index++
                }
                continue
            }

            if (inRawString) {
                if (current == '\n') {
                    result.append('\n')
                } else {
                    result.append(' ')
                }
                if (current == '"' && next == '"' && third == '"') {
                    result.append("  ")
                    inRawString = false
                    index += 3
                } else {
                    index++
                }
                continue
            }

            if (inString) {
                if (current == '\n') {
                    result.append('\n')
                } else {
                    result.append(' ')
                }
                if (!escaping && current == '"') {
                    inString = false
                }
                escaping = !escaping && current == '\\'
                index++
                continue
            }

            if (inChar) {
                result.append(' ')
                if (!escaping && current == '\'') {
                    inChar = false
                }
                escaping = !escaping && current == '\\'
                index++
                continue
            }

            escaping = false

            if (current == '/' && next == '/') {
                result.append("  ")
                inLineComment = true
                index += 2
                continue
            }
            if (current == '/' && next == '*') {
                result.append("  ")
                inBlockComment = true
                index += 2
                continue
            }
            if (current == '"' && next == '"' && third == '"') {
                inRawString = true
                result.append("   ")
                index += 3
                continue
            }
            if (current == '"') {
                inString = true
                result.append(' ')
                index++
                continue
            }
            if (current == '\'') {
                inChar = true
                result.append(' ')
                index++
                continue
            }

            result.append(current)
            index++
        }

        return result.toString()
    }
}
