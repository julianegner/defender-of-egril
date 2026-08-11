package de.egril.defender.editor

import de.egril.defender.model.AttackerType
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
        assertEquals(AttackerType.ORK, template.variants.single().entries.last().attackerType)
        assertEquals(1, template.variants.single().entries.last().levelOffset)
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
}
