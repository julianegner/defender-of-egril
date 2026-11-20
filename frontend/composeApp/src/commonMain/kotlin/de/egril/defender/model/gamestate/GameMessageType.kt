package de.egril.defender.model

/**
 * Types of in-game event messages that are shown to the player.
 */
enum class GameMessageType {
    TARGET_TAKEN, // A SINGLE_HIT target was captured by an enemy
    GATE_DESTROYED, // A named gate barricade was destroyed
    EWHAD_ENTERS, // Ewhad has entered the battlefield
    EWHAD_RETREATS, // Ewhad has retreated (health reached 0, not final stand)
    EWHAD_DEFEATED, // Ewhad is defeated (health reached 0, final stand level)
    VILLAIN_ENTERS, // A villain has entered the battlefield (name = AttackerType.name)
    VILLAIN_DEFEATED, // A non-Ewhad villain was defeated (name = AttackerType.name)
    SILAS_MIRROR_HIT, // A tower struck Silas's illusion and was blinded
    COVEN_SWAP, // Sybilla swapped places with a witch
    WAAAGH_FRENZY, // The horde has entered a Waaagh! frenzy
    STORY_INTRO, // Story narrative shown at the start of a level (name = editorLevelId)
    EVENT_MESSAGE, // Scripted-event story message (name = string-resource key of the predefined text)
}
