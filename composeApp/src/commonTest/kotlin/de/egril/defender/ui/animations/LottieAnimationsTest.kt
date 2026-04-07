package de.egril.defender.ui.animations

import io.github.alexzhirkevich.compottie.Compottie
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for Lottie animation integration
 */
class LottieAnimationsTest {
    
    @Test
    fun testAnimationTypeEnumExists() {
        // Verify that animation types are defined correctly
        val healingType = AnimationType.GREEN_WITCH_HEALING
        val damageType = AnimationType.BARRICADE_DAMAGE
        val freezeType = AnimationType.FREEZE_SPELL
        val doubleLevelType = AnimationType.DOUBLE_LEVEL_SPELL
        val instantTowerType = AnimationType.INSTANT_TOWER_SPELL

        val fearType = AnimationType.FEAR_SPELL
        val waterFlowType = AnimationType.WATER_FLOW
        val enemyDeathType = AnimationType.ENEMY_DEATH
        val towerReadyPulseType = AnimationType.TOWER_READY_PULSE
        val coinGainType = AnimationType.COIN_GAIN
        val towerAttackImpactType = AnimationType.TOWER_ATTACK_IMPACT
        val towerConstructionCompleteType = AnimationType.TOWER_CONSTRUCTION_COMPLETE
        val enemySpawnType = AnimationType.ENEMY_SPAWN
        val trapTriggerType = AnimationType.TRAP_TRIGGER
        val enemyMoveType = AnimationType.ENEMY_MOVE
        val dragonLevelUpType = AnimationType.DRAGON_LEVEL_UP
        val dragonLevelDownType = AnimationType.DRAGON_LEVEL_DOWN
        val wizardIdleType = AnimationType.WIZARD_IDLE
        val alchemyIdleType = AnimationType.ALCHEMY_IDLE
        val mineDigType = AnimationType.MINE_DIG
        val arrowAttackType = AnimationType.ARROW_ATTACK
        val dragonTargetType = AnimationType.DRAGON_TARGET

        assertNotNull(healingType, "Green witch healing animation type should exist")
        assertNotNull(damageType, "Barricade damage animation type should exist")
        assertNotNull(freezeType, "Freeze spell animation type should exist")
        assertNotNull(doubleLevelType, "Double level spell animation type should exist")
        assertNotNull(instantTowerType, "Instant tower spell animation type should exist")
        assertNotNull(fearType, "Fear spell animation type should exist")
        assertNotNull(waterFlowType, "Water flow animation type should exist")
        assertNotNull(enemyDeathType, "Enemy death animation type should exist")
        assertNotNull(towerReadyPulseType, "Tower ready pulse animation type should exist")
        assertNotNull(coinGainType, "Coin gain animation type should exist")
        assertNotNull(towerAttackImpactType, "Tower attack impact animation type should exist")
        assertNotNull(towerConstructionCompleteType, "Tower construction complete animation type should exist")
        assertNotNull(enemySpawnType, "Enemy spawn animation type should exist")
        assertNotNull(trapTriggerType, "Trap trigger animation type should exist")
        assertNotNull(enemyMoveType, "Enemy move trail animation type should exist")
        assertNotNull(dragonLevelUpType, "Dragon level up animation type should exist")
        assertNotNull(dragonLevelDownType, "Dragon level down animation type should exist")
        assertNotNull(wizardIdleType, "Wizard idle animation type should exist")
        assertNotNull(alchemyIdleType, "Alchemy idle animation type should exist")
        assertNotNull(mineDigType, "Mine dig animation type should exist")
        assertNotNull(arrowAttackType, "Arrow attack animation type should exist")
        assertNotNull(dragonTargetType, "Dragon target animation type should exist")
    }

    @Test
    fun testCompottieIterateForeverIsMaxInt() {
        assertEquals(Int.MAX_VALUE, Compottie.IterateForever,
            "Compottie.IterateForever should equal Int.MAX_VALUE")
    }
    
    @Test
    fun testAnimationTypeCount() {
        // Verify we have exactly 23 animation types
        val animationTypes = AnimationType.values()
        assertEquals(23, animationTypes.size, "Should have exactly 23 animation types")
    }
}
