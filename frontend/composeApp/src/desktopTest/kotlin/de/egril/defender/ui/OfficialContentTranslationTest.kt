package de.egril.defender.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * Ensures every official level and map has its displayed text translated in all supported languages.
 *
 * Official levels and maps store their texts as string keys (titleKey, subtitleKey, nameKey).
 * This test verifies that:
 * - every non-blank title/subtitle/name has a key to localize it
 * - every referenced key (the part before an optional ":param" suffix) exists in all language files
 * - every referenced key has a non-blank value in all language files
 */
class OfficialContentTranslationTest {
    private val languageFiles = listOf(
        "values/strings.xml",
        "values-de/strings.xml",
        "values-es/strings.xml",
        "values-fr/strings.xml",
        "values-it/strings.xml",
    )

    private val composeResourcesDir: File = run {
        val currentDir = File(System.getProperty("user.dir"))
        val projectRoot = if (currentDir.name == "composeApp") currentDir.parentFile else currentDir
        File(projectRoot, "composeApp/src/commonMain/composeResources")
    }

    private val repositoryDir: File = File(composeResourcesDir, "files/repository")

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testOfficialLevelsHaveTranslatedTitlesAndSubtitles() {
        val levelFiles = File(repositoryDir, "levels").listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }
        check(levelFiles.isNotEmpty()) { "No official level files found in ${repositoryDir.path}/levels" }

        val problems = mutableListOf<String>()
        levelFiles.forEach { file ->
            val data = dataOf(file)
            checkText(file, "title", data, "titleKey", problems)
            checkText(file, "subtitle", data, "subtitleKey", problems)
        }
        reportIfAny(problems)
    }

    @Test
    fun testOfficialMapsHaveTranslatedNames() {
        val mapFiles = File(repositoryDir, "maps").listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }
        check(mapFiles.isNotEmpty()) { "No official map files found in ${repositoryDir.path}/maps" }

        val problems = mutableListOf<String>()
        mapFiles.forEach { file ->
            val data = dataOf(file)
            checkText(file, "name", data, "nameKey", problems)
        }
        reportIfAny(problems)
    }

    private fun dataOf(file: File): JsonObject {
        val root: JsonElement = json.parseToJsonElement(file.readText())
        return root.jsonObject["data"]?.jsonObject ?: root.jsonObject
    }

    private fun checkText(
        file: File,
        textField: String,
        data: JsonObject,
        keyField: String,
        problems: MutableList<String>,
    ) {
        val text = data[textField]?.jsonPrimitive?.content.orEmpty()
        val key = data[keyField]?.jsonPrimitive?.content
        val fileName = "${file.parentFile.name}/${file.name}"

        if (text.isBlank()) return
        if (key.isNullOrBlank()) {
            problems.add("$fileName: '$textField' is \"$text\" but has no '$keyField'")
            return
        }

        val baseKey = key.substringBefore(':')
        languageFiles.forEach { languageFile ->
            val values = stringValues(languageFile)
            val value = values[baseKey]
            when {
                value == null -> problems.add("$fileName: key '$baseKey' is missing in $languageFile")
                value.isBlank() -> problems.add("$fileName: key '$baseKey' is empty in $languageFile")
            }
        }
    }

    private val stringCache = mutableMapOf<String, Map<String, String>>()

    private fun stringValues(relativePath: String): Map<String, String> =
        stringCache.getOrPut(relativePath) {
            val xml = File(composeResourcesDir, relativePath).readText()
            Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(xml)
                .associate { it.groupValues[1] to it.groupValues[2] }
        }

    private fun reportIfAny(problems: List<String>) {
        if (problems.isNotEmpty()) {
            fail(
                "Official content has untranslated or missing texts (${problems.size} problem(s)):\n" +
                    problems.joinToString("\n") { "  - $it" },
            )
        }
    }
}
