package de.egril.defender.editor

import de.egril.defender.config.LogConfig
import de.egril.defender.editor.EditorStorage.MapSaveResult
import de.egril.defender.mapgen.MapImageEncoder
import de.egril.defender.mapgen.TileZoneImageGenerator
import de.egril.defender.ui.MapImageProvider

/**
 * Save the map JSON data and determine whether the map image needs regeneration.
 * Does NOT generate the image — call [generateMapPixels] and [compressAndSaveMapImage]
 * separately so the UI can show each step.
 *
 * @return a [MapSaveResult] telling the caller whether the image needs regeneration
 */
fun EditorStorage.saveMapData(
    map: EditorMap,
    oldId: String? = null,
    storage: FileStorage = fileStorage,
): MapSaveResult {
    val validatedMap = map.copy(readyToUse = if (map.isValid) map.validateReadyToUse() else false)
    val existingMap = mapsCache[validatedMap.id]

    mapsCache[validatedMap.id] = validatedMap
    val json = EditorJsonSerializer.serializeMap(validatedMap)

    val targetDir = if (validatedMap.isOfficial) OFFICIAL_MAPS_DIR else USER_MAPS_DIR
    storage.writeFile("$targetDir/${validatedMap.id}.json", json)

    val pngPath = "$targetDir/${validatedMap.id}.png"
    val pngExists = storage.fileExists(pngPath)
    val tilesChanged =
        existingMap == null ||
            existingMap.width != validatedMap.width ||
            existingMap.height != validatedMap.height ||
            normalizeForImageComparison(existingMap) != normalizeForImageComparison(validatedMap)
    val imageNeedsRegeneration =
        validatedMap.canRenderMinimap() &&
            (!pngExists || tilesChanged || validatedMap.id in pendingMapImages)
    if (imageNeedsRegeneration && pngExists) pendingMapImages.add(validatedMap.id)
    val zoneIndicesToRegenerate =
        if (validatedMap.canRenderMinimap()) {
            validatedMap.tileZones.indices.filter { index ->
                tilesChanged ||
                    index in pendingZoneImages[validatedMap.id].orEmpty() ||
                    existingMap.tileZones.getOrNull(index)?.tiles != validatedMap.tileZones[index].tiles ||
                    !storage.fileExists("$targetDir/${MapImageProvider.tileZoneImageFileName(validatedMap.id, index)}")
            }
        } else {
            emptyList()
        }
    pendingZoneImages[validatedMap.id] = zoneIndicesToRegenerate.toSet()
    removeStaleTileZoneImages(targetDir, validatedMap.id, validatedMap.tileZones.size, storage)

    if (!imageNeedsRegeneration) {
        println("Skipping map image regeneration for ${validatedMap.id} (no tile type changes)")
    }

    if (oldId != null && oldId != validatedMap.id) {
        mapsCache.remove(oldId)
        pendingMapImages.remove(oldId)
        pendingZoneImages.remove(oldId)
        storage.deleteFile("$USER_MAPS_DIR/$oldId.json")
        storage.deleteFile("$USER_MAPS_DIR/$oldId.png")
        removeStaleTileZoneImages(USER_MAPS_DIR, oldId, 0, storage)
        println("Deleted old map files for renamed map: $oldId -> ${validatedMap.id}")
    }

    if (validatedMap.isOfficial) {
        OfficialDataChangeTracker.trackMapModified(validatedMap.id)
    }

    return MapSaveResult(imageNeedsRegeneration, validatedMap, zoneIndicesToRegenerate)
}

private fun EditorStorage.removeStaleTileZoneImages(
    directory: String,
    mapId: String,
    count: Int,
    storage: FileStorage = fileStorage,
) {
    val expected = (0 until count).map { MapImageProvider.tileZoneImageFileName(mapId, it) }.toSet()
    storage
        .listFiles(directory)
        .filter { it.startsWith("$mapId.zone-") && it.endsWith(".png") && it !in expected }
        .forEach { storage.deleteFile("$directory/$it") }
}

