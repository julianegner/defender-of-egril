package de.egril.defender.editor

import de.egril.defender.editor.EditorEventJsonCodec.serializeEvent
import de.egril.defender.editor.EditorJsonScanner.PROGRAM_NAME
import de.egril.defender.editor.EditorSpawnGroupJsonCodec.serializeSpawnSequenceEntry
import de.egril.defender.editor.EditorSupportsJsonCodec.serializeSupportCount
import de.egril.defender.model.DEFAULT_AUTO_TOWER_BASE_EXTRA_COST

/**
 * JSON serializer for editor level files (map reference, spawns, waypoints, supports, events,
 * initial data and spawn groups).
 */
internal object EditorLevelJsonCodec {
    fun serializeLevel(level: EditorLevel): String {
        val spawnsJson =
            level.enemySpawns.joinToString(",\n    ") { spawn ->
                val spawnPointJson =
                    if (spawn.spawnPoint != null) {
                        """, "spawnPoint": {"x": ${spawn.spawnPoint.x}, "y": ${spawn.spawnPoint.y}}"""
                    } else {
                        ""
                    }
                """{"attackerType": "${spawn.attackerType.name}", "level": ${spawn.level}, "spawnTurn": ${spawn.spawnTurn}$spawnPointJson}"""
            }

        val towersJson = level.availableTowers.joinToString(", ") { "\"${it.name}\"" }

        val waypointsJson =
            level.waypoints.joinToString(",\n    ") { waypoint ->
                """{"position": {"x": ${waypoint.position.x}, "y": ${waypoint.position.y}}, "nextTargetPosition": {"x": ${waypoint.nextTargetPosition.x}, "y": ${waypoint.nextTargetPosition.y}}}"""
            }

        val singleHitTargetOrderJson =
            if (level.singleHitTargetOrder.isNotEmpty()) {
                ",\n  \"singleHitTargetOrder\": [${level.singleHitTargetOrder.joinToString(", ") { position -> """{"x": ${position.x}, "y": ${position.y}}""" }}]"
            } else {
                ""
            }

        val prerequisitesJson = level.prerequisites.joinToString(", ") { "\"$it\"" }

        val titleKeyJson =
            if (level.titleKey != null) {
                ",\n  \"titleKey\": \"${level.titleKey}\""
            } else {
                ""
            }

        val subtitleKeyJson =
            if (level.subtitleKey != null) {
                ",\n  \"subtitleKey\": \"${level.subtitleKey}\""
            } else {
                ""
            }

        val requiredCountJson =
            if (level.requiredPrerequisiteCount != null) {
                ",\n  \"requiredPrerequisiteCount\": ${level.requiredPrerequisiteCount}"
            } else {
                ""
            }

        val testingOnlyJson =
            if (level.testingOnly) {
                ",\n  \"testingOnly\": true"
            } else {
                ""
            }

        val allowAutoAttackJson =
            if (!level.allowAutoAttack) {
                ",\n  \"allowAutoAttack\": false"
            } else {
                ""
            }

        val connectedToPreviousLevelJson =
            if (level.connectedToPreviousLevel) {
                ",\n  \"connectedToPreviousLevel\": true"
            } else {
                ""
            }

        val autoTowerBaseJson =
            if (level.autoTowerBase) {
                ",\n  \"autoTowerBase\": true" +
                    if (level.autoTowerBaseExtraCost != DEFAULT_AUTO_TOWER_BASE_EXTRA_COST) {
                        ",\n  \"autoTowerBaseExtraCost\": ${level.autoTowerBaseExtraCost}"
                    } else {
                        ""
                    }
            } else {
                ""
            }

        val isSandboxJson =
            if (level.isSandbox) {
                ",\n  \"isSandbox\": true"
            } else {
                ""
            }

        val waaghEnabledJson =
            if (level.waaghEnabled) {
                ",\n  \"waaghEnabled\": true"
            } else {
                ""
            }

        val isOfficialJson =
            if (level.isOfficial) {
                ",\n  \"isOfficial\": true"
            } else {
                ""
            }

        val authorJson =
            if (level.author.isNotEmpty()) {
                ",\n  \"author\": \"${level.author}\""
            } else {
                ""
            }

        val communityDescriptionJson =
            if (level.communityDescription.isNotEmpty()) {
                val escaped =
                    level.communityDescription
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t")
                ",\n  \"communityDescription\": \"$escaped\""
            } else {
                ""
            }

        // Serialize player-usable supports (objects + spell tokens), compact one-per-line
        val supportsJson =
            if (level.supports.isNotEmpty()) {
                val parts = mutableListOf<String>()
                if (level.supports.objects.isNotEmpty()) {
                    val objectsData =
                        level.supports.objects.joinToString(",\n      ") { obj ->
                            """{"type": "${obj.type.name}", "count": ${serializeSupportCount(obj.count)}, "damage": ${obj.damage}, "healthPoints": ${obj.healthPoints}}"""
                        }
                    parts.add(
                        """"objects": [
      $objectsData
    ]""",
                    )
                }
                if (level.supports.spells.isNotEmpty()) {
                    val spellsData =
                        level.supports.spells.joinToString(",\n      ") { supportSpell ->
                            """{"spell": "${supportSpell.spell.name}", "count": ${serializeSupportCount(supportSpell.count)}}"""
                        }
                    parts.add(
                        """"spells": [
      $spellsData
    ]""",
                    )
                }
                if (level.supports.cooldownPowers.isNotEmpty()) {
                    val cooldownData =
                        level.supports.cooldownPowers.joinToString(",\n      ") { power ->
                            """{"type": "${power.type.name}", "cooldownTurns": ${power.cooldownTurns}, "startActive": ${power.startActive}}"""
                        }
                    parts.add(
                        """"cooldownPowers": [
      $cooldownData
    ]""",
                    )
                }
                if (level.supports.fiefs.isNotEmpty()) {
                    val fiefsData =
                        level.supports.fiefs.joinToString(",\n      ") { supportFief ->
                            """{"type": "${supportFief.type.name}", "count": ${serializeSupportCount(supportFief.count)}}"""
                        }
                    parts.add(
                        """"fiefs": [
      $fiefsData
    ]""",
                    )
                }
                val allParts = parts.joinToString(",\n    ")
                ",\n  \"supports\": {\n    $allParts\n  }"
            } else {
                ""
            }

        // Serialize scripted events (conditions + actions + predefined story messages), one per line
        val eventsJson =
            if (level.events.isNotEmpty()) {
                val eventsData =
                    level.events.events.joinToString(",\n    ") { event ->
                        serializeEvent(event)
                    }
                ",\n  \"events\": [\n    $eventsData\n  ]"
            } else {
                ""
            }

        // Serialize initial data in new nested format (optional)
        val initialData = level.getEffectiveInitialData()
        val initialDataJson =
            if (initialData.defenders.isNotEmpty() ||
                initialData.attackers.isNotEmpty() ||
                initialData.traps.isNotEmpty() ||
                initialData.barricades.isNotEmpty() ||
                initialData.bridges.isNotEmpty() ||
                initialData.fiefs.isNotEmpty() ||
                initialData.mushrooms.isNotEmpty() ||
                initialData.gargoyles.isNotEmpty() ||
                initialData.portals.isNotEmpty() ||
                initialData.placementAreas.isNotEmpty()
            ) {
                val parts = mutableListOf<String>()

                // Defenders
                if (initialData.defenders.isNotEmpty()) {
                    val defendersData =
                        initialData.defenders.joinToString(",\n      ") { defender ->
                            val dragonNameJson =
                                if (defender.dragonName != null) {
                                    """, "dragonName": "${defender.dragonName}""""
                                } else {
                                    ""
                                }
                            val onTowerBaseJson =
                                if (defender.onTowerBase) {
                                    """, "onTowerBase": true"""
                                } else {
                                    ""
                                }
                            """{"type": "${defender.type.name}", "position": {"x": ${defender.position.x}, "y": ${defender.position.y}}, "level": ${defender.level}$dragonNameJson$onTowerBaseJson}"""
                        }
                    parts.add(
                        """"defenders": [
      $defendersData
    ]""",
                    )
                }

                // Attackers
                if (initialData.attackers.isNotEmpty()) {
                    val attackersData =
                        initialData.attackers.joinToString(",\n      ") { attacker ->
                            val healthJson =
                                if (attacker.currentHealth != null) {
                                    """, "currentHealth": ${attacker.currentHealth}"""
                                } else {
                                    ""
                                }
                            val dragonNameJson =
                                if (attacker.dragonName != null) {
                                    """, "dragonName": "${attacker.dragonName}""""
                                } else {
                                    ""
                                }
                            """{"type": "${attacker.type.name}", "position": {"x": ${attacker.position.x}, "y": ${attacker.position.y}}, "level": ${attacker.level}$healthJson$dragonNameJson}"""
                        }
                    parts.add(
                        """"attackers": [
      $attackersData
    ]""",
                    )
                }

                // Traps
                if (initialData.traps.isNotEmpty()) {
                    val trapsData =
                        initialData.traps.joinToString(",\n      ") { trap ->
                            """{"position": {"x": ${trap.position.x}, "y": ${trap.position.y}}, "damage": ${trap.damage}, "type": "${trap.type}"}"""
                        }
                    parts.add(
                        """"traps": [
      $trapsData
    ]""",
                    )
                }

                // Barricades
                if (initialData.barricades.isNotEmpty()) {
                    val barricadesData =
                        initialData.barricades.joinToString(",\n      ") { barricade ->
                            val supportsTowerJson =
                                if (barricade.supportsTower) {
                                    """, "supportsTower": true"""
                                } else {
                                    ""
                                }
                            val nameJson =
                                if (!barricade.name.isNullOrBlank()) {
                                    """, "name": "${barricade.name}""""
                                } else {
                                    ""
                                }
                            val isGateJson =
                                if (barricade.isGate) {
                                    """, "isGate": true"""
                                } else {
                                    ""
                                }
                            """{"position": {"x": ${barricade.position.x}, "y": ${barricade.position.y}}, "healthPoints": ${barricade.healthPoints}$supportsTowerJson$nameJson$isGateJson}"""
                        }
                    parts.add(
                        """"barricades": [
      $barricadesData
    ]""",
                    )
                }

                // Bridges
                if (initialData.bridges.isNotEmpty()) {
                    val bridgesData =
                        initialData.bridges.joinToString(",\n      ") { bridge ->
                            val isIndestructibleJson =
                                if (bridge.isIndestructible) {
                                    """, "isIndestructible": true"""
                                } else {
                                    ""
                                }
                            """{"position": {"x": ${bridge.position.x}, "y": ${bridge.position.y}}, "type": "${bridge.type.name}", "healthPoints": ${bridge.healthPoints}$isIndestructibleJson}"""
                        }
                    parts.add(
                        """"bridges": [
      $bridgesData
    ]""",
                    )
                }

                // Fiefs
                if (initialData.fiefs.isNotEmpty()) {
                    val fiefsData =
                        initialData.fiefs.joinToString(",\n      ") { fief ->
                            val x = fief.position.x
                            val y = fief.position.y
                            val type = fief.type.name
                            """{"position": {"x": $x, "y": $y}, "type": "$type"}"""
                        }
                    parts.add(
                        """"fiefs": [
      $fiefsData
    ]""",
                    )
                }

                // Mushrooms
                if (initialData.mushrooms.isNotEmpty()) {
                    val mushroomsData =
                        initialData.mushrooms.joinToString(",\n      ") { mushroom ->
                            val x = mushroom.position.x
                            val y = mushroom.position.y
                            """{"position": {"x": $x, "y": $y}}"""
                        }
                    parts.add(
                        """"mushrooms": [
      $mushroomsData
    ]""",
                    )
                }

                // Gargoyles
                if (initialData.gargoyles.isNotEmpty()) {
                    val gargoylesData =
                        initialData.gargoyles.joinToString(",\n      ") { gargoyle ->
                            val x = gargoyle.position.x
                            val y = gargoyle.position.y
                            """{"position": {"x": $x, "y": $y}, "level": ${gargoyle.level}}"""
                        }
                    parts.add(
                        """"gargoyles": [
      $gargoylesData
    ]""",
                    )
                }

                // Portals
                if (initialData.portals.isNotEmpty()) {
                    val portalsData =
                        initialData.portals.joinToString(",\n      ") { portal ->
                            """{"entryPosition": {"x": ${portal.entryPosition.x}, "y": ${portal.entryPosition.y}}, "exitPosition": {"x": ${portal.exitPosition.x}, "y": ${portal.exitPosition.y}}}"""
                        }
                    parts.add(
                        """"portals": [
      $portalsData
    ]""",
                    )
                }

                // Placement areas (single-line position objects)
                if (initialData.placementAreas.isNotEmpty()) {
                    val areaData =
                        initialData.placementAreas.joinToString(", ") { pos ->
                            """{"x": ${pos.x}, "y": ${pos.y}}"""
                        }
                    parts.add(""""placementAreas": [$areaData]""")
                }

                val allParts = parts.joinToString(",\n    ")
                ",\n  \"initialData\": {\n    $allParts\n  }"
            } else {
                ""
            }

        // Serialize spawn groups (issue #694), one group per line with compact spawn entries.
        val spawnGroupsJson =
            if (level.spawnGroups != null) {
                val groupsData =
                    level.spawnGroups.joinToString(",\n    ") { entry ->
                        serializeSpawnSequenceEntry(entry)
                    }
                ",\n  \"spawnGroups\": [\n    $groupsData\n  ]"
            } else {
                ""
            }

        val data = """{
  "id": "${level.id}",
  "mapId": "${level.mapId}",
  "title": "${level.title}"$titleKeyJson,
  "subtitle": "${level.subtitle}"$subtitleKeyJson,
  "startCoins": ${level.startCoins},
  "startHealthPoints": ${level.startHealthPoints},
  "enemySpawns": [
    $spawnsJson
  ],
  "availableTowers": [$towersJson],
  "waypoints": [
    $waypointsJson
  ],
  "prerequisites": [$prerequisitesJson]$singleHitTargetOrderJson$requiredCountJson$testingOnlyJson$allowAutoAttackJson$connectedToPreviousLevelJson$autoTowerBaseJson$isSandboxJson$waaghEnabledJson$isOfficialJson$authorJson$communityDescriptionJson$supportsJson$eventsJson$initialDataJson$spawnGroupsJson
}"""
        return """{
  "metadata": {
    "program": "$PROGRAM_NAME",
    "type": "level",
    "license": "GNU Affero General Public License v3.0 (AGPL-3.0)"
  },
  "data": $data
}"""
    }
}
