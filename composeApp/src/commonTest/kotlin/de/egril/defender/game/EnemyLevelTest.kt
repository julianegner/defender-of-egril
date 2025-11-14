package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.PlannedEnemySpawn
import de.egril.defender.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test enemy level system to ensure levels are stored, displayed, and default to 1
 */
class EnemyLevelTest {
    
    @Test
    fun testEditorEnemySpawnDefaultLevel() {
        // Test that EditorEnemySpawn defaults to level 1 when not specified
        val spawn = EditorEnemySpawn(AttackerType.GOBLIN, spawnTurn = 1)
        assertEquals(1, spawn.level, "EditorEnemySpawn should default to level 1")
    }
    
    @Test
    fun testEditorEnemySpawnWithLevel() {
        // Test that EditorEnemySpawn stores the specified level
        val spawn = EditorEnemySpawn(AttackerType.GOBLIN, level = 3, spawnTurn = 1)
        assertEquals(3, spawn.level, "EditorEnemySpawn should store the specified level")
        
        // Health points should be multiplied by level
        assertEquals(AttackerType.GOBLIN.health * 3, spawn.healthPoints, 
            "EditorEnemySpawn health points should be base health * level")
    }
    
    @Test
    fun testPlannedEnemySpawnDefaultLevel() {
        // Test that PlannedEnemySpawn defaults to level 1 when not specified
        val spawn = PlannedEnemySpawn(AttackerType.ORK, spawnTurn = 1)
        assertEquals(1, spawn.level, "PlannedEnemySpawn should default to level 1")
    }
    
    @Test
    fun testPlannedEnemySpawnWithLevel() {
        // Test that PlannedEnemySpawn stores the specified level
        val spawn = PlannedEnemySpawn(AttackerType.ORK, spawnTurn = 1, level = 5)
        assertEquals(5, spawn.level, "PlannedEnemySpawn should store the specified level")
        
        // Health points should be multiplied by level
        assertEquals(AttackerType.ORK.health * 5, spawn.healthPoints, 
            "PlannedEnemySpawn health points should be base health * level")
    }
    
    @Test
    fun testAttackerDefaultLevel() {
        // Test that Attacker defaults to level 1 when not specified
        val attacker = Attacker(
            id = 1,
            type = AttackerType.OGRE,
            position = mutableStateOf(Position(0, 0))
        )
        assertEquals(1, attacker.level.value, "Attacker should default to level 1")
        assertEquals(AttackerType.OGRE.health, attacker.maxHealth, 
            "Attacker max health should be base health when level is 1")
    }
    
    @Test
    fun testAttackerWithLevel() {
        // Test that Attacker stores the specified level
        val attacker = Attacker(
            id = 1,
            type = AttackerType.OGRE,
            position = mutableStateOf(Position(0, 0)),
            level = mutableStateOf(4)
        )
        assertEquals(4, attacker.level.value, "Attacker should store the specified level")
        assertEquals(AttackerType.OGRE.health * 4, attacker.maxHealth, 
            "Attacker max health should be base health * level")
        assertEquals(AttackerType.OGRE.health * 4, attacker.currentHealth.value, 
            "Attacker current health should start at max health")
    }
    
    @Test
    fun testMultipleLevels() {
        // Test various level values for different enemy types
        val levels = listOf(1, 2, 3, 5, 10)
        val types = listOf(
            AttackerType.GOBLIN,
            AttackerType.ORK,
            AttackerType.SKELETON,
            AttackerType.EVIL_WIZARD,
            AttackerType.EWHAD
        )
        
        for (type in types) {
            for (level in levels) {
                val attacker = Attacker(
                    id = 1,
                    type = type,
                    position = mutableStateOf(Position(0, 0)),
                    level = mutableStateOf(level)
                )
                assertEquals(level, attacker.level.value, 
                    "Attacker ${type.displayName} should have level $level")
                assertEquals(type.health * level, attacker.maxHealth, 
                    "Attacker ${type.displayName} at level $level should have health ${type.health * level}")
            }
        }
    }
}
