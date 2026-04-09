package de.egril.defender.ui

import kotlin.test.Test
import kotlin.test.fail
import java.io.File

/**
 * Test to ensure SelectableText is never used inside Button composables.
 *
 * Background: SelectableText wraps content in a SelectionContainer, which conflicts with
 * button click handling and produces unexpected behavior. Buttons should always use
 * plain Text() for their labels.
 *
 * This test scans all Kotlin source files and fails if SelectableText( is found inside
 * any of the following composables: Button, TextButton, OutlinedButton, ElevatedButton,
 * FilledTonalButton, IconButton, AssistChip, FilterChip, InputChip, SuggestionChip,
 * DropdownMenuItem, NavigationBarItem, NavigationRailItem, NavigationDrawerItem.
 *
 * How to fix a violation:
 *   Replace SelectableText(...) with Text(...) inside the button lambda.
 */
class SelectableTextInButtonValidationTest {

    private val projectRoot: File = run {
        val currentDir = File(System.getProperty("user.dir"))
        if (currentDir.name == "composeApp") currentDir.parentFile else currentDir
    }

    private val buttonTypes = listOf(
        "Button", "TextButton", "OutlinedButton", "ElevatedButton",
        "FilledTonalButton", "IconButton",
        "AssistChip", "ElevatedAssistChip", "FilterChip", "ElevatedFilterChip",
        "InputChip", "SuggestionChip", "ElevatedSuggestionChip",
        "DropdownMenuItem", "NavigationBarItem", "NavigationRailItem",
        "NavigationDrawerItem",
    )

    @Test
    fun testNoSelectableTextInsideButtons() {
        val violations = mutableListOf<String>()
        val sourcePath = File(projectRoot, "composeApp/src")

        if (!sourcePath.exists()) {
            fail("Source path not found: ${sourcePath.absolutePath}")
        }

        sourcePath.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file -> scanFile(file, violations) }

        if (violations.isNotEmpty()) {
            val message = buildString {
                appendLine("Found ${violations.size} SelectableText usage(s) inside Button composable(s):")
                appendLine()
                appendLine("SelectableText must not be used inside Button composables.")
                appendLine("Use plain Text() instead, as SelectableText wraps content in a")
                appendLine("SelectionContainer which conflicts with button click handling.")
                appendLine()
                violations.forEach { appendLine(it) }
                appendLine()
                appendLine("How to fix: replace SelectableText(...) with Text(...) inside the button lambda.")
            }
            fail(message)
        }
    }

    private fun scanFile(file: File, violations: MutableList<String>) {
        val content = file.readText()
        if ("SelectableText" !in content) return

        val buttonPattern = Regex("""\b(?:${buttonTypes.joinToString("|")})\s*\(""")
        val lines = content.lines()
        // Stack entries: brace depth at the time the button's opening brace was pushed
        val contextStack = ArrayDeque<Pair<String, Int>>()
        var braceDepth = 0

        lines.forEachIndexed { index, line ->
            val openBraces = line.count { it == '{' }
            val closeBraces = line.count { it == '}' }

            // Check if a button composable opens on this line
            val match = buttonPattern.find(line)
            if (match != null) {
                contextStack.addLast(Pair(match.value.trim(), braceDepth + openBraces - closeBraces))
            }

            braceDepth += openBraces - closeBraces

            // Remove any contexts whose scope has closed
            while (contextStack.isNotEmpty() && contextStack.last().second > braceDepth) {
                contextStack.removeLast()
            }

            // Report violation if SelectableText appears while inside a button
            if ("SelectableText(" in line && contextStack.isNotEmpty()) {
                val relativePath = file.relativeTo(projectRoot).path
                violations.add("  $relativePath:${index + 1} (inside ${contextStack.last().first})")
            }
        }
    }
}
