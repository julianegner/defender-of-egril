package de.egril.defender.save

import de.egril.defender.model.*

/**
 * JSON serialization for save data.
 * Delegates to the codecs in the `json` subpackage.
 */
object SaveJsonSerializer {
    fun serializeWorldMapSave(worldMap: WorldMapSave): String =
        WorldMapJsonCodec.serializeWorldMapSave(worldMap)

    fun deserializeWorldMapSave(json: String): WorldMapSave? =
        WorldMapJsonCodec.deserializeWorldMapSave(json)

    fun serializeSavedGame(savedGame: SavedGame): String =
        SavedGameJsonWriter.serializeSavedGame(savedGame)

    fun deserializeSavedGame(json: String): SavedGame? =
        SavedGameJsonReader.deserializeSavedGame(json)

    fun serializeSaveGameMetadata(metadata: SaveGameMetadata): String =
        SaveMetadataJsonCodec.serializeSaveGameMetadata(metadata)

    fun serializeLevelHandoffSave(handoff: LevelHandoffSave): String =
        LevelHandoffJsonCodec.serializeLevelHandoffSave(handoff)

    fun deserializeLevelHandoffSave(json: String): LevelHandoffSave? =
        LevelHandoffJsonCodec.deserializeLevelHandoffSave(json)

    fun serializePlayerProfiles(profiles: PlayerProfiles): String =
        PlayerProfilesJsonCodec.serializePlayerProfiles(profiles)

    fun deserializePlayerProfiles(json: String): PlayerProfiles? =
        PlayerProfilesJsonCodec.deserializePlayerProfiles(json)
}
