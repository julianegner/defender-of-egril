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
        private val guardedCollectionsPattern =
            guardedGameStateCollections.joinToString(separator = "|") { Regex.escape(it) }
        private val forbiddenOperationsPattern =
            forbiddenCollectionOperations.joinToString(separator = "|") { Regex.escape(it) }
        private val forbiddenCollectionScanPattern =
            Regex(
                """gameState\.($guardedCollectionsPattern)\s*\.\s*($forbiddenOperationsPattern)\b""",
                setOf(RegexOption.DOT_MATCHES_ALL),
            )
        private val forbiddenLoopOnGuardedCollectionPatterns =
            listOf(
                Regex(
                    """\bfor\s*\([^)]*gameState\.($guardedCollectionsPattern)\b[^)]*\)""",
                    setOf(RegexOption.DOT_MATCHES_ALL),
                ),
                Regex(
                    """\bwhile\s*\([^)]*gameState\.($guardedCollectionsPattern)\b[^)]*\)""",
                    setOf(RegexOption.DOT_MATCHES_ALL),
                ),
                Regex(
                    """\brepeat\s*\(\s*gameState\.($guardedCollectionsPattern)\b[^)]*\)""",
                    setOf(RegexOption.DOT_MATCHES_ALL),
                ),
            )
        private val gridCellCallPattern = Regex("""\bGridCell\s*\(""")
        private val hexagonalMapViewPattern = Regex("""\bHexagonalMapView\s*\(""")
        private const val gameMapModuleRelativePath =
            "src/commonMain/kotlin/de/egril/defender/ui/gameplay/GameMap.kt"
    }

    @Test
    fun hexagonalMapViewTileLambdaDoesNotContainAccumulatingComputeCost() {
        val gameMapFile = findGameMapFile()
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
    fun validatorIgnoresCharsAndEscapes() {
        val tileLambda =
            listOf(
                "val quote = '\"'",
                "val slash = '\\\\'",
                "val text = \"escaped quote: \\\"repeat(gameState.attackers.size)\\\"\"",
                "val isHovering = hoveredPosition == position",
            ).joinToString(separator = "\n")

        assertTrue(findViolations(tileLambda).isEmpty(), "Char literals and escaped quotes/backslashes must not trigger violations")
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

    @Test
    fun productionGameMapExtractionFindsGridCellTileLambda() {
        val extracted = extractHexagonalMapViewTileLambda(findGameMapFile().readText())
        assertContains(extracted, "GridCell(")
        assertContains(extracted, "defender = defendersByPosition[position]")
    }

    private fun findViolations(tileLambda: String): List<String> {
        val sanitizedLambda = stripCommentsAndStrings(tileLambda)
        val matches =
            buildList {
                forbiddenCollectionScanPattern.findAll(sanitizedLambda).forEach { add(it.range.first) }
                forbiddenLoopOnGuardedCollectionPatterns.forEach { pattern ->
                    pattern.findAll(sanitizedLambda).forEach { add(it.range.first) }
                }
            }.sorted().distinct()

        return matches.map { matchIndex ->
            val lineNumber = lineNumberAt(sanitizedLambda, matchIndex)
            "Line $lineNumber: ${tileLambda.lines()[lineNumber - 1].trim()}"
        }
    }

    private fun extractHexagonalMapViewTileLambda(content: String): String {
        val sanitizedContent = stripCommentsAndStrings(content)
        val invocationMatches = hexagonalMapViewPattern.findAll(sanitizedContent).toList()
        for (invocationMatch in invocationMatches) {
            val openingParenthesisIndex = sanitizedContent.indexOf('(', invocationMatch.range.first)
            val closingParenthesisIndex = findMatchingClosingDelimiter(content, openingParenthesisIndex, '(', ')')
            if (closingParenthesisIndex == -1) {
                continue
            }

            var lambdaStart = closingParenthesisIndex + 1
            while (lambdaStart < sanitizedContent.length && sanitizedContent[lambdaStart].isWhitespace()) {
                lambdaStart++
            }
            if (lambdaStart >= sanitizedContent.length || sanitizedContent[lambdaStart] != '{') {
                continue
            }

            val lambdaEnd = findMatchingClosingDelimiter(content, lambdaStart, '{', '}')
            if (lambdaEnd == -1) {
                continue
            }

            val lambdaContent = content.substring(lambdaStart + 1, lambdaEnd)
            if (gridCellCallPattern.containsMatchIn(stripCommentsAndStrings(lambdaContent))) {
                return lambdaContent
            }
        }
        fail("Could not find HexagonalMapView trailing lambda containing GridCell in GameMap.kt")
    }

    private fun findGameMapFile(): File {
        val composeAppProjectDir =
            System.getProperty("composeApp.projectDir")
                ?: fail("Missing required system property composeApp.projectDir")
        val gameMapFile = File(composeAppProjectDir, gameMapModuleRelativePath)
        if (gameMapFile.exists()) {
            return gameMapFile
        }
        fail("Could not locate GameMap.kt at ${gameMapFile.absolutePath}")
    }

    private fun lineNumberAt(source: String, index: Int): Int = source.substring(0, index).count { it == '\n' } + 1

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
