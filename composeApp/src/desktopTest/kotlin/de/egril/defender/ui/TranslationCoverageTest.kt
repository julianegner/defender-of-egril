package de.egril.defender.ui

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail
import java.io.File

/**
 * Comprehensive test suite to ensure all user-facing strings are properly translated
 * and to prevent "???" from appearing in the app due to missing or broken translations.
 * 
 * This test suite performs the following checks:
 * 
 * 1. **testNoHardcodedStringsInUI**: Scans UI code for hardcoded strings
 *    - Ensures all strings use stringResource() for localization
 *    - Exceptions: cheat codes, single-character symbols, variable interpolations
 * 
 * 2. **testAllLanguageFilesHaveSameKeys**: Verifies key synchronization across languages
 *    - Checks that all language files (de, es, fr, it) have the same keys as English
 *    - Detects missing keys that would cause "???" to appear
 *    - Detects extra keys that shouldn't exist
 * 
 * 3. **testNoEmptyOrWhitespaceOnlyTranslations**: Detects empty translation values
 *    - Checks all language files for empty string values
 *    - Empty translations display as "???" in the app
 *    - Provides exact file location and key name for easy fixing
 * 
 * 4. **testAllReferencedKeysExist**: Validates code references to string keys
 *    - Scans ALL UI files for stringResource(Res.string.xxx) calls
 *    - Also checks LocalizationUtils, NameLocalizationUtils, and AchievementLocalization
 *    - Ensures all referenced keys are defined in strings.xml
 *    - Missing key definitions cause "???" to appear at runtime
 * 
 * 5. **testXmlFilesAreWellFormed**: Validates XML structure
 *    - Checks for XML declaration and root element
 *    - Verifies matching open/close tags
 *    - Detects duplicate keys within a language file
 *    - Malformed XML prevents translations from loading correctly
 * 
 * 6. **testParameterizedStringsMatchAcrossLanguages**: Validates parameter consistency
 *    - Checks that parameterized strings use the same placeholders across all languages
 *    - Detects mismatches like %s in English but %d in German
 *    - Parameter mismatches can cause "???" or incorrect formatting at runtime
 * 
 * When a test fails, it provides:
 * - Clear error message explaining the issue
 * - Exact file location and line numbers
 * - Specific keys or strings that are problematic
 * - Guidance on how to fix the issue
 */
class TranslationCoverageTest {
    
    private val projectRoot: File = run {
        // During tests, user.dir is usually the composeApp directory, so we need to go up one level if needed
        val currentDir = File(System.getProperty("user.dir"))
        if (currentDir.name == "composeApp") {
            currentDir.parentFile
        } else {
            currentDir
        }
    }
    private val uiSourcePath = File(projectRoot, "composeApp/src/commonMain/kotlin/de/egril/defender/ui")
    
