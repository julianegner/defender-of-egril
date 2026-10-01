package de.egril.defender.ui.editor.level.events

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Verifies that every predefined event message offered in the level editor exists in all supported
 * languages, so the message popup never falls back to a raw key at runtime.
 */
class EventMessageCatalogTranslationTest {
    private val projectRoot: File =
        run {
            val currentDir = File(System.getProperty("user.dir"))
            if (currentDir.name == "composeApp") currentDir.parentFile else currentDir
        }

    private val languageDirs = listOf("values", "values-de", "values-es", "values-fr", "values-it")

    @Test
    fun testAllCatalogKeysAreTranslatedInAllLanguages() {
        val resourcesPath = File(projectRoot, "composeApp/src/commonMain/composeResources")
        if (!resourcesPath.exists()) fail("Resources path not found: ${resourcesPath.absolutePath}")

        val missing = mutableListOf<String>()
        languageDirs.forEach { dir ->
            val file = File(resourcesPath, "$dir/strings.xml")
            if (!file.exists()) fail("Missing strings file: ${file.absolutePath}")
            val content = file.readText()
            EventMessageCatalog.keys.forEach { key ->
                if (!content.contains("<string name=\"$key\">")) {
                    missing.add("$dir/strings.xml: $key")
                }
            }
        }

        assertTrue(missing.isEmpty(), "Untranslated event message keys:\n${missing.joinToString("\n")}")
    }

    @Test
    fun testTidePresetsAreOffered() {
        assertTrue("event_msg_high_tide" in EventMessageCatalog.keys, "High tide preset should be selectable")
        assertTrue("event_msg_low_tide" in EventMessageCatalog.keys, "Low tide preset should be selectable")
    }
}
