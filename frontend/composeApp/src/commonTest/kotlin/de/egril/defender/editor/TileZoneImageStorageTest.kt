package de.egril.defender.editor

import de.egril.defender.model.Position
import de.egril.defender.model.TileZone
import de.egril.defender.ui.MapImageProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TileZoneImageStorageTest {
    private class MemoryStorage : FileStorage {
        val images = mutableMapOf<String, ByteArray>()
        val text = mutableMapOf<String, String>()

        override fun writeBinaryFile(path: String, content: ByteArray) { images[path] = content }
        override fun readBinaryFile(path: String): ByteArray? = images[path]
        override fun deleteFile(path: String) { images.remove(path); text.remove(path) }
        override fun listFiles(directory: String): List<String> =
            images.keys.filter { it.startsWith("$directory/") }.map { it.removePrefix("$directory/") }

        override fun fileExists(path: String): Boolean = path in images || path in text
        override fun createDirectory(path: String) = Unit
        override fun writeFile(path: String, content: String) { text[path] = content }
        override fun readFile(path: String): String? = text[path]
        override fun renameDirectory(oldPath: String, newPath: String): Boolean = error("Not used")
        override fun copyDirectory(sourcePath: String, targetPath: String): Boolean = error("Not used")
        override fun deleteDirectory(path: String): Boolean = error("Not used")
        override fun getAbsolutePath(path: String): String = error("Not used")
    }

    @Test
    fun savesOnePngPerZoneAndRemovesDeletedZones() {
        val first = TileZone(id = "flood", tiles = mapOf(Position(2, 2) to TileType.RIVER))
        val second = TileZone(id = "ebb", tiles = mapOf(Position(3, 2) to TileType.RIVER))
        val map = EditorMap(
            id = "saved_zones",
            width = 5,
            height = 5,
            tiles = (0..4).flatMap { y -> (0..4).map { x -> "$x,$y" to TileType.PATH } }.toMap(),
            tileZones = listOf(first, second),
        )
        val storage = MemoryStorage()
        val directory = "gamedata/user/maps/"
        val firstFile = directory + MapImageProvider.tileZoneImageFileName(map.id, 0)
        val secondFile = directory + MapImageProvider.tileZoneImageFileName(map.id, 1)

        val bytes = EditorStorage.generateAndSaveTileZoneImages(map, storage)
        assertTrue(bytes > 0)
        assertEquals(setOf(firstFile, secondFile), storage.images.keys)
        assertTrue(storage.images.getValue(firstFile).take(8).toByteArray().contentEquals(
            byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10),
        ))

        EditorStorage.generateAndSaveTileZoneImages(map.copy(tileZones = listOf(first)), storage)
        assertTrue(firstFile in storage.images)
        assertFalse(secondFile in storage.images)
    }

    @Test
    fun editingOnlyTheZoneRegeneratesItsPngNotTheBaseImage() {
        val zone = TileZone(id = "flood", tiles = mapOf(Position(2, 2) to TileType.RIVER))
        val map = EditorMap(
            id = "test_zone_image_regeneration",
            width = 5,
            height = 5,
            tiles = (0..4).flatMap { y -> (0..4).map { x -> "$x,$y" to TileType.PATH } }.toMap(),
            tileZones = listOf(zone),
        )
        val storage = MemoryStorage()
        val first = EditorStorage.saveMapData(map, storage = storage)
        assertTrue(first.imageNeedsRegeneration)
        assertTrue(first.zoneImagesNeedRegeneration)
        storage.images["gamedata/user/maps/${map.id}.png"] = byteArrayOf(1)
        EditorStorage.generateAndSaveTileZoneImages(map, storage)

        val unchanged = EditorStorage.saveMapData(map, storage = storage)
        assertFalse(unchanged.imageNeedsRegeneration)
        assertFalse(unchanged.zoneImagesNeedRegeneration)
        val changed = EditorStorage.saveMapData(
            map.copy(tileZones = listOf(zone.copy(tiles = mapOf(Position(3, 2) to TileType.RIVER)))),
            storage = storage,
        )
        assertFalse(changed.imageNeedsRegeneration)
        assertTrue(changed.zoneImagesNeedRegeneration)
    }

    @Test
    fun onlyChangedOrMissingZoneImagesNeedRegeneration() {
        val first = TileZone(id = "first", tiles = mapOf(Position(2, 2) to TileType.RIVER))
        val second = TileZone(id = "second", tiles = mapOf(Position(3, 3) to TileType.RIVER))
        val map = EditorMap(
            id = "individual_zone_regeneration",
            width = 5,
            height = 5,
            tiles = (0..4).flatMap { y -> (0..4).map { x -> "$x,$y" to TileType.PATH } }.toMap(),
            tileZones = listOf(first, second),
        )
        val storage = MemoryStorage()
        EditorStorage.saveMapData(map, storage = storage)
        storage.images["gamedata/user/maps/${map.id}.png"] = byteArrayOf(1)
        EditorStorage.generateAndSaveTileZoneImages(map, storage)

        val changed = map.copy(tileZones = listOf(first.copy(tiles = mapOf(Position(1, 2) to TileType.RIVER)), second))
        val result = EditorStorage.saveMapData(changed, storage = storage)
        assertFalse(result.imageNeedsRegeneration)
        assertEquals(listOf(0), result.zoneIndicesToRegenerate)

        storage.deleteFile("gamedata/user/maps/${MapImageProvider.tileZoneImageFileName(map.id, 1)}")
        val missing = EditorStorage.saveMapData(changed, storage = storage)
        assertEquals(listOf(0, 1), missing.zoneIndicesToRegenerate)
    }

    @Test
    fun downloadedCommunityMapSavesBaseAndZoneImagesBeforePlay() {
        val zone = TileZone(id = "flood", tiles = mapOf(Position(2, 2) to TileType.RIVER))
        val map = EditorMap(
            id = "downloaded_zone_map",
            width = 5,
            height = 5,
            tiles = (0..4).flatMap { y -> (0..4).map { x -> "$x,$y" to TileType.PATH } }.toMap(),
            tileZones = listOf(zone),
        )
        val storage = MemoryStorage()

        EditorStorage.saveCommunityMap(map, "author", "level_map_alias", storage)

        for (id in listOf(map.id, "level_map_alias")) {
            assertTrue("gamedata/community/maps/$id.json" in storage.text)
            assertTrue("gamedata/community/maps/$id.png" in storage.images)
            assertTrue("gamedata/community/maps/${MapImageProvider.tileZoneImageFileName(id, 0)}" in storage.images)
        }

        val source = map.copy(isCommunity = true)
        val copied = source.copy(id = "copied_community_zone_map", isCommunity = false)
        EditorStorage.copyMap(source, copied, storage)
        assertTrue(storage.images.getValue("gamedata/user/maps/${copied.id}.png").contentEquals(
            storage.images.getValue("gamedata/community/maps/${map.id}.png"),
        ))
        assertTrue(storage.images.getValue(
            "gamedata/user/maps/${MapImageProvider.tileZoneImageFileName(copied.id, 0)}",
        ).contentEquals(
            storage.images.getValue("gamedata/community/maps/${MapImageProvider.tileZoneImageFileName(map.id, 0)}"),
        ))
    }
}
