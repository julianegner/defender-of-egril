package de.egril.defender.model

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.ui.icon.enemy.enemyAttackPreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the attack-damage / lethality / immunity preview helpers used to show a preview
 * on enemy units when a defender is selected (issue #591).
 */
class AttackDamagePreviewTest {
    private fun defender(
        type: DefenderType,
        level: Int,
    ) = Defender(1, type, mutableStateOf(Position(0, 0)), mutableStateOf(level))

    private fun attacker(
        type: AttackerType,
        level: Int = 1,
    ) = Attacker(1, type, mutableStateOf(Position(1, 1)), mutableStateOf(level))

    @Test
    fun singleTargetDamageMatchesBaseDamageFormula() {
        // Bow tower level 1: baseDamage 10
        assertEquals(10, defender(DefenderType.BOW_TOWER, 1).previewAttackDamage())
        // Level 3: 10 + (3-1)*5 = 20
        assertEquals(20, defender(DefenderType.BOW_TOWER, 3).previewAttackDamage())
    }

    @Test
    fun lastingDamageIsHalvedPerHit() {
        // Alchemy tower level 1: baseDamage 15 -> per-hit 7
        assertEquals(7, defender(DefenderType.ALCHEMY_TOWER, 1).previewAttackDamage())
    }

    @Test
    fun nonAttackingTowerDealsZero() {
        assertEquals(0, defender(DefenderType.DWARVEN_MINE, 1).previewAttackDamage())
    }

    @Test
    fun doubleLevelBuffIncreasesDamage() {
        // Bow tower level 3 with double-level buff -> effective level 6: 10 + (6-1)*5 = 35
        assertEquals(35, defender(DefenderType.BOW_TOWER, 3).previewAttackDamage(hasDoubleLevelBuff = true))
    }

    @Test
    fun redDemonImmuneToFireballButNotToOthers() {
        val redDemon = attacker(AttackerType.RED_DEMON)
        assertTrue(redDemon.isImmuneToAttackFrom(DefenderType.WIZARD_TOWER)) // AREA / fireball
        assertFalse(redDemon.isImmuneToAttackFrom(DefenderType.BOW_TOWER)) // RANGED
        assertFalse(redDemon.isImmuneToAttackFrom(DefenderType.ALCHEMY_TOWER)) // LASTING / acid
    }

    @Test
    fun blueDemonImmuneToAcidButNotToFireball() {
        val blueDemon = attacker(AttackerType.BLUE_DEMON)
        assertTrue(blueDemon.isImmuneToAttackFrom(DefenderType.ALCHEMY_TOWER)) // LASTING / acid
        assertFalse(blueDemon.isImmuneToAttackFrom(DefenderType.WIZARD_TOWER)) // AREA / fireball
    }

    @Test
    fun regularEnemyNotImmune() {
        val goblin = attacker(AttackerType.GOBLIN)
        assertFalse(goblin.isImmuneToAttackFrom(DefenderType.WIZARD_TOWER))
        assertFalse(goblin.isImmuneToAttackFrom(DefenderType.ALCHEMY_TOWER))
        assertFalse(goblin.isImmuneToAttackFrom(DefenderType.BOW_TOWER))
    }

    @Test
    fun previewIsLethalWhenDamageMeetsOrExceedsHealth() {
        // Bow tower level 1 deals 10; goblin has 20 HP -> not lethal.
        val goblin = attacker(AttackerType.GOBLIN)
        val bow = defender(DefenderType.BOW_TOWER, 1)
        val nonLethal = enemyAttackPreview(goblin, bow, hasDoubleLevelBuff = false)
        assertEquals(10, nonLethal.damage)
        assertFalse(nonLethal.isLethal)
        assertFalse(nonLethal.isImmune)

        // Reduce goblin health to 10 -> exactly lethal.
        goblin.currentHealth.value = 10
        val lethal = enemyAttackPreview(goblin, bow, hasDoubleLevelBuff = false)
        assertTrue(lethal.isLethal)
    }

    @Test
    fun previewIsNeverLethalWhenImmune() {
        // Red Demon at 1 HP is immune to fireball, so the preview must show immunity, not lethal.
        val redDemon = attacker(AttackerType.RED_DEMON).also { it.currentHealth.value = 1 }
        val wizard = defender(DefenderType.WIZARD_TOWER, 1)
        val preview = enemyAttackPreview(redDemon, wizard, hasDoubleLevelBuff = false)
        assertTrue(preview.isImmune)
        assertFalse(preview.isLethal)
    }
}
