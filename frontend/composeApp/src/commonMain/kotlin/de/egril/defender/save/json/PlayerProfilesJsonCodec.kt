package de.egril.defender.save

import de.egril.defender.config.LogConfig
import de.egril.defender.model.*
import de.egril.defender.save.SaveJsonFormat.PROGRAM_NAME
import de.egril.defender.utils.JsonUtils

internal object PlayerProfilesJsonCodec {
    internal fun serializePlayerProfiles(profiles: PlayerProfiles): String {
        val profilesJson =
            profiles.profiles.joinToString(",\n    ") { profile ->
                val achievementsJson =
                    profile.achievements.joinToString(", ") { achievement ->
                        """{"id": "${achievement.id.name}", "earnedAt": ${achievement.earnedAt}}"""
                    }
                val abilitiesJson = serializePlayerAbilities(profile.abilities)
                val remoteUsernameJson = profile.remoteUsername?.let { "\"$it\"" } ?: "null"
                """{
      "id": "${profile.id}",
      "name": "${profile.name}",
      "createdAt": ${profile.createdAt},
      "lastPlayedAt": ${profile.lastPlayedAt},
      "achievements": [$achievementsJson],
      "abilities": $abilitiesJson,
      "remoteUsername": $remoteUsernameJson,
      "alwaysLogin": ${profile.alwaysLogin},
      "useRemoteSettings": ${profile.useRemoteSettings}
    }"""
            }

        val lastUsedPlayerIdJson = profiles.lastUsedPlayerId?.let { "\"$it\"" } ?: "null"

        return """{
  "metadata": {
    "program": "$PROGRAM_NAME",
    "type": "players"
  },
  "data": {
    "profiles": [
      $profilesJson
    ],
    "lastUsedPlayerId": $lastUsedPlayerIdJson
  }
}"""
    }

    internal fun serializePlayerAbilities(abilities: PlayerAbilities): String {
        val unlockedSpellsJson = abilities.unlockedSpells.joinToString(", ") { "\"${it.name}\"" }
        return """{
      "totalXP": ${abilities.totalXP},
      "level": ${abilities.level},
      "availableAbilityPoints": ${abilities.availableAbilityPoints},
      "healthAbility": ${abilities.healthAbility},
      "treasuryAbility": ${abilities.treasuryAbility},
      "incomeAbility": ${abilities.incomeAbility},
      "constructionAbility": ${abilities.constructionAbility},
      "manaAbility": ${abilities.manaAbility},
      "unlockedSpells": [$unlockedSpellsJson]
    }"""
    }