/** Generate and persist each zone's feathered PNG alongside the map PNG, before gameplay. */
fun EditorStorage.generateAndSaveTileZoneImages(
    map: EditorMap,
    storage: FileStorage = fileStorage,
): Long {
    if (!map.canRenderMinimap()) return 0
    var totalBytes = 0L
    map.tileZones.indices.forEach { index ->
        totalBytes += generateAndSaveTileZoneImage(map, index, storage).toLong()
    }
    val directory =
        if (map.isCommunity) {
            COMMUNITY_MAPS_DIR
        } else if (map.isOfficial) {
            OFFICIAL_MAPS_DIR
        } else {
            USER_MAPS_DIR
        }
    removeStaleTileZoneImages(directory, map.id, map.tileZones.size, storage)
    return totalBytes
}

fun EditorStorage.generateAndSaveTileZoneImage(
    map: EditorMap,
    index: Int,
    storage: FileStorage = fileStorage,
): Int {
    require(map.canRenderMinimap()) { "Map ${map.id} cannot render an image" }
    val (pixels, width, height) = generateTileZonePixels(map, index)
    return compressAndSaveTileZoneImage(map, index, pixels, width, height, storage)
}

fun EditorStorage.generateTileZonePixels(
    map: EditorMap,
    index: Int,
): Triple<IntArray, Int, Int> = TileZoneImageGenerator.generateZonePixels(map, map.tileZones[index])

fun EditorStorage.compressAndSaveTileZoneImage(
    map: EditorMap,
    index: Int,
    pixels: IntArray,
    width: Int,
    height: Int,
    storage: FileStorage = fileStorage,
): Int {
    val directory =
        when {
            map.isCommunity -> COMMUNITY_MAPS_DIR
            map.isOfficial -> OFFICIAL_MAPS_DIR
            else -> USER_MAPS_DIR
        }
    val png =
        MapImageEncoder.encodeToPng(pixels, width, height)
            ?: error("Could not encode tile zone image for ${map.id}, zone ${map.tileZones[index].id}")
    storage.writeBinaryFile("$directory/${MapImageProvider.tileZoneImageFileName(map.id, index)}", png)
    pendingZoneImages[map.id] = pendingZoneImages[map.id].orEmpty() - index
    return png.size
}

/**
 * Generate map image pixel data. This is the CPU-intensive step.
 * @return Triple of (pixels, width, height)
 */
fun EditorStorage.generateMapPixels(map: EditorMap): Triple<IntArray, Int, Int> =
    de.egril.defender.mapgen.MapImageGenerator
        .generatePixels(map)

/**
 * Compress pixel data to PNG and save to disk.
 * @return the compressed PNG size in bytes, or -1 on failure
 */
fun EditorStorage.compressAndSaveMapImage(
    map: EditorMap,
    pixels: IntArray,
    width: Int,
    height: Int,
): Long {
    val pngBytes =
        de.egril.defender.mapgen.MapImageEncoder
            .encodeToPng(pixels, width, height)
    if (pngBytes != null) {
        val targetDir = if (map.isOfficial) OFFICIAL_MAPS_DIR else USER_MAPS_DIR
        fileStorage.writeBinaryFile("$targetDir/${map.id}.png", pngBytes)
        pendingMapImages.remove(map.id)
        println("Generated map image: ${map.id}.png (${pngBytes.size / 1024} KB)")
        return pngBytes.size.toLong()
    }
    return -1
}

/**
 * Save a provided map image (PNG bytes) directly without generation or encoding.
 * Used when a user provides a background image to use as the map image.
 */
fun EditorStorage.saveProvidedMapImage(
    map: EditorMap,
    imageBytes: ByteArray,
): Long {
    val targetDir = if (map.isOfficial) OFFICIAL_MAPS_DIR else USER_MAPS_DIR
    fileStorage.writeBinaryFile("$targetDir/${map.id}.png", imageBytes)
    pendingMapImages.remove(map.id)
    println("Saved provided map image: ${map.id}.png (${imageBytes.size / 1024} KB)")
    return imageBytes.size.toLong()
}

/**
 * Save a map, including JSON data and (if needed) regenerating + compressing the map image.
 * Convenience method that calls [saveMapData], [generateMapPixels], and [compressAndSaveMapImage].
 */
