package de.egril.defender.mapgen

import de.egril.defender.editor.EditorJsonSerializer
import java.io.File

/**
 * Standalone Kotlin JVM program to regenerate all map PNG images from map JSON files.
 *
 * Usage: run via Gradle task `generateMapImages`, or call generateAllMapImages() directly.
 *
 * Reads map JSON files from the given directory, generates a PNG using
 * [MapImageGenerator] and [MapImageEncoder], and writes the PNG alongside the JSON.
 */
object GenerateMapImages {

    /**
     * Generate PNG images for all map JSON files in [mapsDir].
     * Skips maps whose PNG is already up-to-date (same mtime or newer).
     *
     * @param mapsDir Directory containing map_*.json files.
     * @param forceRegenerate If true, regenerate even when PNG already exists.
     */
    fun generateAll(mapsDir: File, forceRegenerate: Boolean = false) {
        require(mapsDir.isDirectory) { "Maps directory not found: ${mapsDir.absolutePath}" }

        val jsonFiles = mapsDir.listFiles { f -> f.extension == "json" && f.name.startsWith("map_") }
            ?: emptyArray()

        if (jsonFiles.isEmpty()) {
            println("GenerateMapImages: No map JSON files found in ${mapsDir.absolutePath}")
            return
        }

        println("GenerateMapImages: Processing ${jsonFiles.size} map(s) in ${mapsDir.absolutePath}")

        var generated = 0
        var skipped = 0
        var failed = 0

        for (jsonFile in jsonFiles.sortedBy { it.name }) {
            val pngFile = File(jsonFile.parentFile, jsonFile.nameWithoutExtension + ".png")

            if (!forceRegenerate && pngFile.exists() && pngFile.lastModified() >= jsonFile.lastModified()) {
                println("  Skipping (up-to-date): ${pngFile.name}")
                skipped++
                continue
            }

            val success = generateOne(jsonFile, pngFile)
            if (success) generated++ else failed++
        }

        println("GenerateMapImages: Done. Generated=$generated, Skipped=$skipped, Failed=$failed")
    }

    /**
     * Generate a single map PNG from [jsonFile] and write it to [pngFile].
     * @return true on success, false on failure.
     */
    fun generateOne(jsonFile: File, pngFile: File): Boolean {
        return try {
            val json = jsonFile.readText()
            val map = EditorJsonSerializer.deserializeMap(json)
            if (map == null) {
                println("  ERROR: Could not parse map JSON: ${jsonFile.name}")
                return false
            }

            print("  Generating ${pngFile.name} (${map.width}x${map.height})...")

            val (pixels, width, height) = MapImageGenerator.generatePixels(map)
            val pngBytes = MapImageEncoder.encodeToPng(pixels, width, height)

            if (pngBytes == null) {
                println(" FAILED (encoder returned null)")
                return false
            }

            pngFile.writeBytes(pngBytes)
            println(" OK (${pngBytes.size / 1024}KB, ${width}x${height}px)")
            true
        } catch (e: Exception) {
            println(" FAILED: ${e.message}")
            false
        }
    }
}

/**
 * Entry point when run as a standalone JVM program.
 *
 * Arguments:
 *   [0] = path to the maps directory (optional, defaults to resources maps directory)
 *   [1] = "--force" to regenerate all images even if already up-to-date (optional)
 */
fun main(args: Array<String>) {
    val mapsDir = if (args.isNotEmpty() && args[0] != "--force") {
        File(args[0])
    } else {
        // Default: relative to project root
        val projectRoot = File(System.getProperty("user.dir"))
        File(projectRoot, "composeApp/src/commonMain/composeResources/files/repository/maps")
    }

    val force = args.any { it == "--force" }

    GenerateMapImages.generateAll(mapsDir, forceRegenerate = force)
}
