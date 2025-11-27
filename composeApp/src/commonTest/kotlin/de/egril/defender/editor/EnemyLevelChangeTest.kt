package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test for enemy level changes in the level editor
 */
class EnemyLevelChangeTest {
    
    @Test
    fun testEnemySpawnLevelChange() {
        // Create an enemy spawn with level 1
        val originalSpawn = EditorEnemySpawn(
            attackerType = AttackerType.GOBLIN,
            level = 1,
            spawnTurn = 1,
            spawnPoint = Position(0, 0)
        )
        
        // Verify initial health points
        assertEquals(AttackerType.GOBLIN.health * 1, originalSpawn.healthPoints)
        assertEquals(1, originalSpawn.level)
        
        // Change level to 5
        val updatedSpawn = originalSpawn.copy(level = 5)
        
        // Verify new level and health points
        assertEquals(5, updatedSpawn.level)
        assertEquals(AttackerType.GOBLIN.health * 5, updatedSpawn.healthPoints)
        
        // Verify other properties remain unchanged
        assertEquals(originalSpawn.attackerType, updatedSpawn.attackerType)
        assertEquals(originalSpawn.spawnTurn, updatedSpawn.spawnTurn)
        assertEquals(originalSpawn.spawnPoint, updatedSpawn.spawnPoint)
    }
    
    @Test
    fun testMultipleEnemyLevelChanges() {
        // Create a list of enemy spawns
        val spawns = mutableListOf(
            EditorEnemySpawn(AttackerType.GOBLIN, level = 1, spawnTurn = 1, spawnPoint = Position(0, 0)),
            EditorEnemySpawn(AttackerType.ORK, level = 2, spawnTurn = 1, spawnPoint = Position(0, 0)),
            EditorEnemySpawn(AttackerType.OGRE, level = 1, spawnTurn = 2, spawnPoint = Position(1, 1))
        )
        
        // Change level of first spawn
        val targetSpawn = spawns[0]
        val newLevel = 3
        
        val updatedSpawns = spawns.map { spawn ->
            if (spawn === targetSpawn) {
                spawn.copy(level = newLevel)
            } else {
                spawn
            }
        }
        
        // Verify only the first spawn was changed
        assertEquals(newLevel, updatedSpawns[0].level)
        assertEquals(AttackerType.GOBLIN.health * newLevel, updatedSpawns[0].healthPoints)
        assertEquals(2, updatedSpawns[1].level)  // unchanged
        assertEquals(1, updatedSpawns[2].level)  // unchanged
    }
    
    @Test
    fun testHealthPointsCalculationForDifferentEnemyTypes() {
        val enemyTypes = listOf(
            AttackerType.GOBLIN,
            AttackerType.ORK,
            AttackerType.OGRE,
            AttackerType.SKELETON,
            AttackerType.EVIL_WIZARD
        )
        
        for (enemyType in enemyTypes) {
            for (level in 1..10) {
                val spawn = EditorEnemySpawn(
                    attackerType = enemyType,
                    level = level,
                    spawnTurn = 1
                )
                
                val expectedHP = enemyType.health * level
                assertEquals(
                    expectedHP, 
                    spawn.healthPoints,
                    "HP calculation incorrect for $enemyType at level $level"
                )
            }
        }
    }
}