fun EditorStorage.saveMap(
    map: EditorMap,
    oldId: String? = null,
): Boolean {
    val result = saveMapData(map, oldId)
    if (result.imageNeedsRegeneration) {
        generateAndSaveMapImage(result.validatedMap)
    }
    result.zoneIndicesToRegenerate.forEach { generateAndSaveTileZoneImage(result.validatedMap, it) }
    return result.imageNeedsRegeneration || result.zoneImagesNeedRegeneration
}

/**
 * Copy a map, reusing the existing PNG image instead of regenerating it.
 * The copied map is always saved as a user map (isOfficial = false).
 */
fun EditorStorage.copyMap(
    sourceMap: EditorMap,
    copiedMap: EditorMap,
    storage: FileStorage = fileStorage,
) {
    val validatedMap =
        copiedMap.copy(
            isOfficial = false,
            isCommunity = false,
            readyToUse = if (copiedMap.isValid) copiedMap.validateReadyToUse() else false,
        )
    mapsCache[validatedMap.id] = validatedMap
    val json = EditorJsonSerializer.serializeMap(validatedMap)
    storage.writeFile("$USER_MAPS_DIR/${validatedMap.id}.json", json)

    if (!validatedMap.canRenderMinimap()) {
        return
    }

    // Copy the PNG from the source map rather than regenerating it
    val sourcePng = readMapImageBytes(sourceMap.id, sourceMap.isOfficial, sourceMap.isCommunity, storage)
    if (sourcePng != null) {
        storage.writeBinaryFile("$USER_MAPS_DIR/${validatedMap.id}.png", sourcePng)
        println("Copied map image from ${sourceMap.id} to ${validatedMap.id}")
    } else {
        println("No source image found for ${sourceMap.id}, generating new image")
        generateAndSaveMapImage(validatedMap, storage)
    }
    validatedMap.tileZones.forEachIndexed { index, _ ->
        val name = MapImageProvider.tileZoneImageFileName(sourceMap.id, index)
        val image = readMapImageBytes(name.removeSuffix(".png"), sourceMap.isOfficial, sourceMap.isCommunity, storage)
        if (sourceMap.tileZones == validatedMap.tileZones && image != null) {
            storage.writeBinaryFile(
                "$USER_MAPS_DIR/${MapImageProvider.tileZoneImageFileName(validatedMap.id, index)}",
                image,
            )
        } else {
            generateAndSaveTileZoneImages(validatedMap, storage)
            return
        }
    }
}

/**
 * Read the raw PNG bytes for a map image.
 * Checks the expected directory first (based on [isOfficial]) to avoid unnecessary lookups.
 */
private fun EditorStorage.readMapImageBytes(
    mapId: String,
    isOfficial: Boolean,
    isCommunity: Boolean = false,
    storage: FileStorage = fileStorage,
): ByteArray? =
    if (isCommunity) {
        storage.readBinaryFile("$COMMUNITY_MAPS_DIR/$mapId.png")
    } else if (isOfficial) {
        storage.readBinaryFile("$OFFICIAL_MAPS_DIR/$mapId.png")
            ?: storage.readBinaryFile("$USER_MAPS_DIR/$mapId.png")
    } else {
        storage.readBinaryFile("$USER_MAPS_DIR/$mapId.png")
            ?: storage.readBinaryFile("$OFFICIAL_MAPS_DIR/$mapId.png")
    } ?: storage.readBinaryFile("$LEGACY_MAPS_DIR/$mapId.png")

/**
 * Normalizes tile types for deciding whether map image regeneration is needed.
 * TARGET always uses PATH visuals. SPAWN_POINT uses PATH when LAND and RIVER when WATER.
 */
internal fun EditorStorage.normalizeForImageComparison(map: EditorMap): Map<String, TileType> =
    map.tiles.mapValues { (pos, tileType) ->
        when (tileType) {
            TileType.SPAWN_POINT ->
                if (map.spawnPointInfoMap[pos] == de.egril.defender.model.SpawnPointType.WATER) {
                    TileType.RIVER
                } else {
                    TileType.PATH
                }
            TileType.TARGET -> TileType.PATH
            else -> tileType
        }
    }

