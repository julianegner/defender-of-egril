package de.egril.defender.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * Validation test to ensure accessibility-relevant composables provide a content description.
 *
 * Scans UI Kotlin source files for `Image(`, `Icon(` and `IconButton(` calls and verifies that
 * the corresponding call expression contains `contentDescription` (including explicit `null`) or
 * the project's a11y helper (`a11ySemantics`).
 * The scanned UI path can be overridden with system property `a11y.ui.path`.
 * False positives can be suppressed per-line with `// a11y:ignore-content-description`.
 *
 * Note: This is intentionally a lightweight static validation based on text search in extracted
 * call expressions. It may produce rare false positives/negatives in unusual edge cases (for
 * example comments or string literals containing these tokens), but provides fast and useful CI
 * coverage for common UI code paths.
 */
class ContentDescriptionCoverageTest {
    private val projectRoot: File =
        run {
            val currentDir = File(System.getProperty("user.dir"))
            if (currentDir.name == "composeApp") currentDir.parentFile else currentDir
        }

    private val targetCalls = Regex("""\b(Image|Icon|IconButton)\s*\(""")

    // Call extraction below accounts for strings/comments while balancing parentheses.
    private val contentDescriptionToken = Regex("""\bcontentDescription\b""")
    private val a11ySemanticsToken = Regex("""\ba11ySemantics\s*\(""")
    private val ignoreMarker = "a11y:ignore-content-description"

    @Test
    fun testContentDescriptionCoverageInUiSources() {
        val violations = mutableListOf<String>()
        val configuredUiPath = System.getProperty("a11y.ui.path")
        val uiPath =
            configuredUiPath?.let { candidate ->
                val file = File(candidate)
                if (file.isAbsolute) file else File(projectRoot, candidate)
            } ?: File(projectRoot, "composeApp/src/commonMain/kotlin/de/egril/defender/ui")

        if (!uiPath.exists()) {
            fail("UI source path not found: ${uiPath.absolutePath}")
        }

        uiPath
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file -> scanFile(file, violations) }

        if (violations.isNotEmpty()) {
            val message =
                buildString {
                    appendLine("Found ${violations.size} UI call(s) without contentDescription:")
                    appendLine()
                    appendLine("Every Image/Icon/IconButton should declare a contentDescription")
                    appendLine("(or explicit contentDescription = null for decorative content).")
                    appendLine("For buttons, place the description on the IconButton modifier")
                    appendLine("(or use a11ySemantics), not only on nested icon content.")
                    appendLine()
                    violations.forEach { appendLine(it) }
                }
            fail(message)
        }
    }

    private fun scanFile(
        file: File,
        violations: MutableList<String>,
    ) {
        val content = file.readText()
        targetCalls.findAll(content).forEach { match ->
            val callStart = match.range.first
            val openParenIndex = content.indexOf('(', callStart)
            if (openParenIndex < 0) return@forEach

            val callText = extractCallText(content, callStart, openParenIndex) ?: return@forEach
            val lineNumber = content.substring(0, callStart).count { it == '\n' } + 1
            val lineText = content.lineSequence().elementAtOrNull(lineNumber - 1).orEmpty()
            if (lineText.contains(ignoreMarker)) {
                return@forEach
            }

            if (!contentDescriptionToken.containsMatchIn(callText) && !a11ySemanticsToken.containsMatchIn(callText)) {
                val relativePath = file.relativeTo(projectRoot).path
                val callName = match.groupValues[1]
                violations.add("  $relativePath:$lineNumber - $callName(...) missing contentDescription")
            }
        }
    }

    /**
     * Extracts a full call expression by balancing parentheses from [openParenIndex].
     *
     * @return The call substring from [callStart] to the matching closing `)`, or `null` if
     * parentheses are unbalanced and no matching closing parenthesis is found.
     */
    private fun extractCallText(
        content: String,
        callStart: Int,
        openParenIndex: Int,
    ): String? {
        var parenDepth = 0
        var index = openParenIndex
        var stringQuote: Char? = null
        var escaped = false
        var inLineComment = false
        var inBlockComment = false

        while (index < content.length) {
            val current = content[index]
            val next = content.getOrNull(index + 1)

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
                    continue
                }
                index++
                continue
            }

            if (stringQuote != null) {
                if (escaped) {
                    escaped = false
                    index++
                    continue
                }
                if (current == '\\') {
                    escaped = true
                    index++
                    continue
                }
                if (current == stringQuote) {
                    stringQuote = null
                }
                index++
                continue
            }

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

            if (current == '"' || current == '\'') {
                stringQuote = current
                index++
                continue
            }

            when (current) {
                '(' -> parenDepth++
                ')' -> {
                    parenDepth--
                    if (parenDepth == 0) {
                        return content.substring(callStart, index + 1)
                    }
                }
            }
            index++
        }
        return null
    }
}
