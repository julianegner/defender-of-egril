package de.egril.defender.ui.gameplay

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AttackerInfoTest {
    @Test
    fun attackerWithoutMushroomBuffHasNoEnhancementKind() {
        val attacker =
            Attacker(
                id = 1,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(0, 0)),
            )

        assertNull(attacker.mushroomEnhancementKind())
    }

    @Test
    fun hordeUnitWithMushroomBuffShowsSpeedAndLevelEnhancement() {
        val attacker =
            Attacker(
                id = 1,
                type = AttackerType.OGRE,
                position = mutableStateOf(Position(0, 0)),
                mushroomTurnsRemaining = mutableStateOf(2),
            )

        assertEquals(MushroomEnhancementKind.SPEED_AND_LEVEL, attacker.mushroomEnhancementKind())
    }

    @Test
    fun witchWithMushroomBuffShowsAbilityEnhancement() {
        val attacker =
            Attacker(
                id = 1,
                type = AttackerType.RED_WITCH,
                position = mutableStateOf(Position(0, 0)),
                mushroomTurnsRemaining = mutableStateOf(2),
            )

        assertEquals(MushroomEnhancementKind.SPEED_LEVEL_AND_ABILITIES, attacker.mushroomEnhancementKind())
    }
}
