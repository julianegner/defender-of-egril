package de.egril.defender.ui

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail
import java.io.File

/**
 * Test to ensure all user-facing strings are properly translated.
 * 
 * This test scans the UI code for hardcoded strings and verifies that:
 * 1. No hardcoded user-facing strings exist in UI components
 * 2. All strings use stringResource() for localization
 * 
 * Exceptions:
 * - Cheat codes should NOT be translated
 * - Single-character symbols (•, ✓, ➕, X, +, -, 📍, ✗) are acceptable
 * - Variable interpolations ($variable) are acceptable
 * - stringResource() calls are acceptable
 */
class TranslationCoverageTest {
    
    private val projectRoot = File(System.getProperty("user.dir"))
    private val uiSourcePath = File(projectRoot, "composeApp/src/commonMain/kotlin/de/egril/defender/ui")
    
    // Patterns to detect hardcoded strings
    private val hardcodedStringPattern = Regex("""Text\s*\(\s*"([^"]+)"""")
    
    // Patterns for acceptable strings (symbols, single chars, etc.)
    private val symbolPatterns = listOf(
        "•", "✓", "➕", "X", "+", "-", "📍", "✗", "• "
    )
    
    // Patterns for strings that are part of cheat code system
    private val cheatCodeFiles = listOf(
        "CheatCodeDialog.kt"
    )
    
    @Test
    fun testNoHardcodedStringsInUI() {
        val violations = mutableListOf<String>()
        
        if (!uiSourcePath.exists()) {
            fail("UI source path not found: ${uiSourcePath.absolutePath}")
        }
        
        // Scan all Kotlin files in UI directory
        uiSourcePath.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                scanFileForHardcodedStrings(file, violations)
            }
        
        if (violations.isNotEmpty()) {
            val message = buildString {
                appendLine("Found ${violations.size} hardcoded string(s) that should be translated:")
                appendLine()
                violations.forEach { violation ->
                    appendLine(violation)
                }
                appendLine()
                appendLine("All user-facing strings should use stringResource(Res.string.key_name)")
            }
            fail(message)
        }
    }
    
    private fun scanFileForHardcodedStrings(file: File, violations: MutableList<String>) {
        val content = file.readText()
        val relativePath = file.relativeTo(projectRoot).path
        
        // Skip cheat code files
        if (cheatCodeFiles.any { relativePath.contains(it) }) {
            return
        }
        
        val lines = content.lines()
        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            val matches = hardcodedStringPattern.findAll(line)
            
            matches.forEach { match ->
                val stringValue = match.groupValues[1]
                
                // Skip if it's a symbol
                if (symbolPatterns.contains(stringValue)) {
                    return@forEach
                }
                
                // Skip if line contains stringResource (already translated)
                if (line.contains("stringResource")) {
                    return@forEach
                }
                
                // Skip if it's a variable interpolation
                if (stringValue.startsWith("$")) {
                    return@forEach
                }
                
                // Skip if it contains only numbers and basic formatting
                if (stringValue.matches(Regex("""^[\d\s%().,:|/\-]+$"""))) {
                    return@forEach
                }
                
                // This is a violation - hardcoded user-facing string
                violations.add("  $relativePath:$lineNumber - \"$stringValue\"")
            }
        }
    }
    
    @Test
    fun testAllLanguageFilesHaveSameKeys() {
        val resourcesPath = File(projectRoot, "composeApp/src/commonMain/composeResources")
        
        if (!resourcesPath.exists()) {
            fail("Resources path not found: ${resourcesPath.absolutePath}")
        }
        
        // Find all strings.xml files
        val stringFiles = mutableMapOf<String, File>()
        listOf("values", "values-de", "values-es", "values-fr", "values-it").forEach { dir ->
            val file = File(resourcesPath, "$dir/strings.xml")
            if (file.exists()) {
                stringFiles[dir] = file
            }
        }
        
        assertTrue(stringFiles.containsKey("values"), "English strings.xml (values/) should exist")
        
        // Extract keys from each file
        val keysByLanguage = stringFiles.mapValues { (_, file) ->
            extractStringKeys(file)
        }
        
        val englishKeys = keysByLanguage["values"] ?: emptySet()
        val violations = mutableListOf<String>()
        
        // Check each language has all English keys
        keysByLanguage.forEach { (language, keys) ->
            if (language != "values") {
                val missingKeys = englishKeys - keys
                val extraKeys = keys - englishKeys
                
                if (missingKeys.isNotEmpty()) {
                    violations.add("  $language/ is missing keys: ${missingKeys.sorted().joinToString(", ")}")
                }
                if (extraKeys.isNotEmpty()) {
                    violations.add("  $language/ has extra keys not in English: ${extraKeys.sorted().joinToString(", ")}")
                }
            }
        }
        
        if (violations.isNotEmpty()) {
            val message = buildString {
                appendLine("Translation files are not synchronized:")
                appendLine()
                violations.forEach { violation ->
                    appendLine(violation)
                }
            }
            fail(message)
        }
    }
    
    private fun extractStringKeys(file: File): Set<String> {
        val keys = mutableSetOf<String>()
        val stringPattern = Regex("""<string\s+name="([^"]+)"""")
        
        file.readLines().forEach { line ->
            stringPattern.find(line)?.let { match ->
                keys.add(match.groupValues[1])
            }
        }
        
        return keys
    }
}
