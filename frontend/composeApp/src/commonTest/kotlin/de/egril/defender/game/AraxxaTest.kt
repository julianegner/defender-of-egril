package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import de.egril.defender.model.isSummoner
import de.egril.defender.model.isUniqueEnemyAlreadyPresent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for Araxxa the Giant Spider villain, her spiderlings, and the spreading web area.
 */
class AraxxaTest {
    private fun createOpenLevel(): Level {
        val allCells = (0 until 12).flatMap { x -> (0 until 8).map { y -> Position(x, y) } }.toSet()
        return Level(
            id = 1,
            name = "Araxxa Test Level",
            gridWidth = 12,
            gridHeight = 8,
            startPositions = listOf(Position(0, 4)),
            targetPositions = listOf(Position(11, 4)),
            pathCells = allCells,
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10,
        )
    }

    private fun createCombatLevel(): Level =
        Level(
            id = 1,
            name = "Araxxa Combat Test",
            gridWidth = 10,
            gridHeight = 6,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(9, 3)),
            pathCells = (0..9).map { Position(it, 3) }.toSet(),
            buildAreas = setOf(Position(2, 1), Position(2, 2)),
            attackerWaves = emptyList(),
            initialCoins = 1000,
            healthPoints = 10,
        )

    private fun createAraxxaSpawnLevel(): Level {
        val pathCells = (1..9).map { Position(it, 3) }.toSet()
        return Level(
            id = 2,
            name = "Araxxa Spawn Test",
            gridWidth = 12,
            gridHeight = 8,
            startPositions = listOf(Position(0, 3)),
            targetPositions = listOf(Position(10, 3)),
            pathCells = pathCells,
            attackerWaves = emptyList(),
            directSpawnPlan =
                listOf(
                    PlannedEnemySpawn(
                        attackerType = AttackerType.ARAXXA,
                        spawnTurn = 1,
                        level = 1,
                        spawnPoint = Position(0, 3),
                    ),
                ),
            initialCoins = 1000,
            healthPoints = 10,
        )
    }

    @Test
    fun araxxaIsConfiguredAsVillainAndSummoner() {
        val type = AttackerType.ARAXXA
        assertTrue(type.isVillain, "Araxxa should be a villain")
        assertTrue(type.isBoss, "Araxxa should be a boss")
        assertTrue(type.canSummon, "Araxxa should summon spiderlings")
        assertTrue(type.isSummoner(), "Araxxa must count as a summoner")
        assertEquals("Araxxa", type.villainName, "Araxxa's short villain name should live in the enum")
        assertTrue(isUniqueEnemyAlreadyPresent(type, listOf(Attacker(1, type, mutableStateOf(Position(5, 4))))))
    }

    @Test
    fun araxxaSpawnsSpiderlingsOnAdjacentTiles() {
        val state = GameState(createOpenLevel())
        val araxxaPos = Position(5, 4)
        val araxxa =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.ARAXXA,
                position = mutableStateOf(araxxaPos),
                level = mutableStateOf(2),
            )
        state.attackers.add(araxxa)

        EnemyAbilitySystem(state).processEnemyAbilities()

        val spiderlings = state.attackers.filter { it.type == AttackerType.SPIDERLING && !it.isDefeated.value }
        assertTrue(spiderlings.isNotEmpty(), "Araxxa should spawn spiderlings")
        spiderlings.forEach { spiderling ->
            assertEquals(1, spiderling.position.value.distanceTo(araxxaPos), "Spiderlings should spawn adjacent to Araxxa")
            assertEquals(2, spiderling.level.value, "Spiderlings should inherit Araxxa's level")
        }
        assertEquals(3, araxxa.summonCooldown.value, "Spiderling summon should start a cooldown")
    }

    @Test
    fun webSpreadsAndBuffsSpidersInsideIt() {
        val state = GameState(createOpenLevel())
        val araxxaPos = Position(5, 4)
        val araxxa =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.ARAXXA,
                position = mutableStateOf(araxxaPos),
            )
        val spiderling =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SPIDERLING,
                position = mutableStateOf(araxxaPos),
            )
        val goblin =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(araxxaPos),
            )
        state.attackers.addAll(listOf(araxxa, spiderling, goblin))

        val abilities = EnemyAbilitySystem(state)
        abilities.processEnemyAbilities()
        val firstWebCount = state.fieldEffects.count { it.type == FieldEffectType.WEB }
        val webAtDistanceTwo =
            state.fieldEffects.count {
                it.type == FieldEffectType.WEB && it.position.hexDistanceTo(araxxaPos) == 2
            }
        val webDurations = state.fieldEffects.filter { it.type == FieldEffectType.WEB }.map { it.turnsRemaining }

        assertTrue(firstWebCount >= 7, "Araxxa should web her own tile and nearby path tiles")
        assertTrue(webAtDistanceTwo > 0, "Stationary Araxxa should also web tiles at distance 2")
        assertTrue(state.fieldEffects.any { it.type == FieldEffectType.WEB && it.position == araxxaPos }, "Araxxa's tile should always be webbed")
        assertTrue(webDurations.all { it == 10 }, "Araxxa web tiles should last 10 turns when created")
        assertEquals(1, araxxa.speedBonus.value, "Araxxa should gain a speed bonus while in her web")
        assertEquals(1, spiderling.speedBonus.value, "Spiderlings inside the web should gain a speed bonus")
        assertEquals(0, goblin.speedBonus.value, "Non-spider enemies must not gain the web bonus")
    }

    @Test
    fun fireAndAcidDestroySpiderWeb() {
        val level = createCombatLevel()

        run {
            val state = GameState(level)
            val engine = GameEngine(state)
            val araxxa =
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = AttackerType.ARAXXA,
                    position = mutableStateOf(Position(4, 3)),
                )
            state.attackers.add(araxxa)
            EnemyAbilitySystem(state).processEnemyAbilities()

            assertTrue(engine.placeDefender(DefenderType.WIZARD_TOWER, Position(2, 1)))
            val wizard = state.defenders.first()
            wizard.buildTimeRemaining.value = 0
            engine.startFirstPlayerTurn()
            wizard.resetActions()

            assertTrue(state.fieldEffects.any { it.type == FieldEffectType.WEB && it.position == araxxa.position.value })
            assertTrue(engine.defenderAttackPosition(wizard.id, araxxa.position.value))
            assertFalse(state.fieldEffects.any { it.type == FieldEffectType.WEB && it.position == araxxa.position.value }, "Fire should destroy web tiles it hits")
        }

        run {
            val state = GameState(level)
            val engine = GameEngine(state)
            val araxxa =
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = AttackerType.ARAXXA,
                    position = mutableStateOf(Position(3, 3)),
                )
            state.attackers.add(araxxa)
            EnemyAbilitySystem(state).processEnemyAbilities()

            assertTrue(engine.placeDefender(DefenderType.ALCHEMY_TOWER, Position(2, 1)))
            val alchemy = state.defenders.first()
            alchemy.buildTimeRemaining.value = 0
            engine.startFirstPlayerTurn()
            alchemy.resetActions()

            assertTrue(state.fieldEffects.any { it.type == FieldEffectType.WEB && it.position == araxxa.position.value })
            assertTrue(engine.defenderAttackPosition(alchemy.id, araxxa.position.value))
            assertFalse(state.fieldEffects.any { it.type == FieldEffectType.WEB && it.position == araxxa.position.value }, "Acid should destroy web tiles it hits")
        }
    }

    @Test
    fun spiderlingsMergeLikeSnotlings() {
        val state = GameState(createOpenLevel())
        val engine = GameEngine(state)
        val first =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SPIDERLING,
                position = mutableStateOf(Position(3, 4)),
            )
        val second =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SPIDERLING,
                position = mutableStateOf(Position(4, 4)),
            )
        state.attackers.addAll(listOf(first, second))

        engine.applyMovement(first.id, second.position.value)

        assertTrue(first.isDefeated.value, "Moving spiderling should be absorbed into the existing stack")
        assertTrue(first.wasMerged.value, "Merged spiderlings should not count as real kills")
        assertEquals(10, second.currentHealth.value, "Spiderling stacks should merge their health like snotlings")
    }

    @Test
    fun araxxaWebCoversOnlyDistanceOneAfterMoving() {
        val state = GameState(createOpenLevel())
        val araxxaStart = Position(3, 3)
        val araxxaEnd = Position(4, 3)
        val araxxa =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.ARAXXA,
                position = mutableStateOf(araxxaEnd),
            )
        state.attackers.add(araxxa)
        state.enemyTurnStartPositions[araxxa.id] = araxxaStart

        EnemyAbilitySystem(state).processEnemyAbilities()

        val distanceTwoTiles =
            state.fieldEffects.filter {
                it.type == FieldEffectType.WEB && it.position.hexDistanceTo(araxxaEnd) == 2
            }
        assertEquals(0, distanceTwoTiles.size, "Araxxa should not create distance-2 web when she moved this turn")
    }

    @Test
    fun spiderlingsUseNearbyFallbackLikeSnotlings() {
        val state = GameState(createOpenLevel())
        val araxxaPos = Position(5, 4)
        val araxxa =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.ARAXXA,
                position = mutableStateOf(araxxaPos),
            )
        state.attackers.add(araxxa)

        araxxaPos.getHexNeighbors().forEachIndexed { index, neighbor ->
            state.attackers.add(
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = if (index == 0) AttackerType.SPIDERLING else AttackerType.GOBLIN,
                    position = mutableStateOf(neighbor),
                ),
            )
        }

        EnemyAbilitySystem(state).processEnemyAbilities()

        val summonedSpiderlings =
            state.attackers.filter {
                it.type == AttackerType.SPIDERLING && !it.isDefeated.value && it.id != araxxa.id
            }
        assertTrue(summonedSpiderlings.size > 1, "Araxxa should still summon spiderlings when adjacent tiles are blocked")
        assertTrue(
            summonedSpiderlings.any { it.position.value.hexDistanceTo(araxxaPos) > 1 },
            "Blocked adjacent spawns should fall back to nearby valid tiles",
        )
    }

    @Test
    fun araxxaAvoidsBarricadesForWebAndSpiderlingSpawns() {
        val state = GameState(createOpenLevel())
        val araxxaPos = Position(5, 4)
        val neighbors = araxxaPos.getHexNeighbors()
        val stackTile = neighbors.first()
        val barricadeTile = neighbors[1]
        val araxxa =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.ARAXXA,
                position = mutableStateOf(araxxaPos),
            )
        state.attackers.add(araxxa)
        state.attackers.add(
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SPIDERLING,
                position = mutableStateOf(stackTile),
            ),
        )
        state.barricades.add(
            Barricade(
                id = 1,
                position = barricadeTile,
                healthPoints = mutableStateOf(120),
                defenderId = 0,
            ),
        )
        neighbors.drop(2).forEach { blocked ->
            state.attackers.add(
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = AttackerType.GOBLIN,
                    position = mutableStateOf(blocked),
                ),
            )
        }
        barricadeTile
            .getHexNeighbors()
            .filter { it != araxxaPos && it != stackTile }
            .forEach { blocked ->
                state.attackers.add(
                    Attacker(
                        id = state.nextAttackerId.value++,
                        type = AttackerType.GOBLIN,
                        position = mutableStateOf(blocked),
                    ),
                )
            }

        EnemyAbilitySystem(state).processEnemyAbilities()

        assertFalse(
            state.fieldEffects.any { it.type == FieldEffectType.WEB && it.position == barricadeTile },
            "Araxxa must not place web on a barricade tile",
        )
        assertFalse(
            state.attackers.any {
                it.type == AttackerType.SPIDERLING && !it.isDefeated.value && it.position.value == barricadeTile
            },
            "Spiderlings must not spawn on a barricade tile",
        )
        assertTrue(
            state.attackers.count {
                it.type == AttackerType.SPIDERLING && !it.isDefeated.value && it.position.value == stackTile
            } > 1,
            "Blocked spiderling spawns should be redirected to valid spiderling stacks",
        )
    }

    @Test
    fun araxxaSummonsOneSpiderlingPerAdjacentTileEvenWhenBlocked() {
        val araxxaPos = Position(5, 4)
        val neighbors = araxxaPos.getHexNeighbors()
        val stackTile = neighbors.first()
        val traversableTiles = (neighbors + araxxaPos).toSet()
        val level =
            Level(
                id = 3,
                name = "Araxxa Blocked Summon Test",
                gridWidth = 12,
                gridHeight = 8,
                startPositions = listOf(Position(0, 4)),
                targetPositions = listOf(Position(11, 4)),
                pathCells = traversableTiles,
                attackerWaves = emptyList(),
                initialCoins = 1000,
                healthPoints = 10,
            )
        val state = GameState(level)
        val araxxa =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.ARAXXA,
                position = mutableStateOf(araxxaPos),
            )
        state.attackers.add(araxxa)
        state.attackers.add(
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SPIDERLING,
                position = mutableStateOf(stackTile),
            ),
        )
        neighbors.drop(1).forEach { blocked ->
            state.attackers.add(
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = AttackerType.GOBLIN,
                    position = mutableStateOf(blocked),
                ),
            )
        }

        EnemyAbilitySystem(state).processEnemyAbilities()

        assertEquals(
            7,
            state.attackers.count {
                it.type == AttackerType.SPIDERLING && !it.isDefeated.value && it.position.value == stackTile
            },
            "Araxxa should summon one spiderling per adjacent tile, redirecting blocked spawns onto valid spiderling stacks",
        )
    }

    @Test
    fun araxxaSpawnTurnSummonsAfterMovingAndUsesMovedWebRadius() {
        val state = GameState(createAraxxaSpawnLevel())
        val engine = GameEngine(state)

        engine.startFirstPlayerTurn()
        engine.startEnemyTurn()
        engine.spawnEnemyTurnAttackers()
        val newSpawnMovements = engine.calculateNewlySpawnedMovements()
        for (stepMovements in newSpawnMovements) {
            for ((attackerId, newPosition) in stepMovements) {
                engine.applyMovement(attackerId, newPosition)
            }
        }
        engine.completeEnemyTurn()

        val araxxa = state.attackers.first { it.type == AttackerType.ARAXXA && !it.isDefeated.value }
        assertTrue(araxxa.position.value != Position(0, 3), "Araxxa should move away from her spawn tile on entry turn")
        assertTrue(
            state.attackers.any { it.type == AttackerType.SPIDERLING && !it.isDefeated.value },
            "Araxxa should summon spiderlings on the same turn she enters and moves",
        )
        assertEquals(
            0,
            state.fieldEffects.count {
                it.type == FieldEffectType.WEB && it.position.hexDistanceTo(araxxa.position.value) == 2
            },
            "After moving on her entry turn, Araxxa should only create distance-1 web tiles",
        )
    }

    @Test
    fun swarmUnitsDealBarricadeDamageFromCurrentHealth() {
        val state = GameState(createOpenLevel())
        val engine = GameEngine(state)

        val snotling =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SNOTLING,
                position = mutableStateOf(Position(2, 2)),
            )
        snotling.currentHealth.value = 24
        val spiderling =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.SPIDERLING,
                position = mutableStateOf(Position(2, 3)),
            )
        spiderling.currentHealth.value = 4

        assertEquals(4, engine.getBarricadeDamageForEnemyUnit(snotling), "Snotlings should deal floor(HP/5) barricade damage")
        assertEquals(1, engine.getBarricadeDamageForEnemyUnit(spiderling), "Spiderlings should deal at least 1 barricade damage")
    }
}
