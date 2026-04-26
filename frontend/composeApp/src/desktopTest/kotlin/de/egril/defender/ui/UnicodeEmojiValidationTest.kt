package de.egril.defender.ui

import kotlin.test.Test
import kotlin.test.fail
import java.io.File

/**
 * Test to ensure no Unicode emojis or icons are present in user-facing code or language files.
 * 
 * Background: Unicode emojis do not display correctly on WASM platform.
 * Solution: Use PNG image files instead (already available in the project).
 * 
 * This test verifies that:
 * 1. No Unicode emojis/icons exist in Kotlin source files (except in comments)
 * 2. No Unicode emojis/icons exist in language XML files
 * 
 * Exceptions:
 * - Emojis in comments are allowed (// or /* */)
 * - Emojis in .md documentation files are allowed
 * - PNG emoji images (emoji_*.png) are the correct way to display emojis
 * 
 * If a new emoji is needed:
 * 1. Look it up at https://api.github.com/repos/googlefonts/noto-emoji
 * 2. Download the PNG version
 * 3. Integrate it as emoji_*.png in composeResources/drawable/
 */
class UnicodeEmojiValidationTest {
    
    private val projectRoot: File = run {
        // During tests, user.dir is usually the composeApp directory, so we need to go up one level if needed
        val currentDir = File(System.getProperty("user.dir"))
        if (currentDir.name == "composeApp") {
            currentDir.parentFile
        } else {
            currentDir
        }
    }
    
    // Comprehensive Unicode emoji ranges covering most emoji characters
    // Reference: https://unicode.org/emoji/charts/full-emoji-list.html
    // Also includes arrow/symbol characters that do not render correctly on WASM
    private val emojiPattern = Regex(
        "[" +
        // Emoticons
        "\u263a-\u263b" +
        // Dingbats
        "\u2702-\u27b0" +
        // Miscellaneous Symbols
        "\u2600-\u26ff" +
        // Arrows block (U+2190–U+21FF) - arrows do not render correctly on WASM
        "\u2190-\u21ff" +
        // Geometric Shapes (U+25A0–U+25FF) - do not render correctly on WASM
        "\u25a0-\u25ff" +
        // Miscellaneous Symbols and Arrows
        "\u2b05-\u2b07\u2b1b-\u2b1c\u2b50\u2b55" +
        // Supplemental Symbols and Pictographs
        "\ud83c[\udf00-\udfff]" +
        "\ud83d[\udc00-\ude4f]" +
        "\ud83d[\ude80-\udeff]" +
        "\ud83e[\udd00-\uddff]" +
        // Enclosed Alphanumeric Supplement
        "\ud83c[\udde6-\uddff][\udde6-\uddff]" +
        // Miscellaneous Technical
        "\u2300-\u23ff" +
        "]"
    )
    
    @Test
    fun testNoUnicodeEmojisInKotlinSourceFiles() {
        val violations = mutableListOf<String>()
        val kotlinSourcePath = File(projectRoot, "composeApp/src")
        
        if (!kotlinSourcePath.exists()) {
            fail("Kotlin source path not found: ${kotlinSourcePath.absolutePath}")
        }
        
        kotlinSourcePath.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                scanKotlinFileForEmojis(file, violations)
            }
        
        if (violations.isNotEmpty()) {
            val message = buildString {
                appendLine("Found ${violations.size} Unicode emoji(s)/icon(s) in Kotlin source files:")
                appendLine()
                appendLine("Unicode emojis do not display correctly on WASM platform.")
                appendLine("Please use PNG image files instead (emoji_*.png in composeResources/drawable/).")
                appendLine()
                violations.forEach { violation ->
                    appendLine(violation)
                }
                appendLine()
                appendLine("To add a new emoji:")
                appendLine("  1. Look it up at https://api.github.com/repos/googlefonts/noto-emoji")
                appendLine("  2. Download the PNG version")
                appendLine("  3. Add it as emoji_*.png in composeResources/drawable/")
                appendLine("  4. Use an Icon component instead of Unicode character")
            }
            fail(message)
        }
    }
    
    @Test
    fun testNoUnicodeEmojisInLanguageFiles() {
        val violations = mutableListOf<String>()
        val resourcesPath = File(projectRoot, "composeApp/src/commonMain/composeResources")
        
        if (!resourcesPath.exists()) {
            fail("Resources path not found: ${resourcesPath.absolutePath}")
        }
        
        // Check all language directories
        listOf("values", "values-de", "values-es", "values-fr", "values-it").forEach { dir ->
            val stringsFile = File(resourcesPath, "$dir/strings.xml")
            if (stringsFile.exists()) {
                scanXmlFileForEmojis(stringsFile, dir, violations)
            }
        }
        
        if (violations.isNotEmpty()) {
            val message = buildString {
                appendLine("Found ${violations.size} Unicode emoji(s)/icon(s) in language XML files:")
                appendLine()
                appendLine("Unicode emojis do not display correctly on WASM platform.")
                appendLine("Please use Icon components instead of Unicode characters in strings.")
                appendLine()
                violations.forEach { violation ->
                    appendLine(violation)
                }
                appendLine()
                appendLine("How to fix:")
                appendLine("  1. Remove the emoji from the string resource")
                appendLine("  2. Use a Row with Icon component + Text in the UI code")
                appendLine("  3. See 'Icons and Emojis' section in .github/copilot-instructions.md")
            }
            fail(message)
        }
    }
    
    private fun scanKotlinFileForEmojis(file: File, violations: MutableList<String>) {
        val content = file.readText()
        val relativePath = file.relativeTo(projectRoot).path
        val lines = content.lines()
        
        var inMultilineComment = false
        
        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            var processedLine = line
            
            // Handle multi-line comments
            if (processedLine.contains("/*")) {
                inMultilineComment = true
            }
            if (inMultilineComment) {
                if (processedLine.contains("*/")) {
                    inMultilineComment = false
                }
                return@forEachIndexed // Skip lines in multi-line comments
            }
            
            // Skip single-line comments
            val commentIndex = processedLine.indexOf("//")
            if (commentIndex != -1) {
                processedLine = processedLine.substring(0, commentIndex)
            }
            
            // Check for emojis in the processed line (excluding comments)
            val matches = emojiPattern.findAll(processedLine)
            matches.forEach { match ->
                val emoji = match.value
                val context = getContextAroundMatch(processedLine, match.range.first, 40)
                violations.add("  $relativePath:$lineNumber - '$emoji' in: $context")
            }
        }
    }
    
    private fun scanXmlFileForEmojis(file: File, languageDir: String, violations: MutableList<String>) {
        val content = file.readText()
        val lines = content.lines()
        
        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            
            // Skip XML comments
            if (line.trim().startsWith("<!--")) {
                return@forEachIndexed
            }
            
            // Check for emojis
            val matches = emojiPattern.findAll(line)
            matches.forEach { match ->
                val emoji = match.value
                val context = getContextAroundMatch(line, match.range.first, 60)
                violations.add("  $languageDir/strings.xml:$lineNumber - '$emoji' in: $context")
            }
        }
    }
    
    private fun getContextAroundMatch(text: String, position: Int, contextLength: Int): String {
        val start = maxOf(0, position - contextLength / 2)
        val end = minOf(text.length, position + contextLength / 2)
        var context = text.substring(start, end).trim()
        if (start > 0) context = "...$context"
        if (end < text.length) context = "$context..."
        return context
    }
}
