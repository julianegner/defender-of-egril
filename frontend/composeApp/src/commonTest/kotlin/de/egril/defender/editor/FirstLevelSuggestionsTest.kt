package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.ui.editor.level.events.EventMessageCatalog
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Validates the three suggested "first level after the tutorial" levels (see issue #788) and
 * their maps: they must be loadable, playable directly after the tutorial and actually use the
 * villain, support and scripted-event mechanics they were designed to showcase.
 *
 * Like the other repository tests, these checks are skipped when the repository resources are not
 * available in the current test environment.
 */
class FirstLevelSuggestionsTest {
    private val levelToMap =
        mapOf(
            "gribnaks_ambush" to "map_goblin_gorge",
            "hold_the_gate" to "map_the_watch_gate",
            "the_golden_road" to "map_golden_road",
        )

    private val villainPerLevel =
        mapOf(
            "gribnaks_ambush" to AttackerType.SNOTLING_BOSS,
            "hold_the_gate" to AttackerType.MORGUK_BONEWHISPER,
            "the_golden_road" to AttackerType.ZUSSA,
        )

    @Test
    fun suggestedFirstLevelsAreUnlockedByTheTutorialAndUseTheirMap() =
        runTest {
            for ((levelId, mapId) in levelToMap) {
                val level = RepositoryLoader.loadLevel(levelId) ?: continue

                assertEquals(mapId, level.mapId, "$levelId must use its own map")
                assertEquals(setOf("welcome_to_defender_of_egril"), level.prerequisites, "$levelId follows the tutorial")
                assertTrue(level.isOfficial, "$levelId must be official content")
                assertTrue(level.titleKey != null && level.subtitleKey != null, "$levelId must be translatable")
                assertTrue(level.enemySpawns.isNotEmpty(), "$levelId must spawn enemies")
                assertTrue(level.availableTowers.isNotEmpty(), "$levelId must offer towers")
            }
        }

    @Test
    fun suggestedFirstLevelsUseVillainsSupportsAndEvents() =
        runTest {
            for ((levelId, villain) in villainPerLevel) {
                val level = RepositoryLoader.loadLevel(levelId) ?: continue

                assertTrue(
                    level.enemySpawns.any { it.attackerType == villain },
                    "$levelId must feature the villain ${villain.name}",
                )
                assertTrue(level.supports.isNotEmpty(), "$levelId must grant player supports")
                assertTrue(level.events.isNotEmpty(), "$levelId must contain scripted events")
                level.events.events.forEach { event ->
                    val messageKey = event.messageKey
                    if (messageKey != null) {
                        assertTrue(
                            messageKey in EventMessageCatalog.keys,
                            "$levelId uses unknown event message key $messageKey",
                        )
                    }
                }
            }
        }

    @Test
    fun suggestedFirstLevelMapsArePlayable() =
        runTest {
            for ((levelId, mapId) in levelToMap) {
                val map = RepositoryLoader.loadMap(mapId) ?: continue
                val level = RepositoryLoader.loadLevel(levelId) ?: continue

                assertTrue(map.readyToUse, "$mapId must be marked ready to use")
                assertTrue(map.validateReadyToUse(), "$mapId must connect every spawn point to a target")
                assertTrue(map.hasBuildAreas(), "$mapId must offer buildable tiles")

                val spawnPoints = map.getSpawnPoints().toSet()
                level.enemySpawns.forEach { spawn ->
                    assertTrue(
                        spawn.spawnPoint in spawnPoints,
                        "$levelId spawns ${spawn.attackerType.name} on a tile that is no spawn point of $mapId",
                    )
                }

                val pathCells = map.getPathCells()
                val buildAreas = map.getBuildAreas()
                val initialData = level.initialData ?: InitialData.EMPTY
                initialData.fiefs.forEach {
                    assertTrue(it.position in pathCells, "$levelId places a fief off the path")
                }
                initialData.barricades.forEach {
                    assertTrue(it.position in pathCells, "$levelId places a barricade off the path")
                }
                initialData.defenders.forEach {
                    assertTrue(it.position in buildAreas, "$levelId places a tower outside a build area")
                }
            }
        }

    @Test
    fun suggestedFirstLevelsAreRegisteredInTheSequence() =
        runTest {
            val sequence = RepositoryLoader.loadSequence() ?: return@runTest
            val worldMap = RepositoryLoader.loadWorldMapData()

            levelToMap.keys.forEach { levelId ->
                assertTrue(levelId in sequence.sequence, "$levelId must be part of the level sequence")
                if (worldMap != null) {
                    assertTrue(
                        worldMap.findLocationByLevelId(levelId) != null,
                        "$levelId must be placed on the world map",
                    )
                }
            }
        }
}