    internal fun deserializePlayerProfiles(json: String): PlayerProfiles? {
        val dataJson =
            de.egril.defender.utils.JsonUtils
                .extractDataSection(json)
        try {
            val profiles = mutableListOf<PlayerProfile>()

            // Parse profiles
            val profilesSection = JsonUtils.extractJsonArraySection(json, "\"profiles\": [")

            if (profilesSection.isNotBlank()) {
                val profileEntries = JsonUtils.splitJsonArray(profilesSection)
                for (entry in profileEntries) {
                    val id = JsonUtils.extractValue(entry, "id")
                    val name = JsonUtils.extractValue(entry, "name")
                    val createdAt = JsonUtils.extractValue(entry, "createdAt").toLong()
                    val lastPlayedAt = JsonUtils.extractValue(entry, "lastPlayedAt").toLong()

                    // Parse achievements (if present, for backward compatibility)
                    val achievements = mutableListOf<Achievement>()
                    try {
                        val achievementsSection = entry.substringAfter("\"achievements\": [").substringBefore("]")
                        if (achievementsSection.isNotBlank()) {
                            val achievementEntries = JsonUtils.splitJsonArray(achievementsSection)
                            for (achEntry in achievementEntries) {
                                val achievementId = JsonUtils.extractValue(achEntry, "id")
                                val earnedAt = JsonUtils.extractValue(achEntry, "earnedAt").toLong()
                                achievements.add(
                                    Achievement(
                                        id = AchievementId.valueOf(achievementId),
                                        earnedAt = earnedAt,
                                    ),
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore achievement parsing errors for backward compatibility
                    }

                    // if (LogConfig.ENABLE_XP_LOGGING) {
                    //     println("=== XP DEBUG: Deserializing player entry JSON: $entry")
                    // }

                    // Parse abilities (if present, for backward compatibility)
                    val abilities =
                        try {
                            // Extract the abilities object - find the opening brace and match it
                            val abilitiesStart = entry.indexOf("\"abilities\": {")
                            if (abilitiesStart >= 0) {
                                val jsonAfterAbilities = entry.substring(abilitiesStart + "\"abilities\": ".length)
                                // Find matching closing brace
                                var depth = 0
                                var endIndex = -1
                                for (i in jsonAfterAbilities.indices) {
                                    when (jsonAfterAbilities[i]) {
                                        '{' -> depth++
                                        '}' -> {
                                            depth--
                                            if (depth == 0) {
                                                endIndex = i + 1
                                                break
                                            }
                                        }
                                    }
                                }
                                val abilitiesJson = if (endIndex > 0) jsonAfterAbilities.substring(0, endIndex) else jsonAfterAbilities
                                if (LogConfig.ENABLE_XP_LOGGING) {
                                    println("=== XP DEBUG: Deserializing abilities JSON: $abilitiesJson")
                                }
                                val result = deserializePlayerAbilities(abilitiesJson)
                                if (LogConfig.ENABLE_XP_LOGGING) {
                                    println(
                                        "=== XP DEBUG: Deserialized abilities: totalXP=${result.totalXP}, healthAbility=${result.healthAbility}, level=${result.level}",
                                    )
                                }
                                result
                            } else {
                                PlayerAbilities()
                            }
                        } catch (e: Exception) {
                            if (LogConfig.ENABLE_XP_LOGGING) {
                                println("=== XP DEBUG ERROR: Failed to parse abilities: ${e.message}")
                                e.printStackTrace()
                                if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                                    println("=== XP DEBUG: Returning default PlayerAbilities() with totalXP=0")
                                }
                            }
                            PlayerAbilities() // Default abilities for backward compatibility
                        }

                    // Parse remoteUsername (optional, for backward compatibility)
                    val remoteUsername =
                        try {
                            val value = JsonUtils.extractValue(entry, "remoteUsername")
                            if (value == "null") null else value
                        } catch (e: Exception) {
                            null
                        }

                    // Parse alwaysLogin (optional, for backward compatibility – defaults to false)
                    val alwaysLogin =
                        try {
                            JsonUtils.extractValue(entry, "alwaysLogin").toBooleanStrict()
                        } catch (e: Exception) {
                            false
                        }

                    // Parse useRemoteSettings (optional, for backward compatibility – defaults to true)
                    val useRemoteSettings =
                        try {
                            JsonUtils.extractValue(entry, "useRemoteSettings").toBooleanStrict()
                        } catch (e: Exception) {
                            true
                        }

                    profiles.add(
                        PlayerProfile(
                            id = id,
                            name = name,
                            createdAt = createdAt,
                            lastPlayedAt = lastPlayedAt,
                            achievements = achievements,
                            abilities = abilities,
                            remoteUsername = remoteUsername,
                            alwaysLogin = alwaysLogin,
                            useRemoteSettings = useRemoteSettings,
                        ),
                    )
                }
            }

            // Parse lastUsedPlayerId
            val lastUsedPlayerId =
                try {
                    val value = JsonUtils.extractValue(dataJson, "lastUsedPlayerId")
                    if (value == "null") null else value
                } catch (e: Exception) {
                    null
                }

            return PlayerProfiles(profiles, lastUsedPlayerId)
        } catch (e: Exception) {
            if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Error deserializing player profiles: ${e.message}")
            }
            return null
        }
    }

    internal fun deserializePlayerAbilities(json: String): PlayerAbilities {
        try {
            if (LogConfig.ENABLE_XP_LOGGING) {
                println("=== XP DEBUG: deserializePlayerAbilities called with: $json")
            }
            val totalXP = JsonUtils.extractValue(json, "totalXP").toInt()
            if (LogConfig.ENABLE_XP_LOGGING) {
                println("=== XP DEBUG: Extracted totalXP = $totalXP")
            }

            // Try to extract level and availableAbilityPoints, fallback to calculated/default for backward compatibility
            val level =
                try {
                    JsonUtils.extractValue(json, "level").toInt()
                } catch (e: Exception) {
                    PlayerAbilities.calculateLevel(totalXP) // Backward compatibility: calculate from XP
                }

            val availableAbilityPoints =
                try {
                    JsonUtils.extractValue(json, "availableAbilityPoints").toInt()
                } catch (e: Exception) {
                    0 // Backward compatibility: default to 0
                }

            val healthAbility = JsonUtils.extractValue(json, "healthAbility").toInt()
            val treasuryAbility = JsonUtils.extractValue(json, "treasuryAbility").toInt()
            val incomeAbility = JsonUtils.extractValue(json, "incomeAbility").toInt()
            val constructionAbility = JsonUtils.extractValue(json, "constructionAbility").toInt()
            val manaAbility = JsonUtils.extractValue(json, "manaAbility").toInt()

            // Parse unlocked spells
            val unlockedSpells = mutableListOf<SpellType>()
            try {
                val spellsSection = json.substringAfter("\"unlockedSpells\": [").substringBefore("]")
                if (spellsSection.isNotBlank()) {
                    val spellNames = spellsSection.split(",").map { it.trim().removeSurrounding("\"") }
                    for (spellName in spellNames) {
                        if (spellName.isNotBlank()) {
                            unlockedSpells.add(SpellType.valueOf(spellName))
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore spell parsing errors
                if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("DEBUG: Error parsing spells: ${e.message}")
                }
            }

            val result =
                PlayerAbilities(
                    totalXP = totalXP,
                    level = level,
                    availableAbilityPoints = availableAbilityPoints,
                    healthAbility = healthAbility,
                    treasuryAbility = treasuryAbility,
                    incomeAbility = incomeAbility,
                    constructionAbility = constructionAbility,
                    manaAbility = manaAbility,
                    unlockedSpells = unlockedSpells.toSet(),
                )
            if (LogConfig.ENABLE_XP_LOGGING) {
                println(
                    "=== XP DEBUG: Created PlayerAbilities with totalXP=${result.totalXP}, level=${result.level}, availableAbilityPoints=${result.availableAbilityPoints}",
                )
            }
            return result
        } catch (e: Exception) {
            if (LogConfig.ENABLE_XP_LOGGING) {
                println("=== XP DEBUG ERROR: Failed to deserialize PlayerAbilities: ${e.message}")
                e.printStackTrace()
                if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("=== XP DEBUG: Returning default PlayerAbilities() with totalXP=0, level=1, availableAbilityPoints=0")
                }
            }
            return PlayerAbilities()
        }
    }
}