private fun EditorStorage.generateAndSaveMapImage(
    map: EditorMap,
    storage: FileStorage = fileStorage,
) {
    if (!map.canRenderMinimap()) {
        return
    }
    try {
        val (pixels, width, height) =
            de.egril.defender.mapgen.MapImageGenerator
                .generatePixels(map)
        val pngBytes =
            de.egril.defender.mapgen.MapImageEncoder
                .encodeToPng(pixels, width, height)
        if (pngBytes != null) {
            val targetDir = if (map.isOfficial) OFFICIAL_MAPS_DIR else USER_MAPS_DIR
            storage.writeBinaryFile("$targetDir/${map.id}.png", pngBytes)
            pendingMapImages.remove(map.id)
            println("Generated map image: ${map.id}.png (${pngBytes.size / 1024} KB)")
        }
    } catch (e: Exception) {
        println("Failed to generate map image for ${map.id}: ${e.message}")
    }
}

internal fun EditorStorage.generateAndSaveCommunityMapImage(
    map: EditorMap,
    storage: FileStorage = fileStorage,
) {
    if (!map.canRenderMinimap()) {
        return
    }
    try {
        val (pixels, width, height) =
            de.egril.defender.mapgen.MapImageGenerator
                .generatePixels(map)
        val pngBytes =
            de.egril.defender.mapgen.MapImageEncoder
                .encodeToPng(pixels, width, height)
        if (pngBytes != null) {
            storage.writeBinaryFile("$COMMUNITY_MAPS_DIR/${map.id}.png", pngBytes)
            println("Generated community map image: ${map.id}.png")
        }
    } catch (e: Exception) {
        println("Failed to generate community map image for ${map.id}: ${e.message}")
    }
}

fun EditorStorage.reloadMap(id: String): EditorMap? {
    // Force reload from file, bypassing cache
    mapsCache.remove(id)
    return getMap(id)
}

fun EditorStorage.getMap(id: String): EditorMap? {
    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
        println("EditorStorage: Retrieving map with ID: $id")
    }

    // Check cache first
    if (mapsCache.containsKey(id)) {
        return mapsCache[id]
    }

    // Check community cache
    if (communityMapsCache.containsKey(id)) {
        return communityMapsCache[id]
    }

    // Try to load from official directory first, then community, then user, then legacy.
    // Community comes before user so that maps downloaded alongside community levels are found
    // even if no identically-named map exists in the user maps directory.
    var json = fileStorage.readFile("$OFFICIAL_MAPS_DIR/$id.json")
    var isOfficial = true
    var isCommunity = false

    if (json == null) {
        json = fileStorage.readFile("$COMMUNITY_MAPS_DIR/$id.json")
        isOfficial = false
        isCommunity = json != null
    }

    if (json == null) {
        json = fileStorage.readFile("$USER_MAPS_DIR/$id.json")
        isOfficial = false
    }

    if (json == null) {
        json = fileStorage.readFile("$LEGACY_MAPS_DIR/$id.json")
        // For legacy files, check if it's an official map
        isOfficial = OfficialContent.isOfficialMap(id)
        isCommunity = false
    }

    if (json != null) {
        val map = EditorJsonSerializer.deserializeMap(json)
        if (map != null) {
            // Always recalculate readyToUse when loading from file
            // Set isOfficial and isCommunity flags based on which directory it was found in
            val validatedMap =
                map.copy(
                    readyToUse = if (map.isValid) map.validateReadyToUse() else false,
                    isOfficial = map.isOfficial || isOfficial,
                    isCommunity = isCommunity,
                )
            if (isCommunity) {
                communityMapsCache[id] = validatedMap
            } else {
                mapsCache[id] = validatedMap
            }
            return validatedMap
        }
    }

    return null
}

fun EditorStorage.getAllMaps(): List<EditorMap> {
    // Load all maps from both official and user directories
    fileStorage.createDirectory(OFFICIAL_MAPS_DIR)
    fileStorage.createDirectory(USER_MAPS_DIR)

    val officialFiles = fileStorage.listFiles(OFFICIAL_MAPS_DIR)
    val userFiles = fileStorage.listFiles(USER_MAPS_DIR)

    // Load official maps
    for (filename in officialFiles) {
        if (!filename.endsWith(".json")) continue
        val id = filename.removeSuffix(".json")
        if (!mapsCache.containsKey(id)) {
            getMap(id) // This will load and cache it
        }
    }

    // Load user maps
    for (filename in userFiles) {
        if (!filename.endsWith(".json")) continue
        val id = filename.removeSuffix(".json")
        if (!mapsCache.containsKey(id)) {
            getMap(id) // This will load and cache it
        }
    }

    return mapsCache.values.toList()
}