    // Patterns to detect hardcoded strings
    private val hardcodedStringPattern = Regex("""Text\s*\(\s*"([^"]+)"""")
    
    // Patterns for acceptable strings (symbols, single chars, etc.)
    private val symbolPatterns = listOf(
        "•", "X", "+", "-", "• "
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
                
                // Skip if it's a URL or domain name (contains .com, .de, .org, etc.)
                if (stringValue.matches(Regex(""".*\.(com|de|org|net|io|co|uk)(/.*)?$"""))) {
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
        println("DEBUG: projectRoot = ${projectRoot.absolutePath}")
        println("DEBUG: resourcesPath = ${resourcesPath.absolutePath}")
        println("DEBUG: resourcesPath.exists() = ${resourcesPath.exists()}")
        
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
    
    @Test
    fun testNoEmptyOrWhitespaceOnlyTranslations() {
        val resourcesPath = File(projectRoot, "composeApp/src/commonMain/composeResources")
        
        if (!resourcesPath.exists()) {
            fail("Resources path not found: ${resourcesPath.absolutePath}")
        }
        
        val violations = mutableListOf<String>()
        
        // Check all language files for empty translations
        listOf("values", "values-de", "values-es", "values-fr", "values-it").forEach { dir ->
            val file = File(resourcesPath, "$dir/strings.xml")
            if (file.exists()) {
                checkFileForEmptyTranslations(file, dir, violations)
            }
        }
        
        if (violations.isNotEmpty()) {
            val message = buildString {
                appendLine("Found ${violations.size} empty or whitespace-only translation(s):")
                appendLine("Empty translations will display as '???' in the app.")
                appendLine()
                violations.forEach { violation ->
                    appendLine(violation)
                }
                appendLine()
                appendLine("All translations must have non-empty values.")
            }
            fail(message)
        }
    }
    
    private fun checkFileForEmptyTranslations(file: File, languageDir: String, violations: MutableList<String>) {
        val emptyStringPattern = Regex("""<string\s+name="([^"]+)"\s*>\s*</string>""")
        
        file.readLines().forEachIndexed { index, line ->
            val lineNumber = index + 1
            emptyStringPattern.find(line)?.let { match ->
                val keyName = match.groupValues[1]
                violations.add("  $languageDir/strings.xml:$lineNumber - Key '$keyName' has empty value")
            }
        }
    }
    
    @Test
    fun testAllReferencedKeysExist() {
        val resourcesPath = File(projectRoot, "composeApp/src/commonMain/composeResources")
        val uiSourcePath = File(projectRoot, "composeApp/src/commonMain/kotlin/de/egril/defender/ui")
        
        if (!resourcesPath.exists()) {
            fail("Resources path not found: ${resourcesPath.absolutePath}")
        }
        
        if (!uiSourcePath.exists()) {
            fail("UI source path not found: ${uiSourcePath.absolutePath}")
        }
        
        // Get all defined keys from English strings.xml
        val englishFile = File(resourcesPath, "values/strings.xml")
        val definedKeys = extractStringKeys(englishFile)
        
        // Find all keys referenced in ALL UI files via stringResource() calls
        val referencedKeys = mutableSetOf<String>()
        
        // Scan all .kt files in the UI directory
        uiSourcePath.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                extractStringResourceKeys(file, referencedKeys)
            }
        
        // Also check localization utility files for keys in when statements
        listOf("LocalizationUtils.kt", "NameLocalizationUtils.kt", "AchievementLocalization.kt").forEach { fileName ->
            val utilFile = File(uiSourcePath, fileName)
            if (utilFile.exists()) {
                extractReferencedKeys(utilFile, referencedKeys)
            }
        }
        
        // Find missing keys
        val missingKeys = referencedKeys - definedKeys
        
        if (missingKeys.isNotEmpty()) {
            val message = buildString {
                appendLine("Found ${missingKeys.size} key(s) referenced in code but not defined in strings.xml:")
                appendLine("These will display as '???' in the app.")
                appendLine()
                missingKeys.sorted().forEach { key ->
                    appendLine("  - $key")
                }
                appendLine()
                appendLine("All referenced keys must be defined in values/strings.xml")
            }
            fail(message)
        }
    }
    
    private fun extractStringResourceKeys(file: File, keys: MutableSet<String>) {
        // Pattern to match stringResource(Res.string.key_name) calls
        val stringResourcePattern = Regex("""stringResource\s*\(\s*Res\.string\.([a-z_][a-z_0-9]*)\b""")
        
        file.readLines().forEach { line ->
            stringResourcePattern.findAll(line).forEach { match ->
                val key = match.groupValues[1]
                keys.add(key)
            }
        }
    }
    
    private fun extractReferencedKeys(file: File, keys: MutableSet<String>) {
        // Pattern to match string literals in when statements or variable assignments
        val keyPattern = Regex("""["']([a-z_][a-z0-9_]*)["']""")
        
        file.readLines().forEach { line ->
            // Skip comments
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("//") || trimmedLine.startsWith("*") || trimmedLine.startsWith("/*")) {
                return@forEach
            }
            
            // Only look at lines that reference string keys (contain quotes and underscore)
            if (line.contains("\"") || line.contains("'")) {
                keyPattern.findAll(line).forEach { match ->
                    val potentialKey = match.groupValues[1]
                    // Only consider it a key if it looks like a string resource key
                    // (lowercase with underscores, not a file path or URL)
                    if (potentialKey.contains("_") && !potentialKey.contains("/") && !potentialKey.contains(".")) {
                        keys.add(potentialKey)
                    }
                }
            }
        }
    }
    
    @Test
    fun testXmlFilesAreWellFormed() {
        val resourcesPath = File(projectRoot, "composeApp/src/commonMain/composeResources")
        
        if (!resourcesPath.exists()) {
            fail("Resources path not found: ${resourcesPath.absolutePath}")
        }
        
        val violations = mutableListOf<String>()
        
        // Check all language files for XML well-formedness
        listOf("values", "values-de", "values-es", "values-fr", "values-it").forEach { dir ->
            val file = File(resourcesPath, "$dir/strings.xml")
            if (file.exists()) {
                try {
                    validateXmlStructure(file, dir, violations)
                } catch (e: Exception) {
                    violations.add("  $dir/strings.xml - XML parsing error: ${e.message}")
                }
            }
        }
        
        if (violations.isNotEmpty()) {
            val message = buildString {
                appendLine("Found ${violations.size} XML structure issue(s):")
                appendLine("Malformed XML will cause translations to fail and display as '???'")
                appendLine()
                violations.forEach { violation ->
                    appendLine(violation)
                }
            }
            fail(message)
        }
    }
    
    private fun validateXmlStructure(file: File, languageDir: String, violations: MutableList<String>) {
        val content = file.readText()
        
        // Check for basic XML structure issues
        if (!content.contains("<?xml")) {
            violations.add("  $languageDir/strings.xml - Missing XML declaration")
        }
        
        if (!content.contains("<resources>") || !content.contains("</resources>")) {
            violations.add("  $languageDir/strings.xml - Missing <resources> root element")
        }
        
        // Check for unclosed tags
        val openTags = Regex("""<string\s+name="[^"]+">""").findAll(content).count()
        val closeTags = Regex("""</string>""").findAll(content).count()
        
        if (openTags != closeTags) {
            violations.add("  $languageDir/strings.xml - Mismatched <string> tags (open: $openTags, close: $closeTags)")
        }
        
        // Check for duplicate keys
        val keys = mutableMapOf<String, Int>()
        val stringPattern = Regex("""<string\s+name="([^"]+)"""")
        
        file.readLines().forEachIndexed { index, line ->
            stringPattern.find(line)?.let { match ->
                val key = match.groupValues[1]
                if (keys.containsKey(key)) {
                    violations.add("  $languageDir/strings.xml:${index + 1} - Duplicate key '$key' (first defined at line ${keys[key]})")
                } else {
                    keys[key] = index + 1
                }
            }
        }
    }
    
    @Test
    fun testParameterizedStringsMatchAcrossLanguages() {
        val resourcesPath = File(projectRoot, "composeApp/src/commonMain/composeResources")
        
        if (!resourcesPath.exists()) {
            fail("Resources path not found: ${resourcesPath.absolutePath}")
        }
        
        val violations = mutableListOf<String>()
        
        // Extract parameterized strings from English
        val englishFile = File(resourcesPath, "values/strings.xml")
        val englishParameters = extractParametersFromStrings(englishFile)
        
        // Check each language file
        listOf("values-de", "values-es", "values-fr", "values-it").forEach { dir ->
            val file = File(resourcesPath, "$dir/strings.xml")
            if (file.exists()) {
                val languageParameters = extractParametersFromStrings(file)
                
                // Check if parameters match for each key
                englishParameters.forEach { (key, englishParams) ->
                    val languageParams = languageParameters[key]
                    if (languageParams != null && englishParams != languageParams) {
                        violations.add("  $dir/strings.xml - Key '$key' has parameter mismatch")
                        violations.add("    English: $englishParams")
                        violations.add("    $dir: $languageParams")
                    }
                }
            }
        }
        
        if (violations.isNotEmpty()) {
            val message = buildString {
                appendLine("Found ${violations.size / 3} parameterized string(s) with mismatched parameters:")
                appendLine("Parameter mismatches can cause '???' or incorrect formatting at runtime.")
                appendLine()
                violations.forEach { violation ->
                    appendLine(violation)
                }
                appendLine()
                appendLine("All languages must use the same parameter placeholders (e.g., %s, %d, %1\$s, %2\$d)")
            }
            fail(message)
        }
    }
    
    private fun extractParametersFromStrings(file: File): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        val stringPattern = Regex("""<string\s+name="([^"]+)"[^>]*>([^<]*)</string>""")
        val parameterPattern = Regex("""%(\d+\$)?[sdif]""")
        
        file.readLines().forEach { line ->
            stringPattern.find(line)?.let { match ->
                val key = match.groupValues[1]
                val value = match.groupValues[2]
                val parameters = parameterPattern.findAll(value).map { it.value }.toList()
                if (parameters.isNotEmpty()) {
                    result[key] = parameters
                }
            }
        }
        
        return result
    }
}
