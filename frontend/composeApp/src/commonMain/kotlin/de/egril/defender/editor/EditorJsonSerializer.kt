package de.egril.defender.editor

/**
 * Facade for editor JSON serialization. The codec implementations live in the `json` subpackage directory.
 */
object EditorJsonSerializer {
    internal fun extractDataSection(json: String): String = EditorJsonScanner.extractDataSection(json)

    fun serializeMap(map: EditorMap): String = EditorMapJsonCodec.serializeMap(map)

    fun deserializeMap(json: String): EditorMap? = EditorMapJsonCodec.deserializeMap(json)

    fun serializeLevel(level: EditorLevel): String = EditorLevelJsonCodec.serializeLevel(level)

    fun deserializeLevel(json: String): EditorLevel? = EditorLevelJsonDeserializer.deserializeLevel(json)

    fun serializeSequence(sequence: LevelSequence): String = EditorSequenceJsonCodec.serializeSequence(sequence)

    fun deserializeSequence(json: String): LevelSequence? = EditorSequenceJsonCodec.deserializeSequence(json)

    fun serializeWorldMapData(data: WorldMapData): String = EditorSequenceJsonCodec.serializeWorldMapData(data)

    fun deserializeWorldMapData(json: String): WorldMapData? = EditorSequenceJsonCodec.deserializeWorldMapData(json)
}
