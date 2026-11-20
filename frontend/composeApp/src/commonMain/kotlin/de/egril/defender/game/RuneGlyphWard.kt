package de.egril.defender.game

import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GameState
import de.egril.defender.model.Position
import de.egril.defender.model.hexDistanceTo

fun GameState.isProtectedByRuneGlyphWard(
    target: Attacker,
    position: Position = target.position.value,
): Boolean =
    target.type != AttackerType.GRAND_RUNEMASTER_VAELEN &&
        attackers.any { vaelen ->
            val ability = vaelen.type.villainAbility
            !vaelen.isDefeated.value &&
                vaelen.type == AttackerType.GRAND_RUNEMASTER_VAELEN &&
                ability != null &&
                vaelen.position.value.hexDistanceTo(position) <= ability.range
        }

fun GameState.runeGlyphWardArmor(target: Attacker): Int =
    if (isProtectedByRuneGlyphWard(target)) {
        AttackerType.GRAND_RUNEMASTER_VAELEN.villainAbility?.magnitude ?: 0
    } else {
        0
    }