fun EditorStorage.getMapTemplates(): List<MapTemplateDefinition> {
    ensureInitialized()
    fileStorage.createDirectory(OFFICIAL_MAP_TEMPLATES_DIR)
    fileStorage.createDirectory(USER_MAP_TEMPLATES_DIR)

    val templatesById = linkedMapOf<String, MapTemplateDefinition>()
    loadMapTemplatesFromDirectory(OFFICIAL_MAP_TEMPLATES_DIR).forEach { template ->
        templatesById[template.id] = template
    }
    loadMapTemplatesFromDirectory(USER_MAP_TEMPLATES_DIR).forEach { template ->
        templatesById[template.id] = template
    }
    return templatesById.values.sortedBy { it.name.lowercase() }
}

fun EditorStorage.getSpawnTurnTemplates(): List<SpawnTurnTemplateDefinition> {
    ensureInitialized()
    fileStorage.createDirectory(OFFICIAL_SPAWN_TEMPLATES_DIR)
    fileStorage.createDirectory(USER_SPAWN_TEMPLATES_DIR)

    val templatesById = linkedMapOf<String, SpawnTurnTemplateDefinition>()
    loadSpawnTurnTemplatesFromDirectory(OFFICIAL_SPAWN_TEMPLATES_DIR).forEach { template ->
        templatesById[template.id] = template
    }
    loadSpawnTurnTemplatesFromDirectory(USER_SPAWN_TEMPLATES_DIR).forEach { template ->
        templatesById[template.id] = template
    }
    return templatesById.values.sortedBy { it.name.lowercase() }
}

fun EditorStorage.saveMapTemplate(template: MapTemplateDefinition) {
    ensureInitialized()
    fileStorage.createDirectory(USER_MAP_TEMPLATES_DIR)
    val json = EditorTemplateJsonSerializer.serializeMapTemplate(template)
    fileStorage.writeFile("$USER_MAP_TEMPLATES_DIR/${template.id}.json", json)
}

fun EditorStorage.saveSpawnTurnTemplate(template: SpawnTurnTemplateDefinition) {
    ensureInitialized()
    fileStorage.createDirectory(USER_SPAWN_TEMPLATES_DIR)
    val json = EditorTemplateJsonSerializer.serializeSpawnTurnTemplate(template)
    fileStorage.writeFile("$USER_SPAWN_TEMPLATES_DIR/${template.id}.json", json)
}

private fun EditorStorage.loadMapTemplatesFromDirectory(directory: String): List<MapTemplateDefinition> =
    fileStorage
        .listFiles(directory)
        .filter { it.endsWith(".json") }
        .mapNotNull { filename ->
            val json = fileStorage.readFile("$directory/$filename") ?: return@mapNotNull null
            EditorTemplateJsonSerializer.deserializeMapTemplate(json)
        }

private fun EditorStorage.loadSpawnTurnTemplatesFromDirectory(directory: String): List<SpawnTurnTemplateDefinition> =
    fileStorage
        .listFiles(directory)
        .filter { it.endsWith(".json") }
        .mapNotNull { filename ->
            val json = fileStorage.readFile("$directory/$filename") ?: return@mapNotNull null
            EditorTemplateJsonSerializer.deserializeSpawnTurnTemplate(json)
        }

fun EditorStorage.deleteMap(mapId: String): Boolean {
    // Get the map to check if it's official
    val map = getMap(mapId)
    if (map?.isOfficial == true) {
        println("Cannot delete official map: $mapId")
        return false
    }

    // Remove from cache
    mapsCache.remove(mapId)

    // Delete JSON and PNG files from user directory
    fileStorage.deleteFile("$USER_MAPS_DIR/$mapId.json")
    fileStorage.deleteFile("$USER_MAPS_DIR/$mapId.png")
    removeStaleTileZoneImages(USER_MAPS_DIR, mapId, 0)
    return true
}
