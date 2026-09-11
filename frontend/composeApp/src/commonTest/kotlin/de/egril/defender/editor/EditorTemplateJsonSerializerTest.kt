package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.RiverTile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EditorTemplateJsonSerializerTest {
    @Test
    fun deserializeTemplateIndexReturnsTemplateIds() {
        val ids =
            EditorTemplateJsonSerializer.deserializeTemplateIndex(
                """
                {
                  "templates": ["alpha", "beta"]
                }
                """.trimIndent(),
            )

        assertEquals(listOf("alpha", "beta"), ids)
    }

    @Test
    fun deserializeSpawnTurnTemplateParsesVariantsAndOffsets() {
        val template =
            EditorTemplateJsonSerializer.deserializeSpawnTurnTemplate(
                """
                {
                  "metadata": {"program": "Defender of Egril", "type": "spawn-turn-template"},
                  "data": {
                    "id": "scouting",
                    "name": "Scouting",
                    "description": "Probe",
                    "variants": [
                      {
                        "kind": "HORDE",
                        "entries": [
                          {"attackerType": "GOBLIN", "turnOffset": 0, "amount": 3},
                          {"attackerType": "ORK", "turnOffset": 1, "amount": 1, "levelOffset": 1}
                        ]
                      }
                    ]
                  }
                }
                """.trimIndent(),
            )

        assertNotNull(template)
        assertEquals("scouting", template.id)
        assertEquals(EditorEnemyTemplateKind.HORDE, template.variants.single().kind)
        assertEquals(
            AttackerType.ORK,
            template.variants
                .single()
                .entries
                .last()
                .attackerType,
        )
        assertEquals(
            1,
            template.variants
                .single()
                .entries
                .last()
                .levelOffset,
        )
    }

    @Test
    fun deserializeMapTemplateParsesLayoutKind() {
        val template =
            EditorTemplateJsonSerializer.deserializeMapTemplate(
                """
                {
                  "metadata": {"program": "Defender of Egril", "type": "map-template"},
                  "data": {
                    "id": "straight_approach",
                    "name": "Straight approach",
                    "layoutKind": "STRAIGHT_APPROACH"
                  }
                }
                """.trimIndent(),
            )

        assertNotNull(template)
        assertEquals(MapTemplateLayoutKind.STRAIGHT_APPROACH, template.layoutKind)
    }

    @Test
    fun spawnTemplateRoundTripKeepsVariantData() {
        val template =
            SpawnTurnTemplateDefinition(
                id = "custom_wave",
                name = "Custom wave",
                description = "Saved from editor",
                variants =
                    listOf(
                        SpawnTurnTemplateVariant(
                            kind = EditorEnemyTemplateKind.DARK_MAGIC,
                            entries =
                                listOf(
                                    SpawnTurnTemplateEntry(AttackerType.EVIL_WIZARD, turnOffset = 0, amount = 2),
                                    SpawnTurnTemplateEntry(AttackerType.RED_WITCH, turnOffset = 1, amount = 1, levelOffset = 2),
                                ),
                        ),
                    ),
            )

        val json = EditorTemplateJsonSerializer.serializeSpawnTurnTemplate(template)
        val restored = EditorTemplateJsonSerializer.deserializeSpawnTurnTemplate(json)

        assertNotNull(restored)
        assertEquals(template, restored)
    }

    @Test
    fun fixedMapTemplateRoundTripKeepsMapData() {
        val map =
            EditorMap(
                id = "template_map",
                name = "Template Map",
                width = 3,
                height = 3,
                tiles = mapOf("0,1" to TileType.SPAWN_POINT, "1,1" to TileType.PATH, "2,1" to TileType.TARGET),
                riverTiles = mapOf("1,0" to RiverTile(Position(1, 0), RiverFlow.EAST, 1)),
            )
        val template = MapTemplateDefinition(id = "template_map", name = "Template Map", templateMap = map)

        val json = EditorTemplateJsonSerializer.serializeMapTemplate(template)
        val restored = EditorTemplateJsonSerializer.deserializeMapTemplate(json)

        assertNotNull(restored)
        assertNotNull(restored.templateMap)
        assertEquals(map.tiles, restored.templateMap.tiles)
        assertEquals(map.riverTiles.keys, restored.templateMap.riverTiles.keys)
    }
}
