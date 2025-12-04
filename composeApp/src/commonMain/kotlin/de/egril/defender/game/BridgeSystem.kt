package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*

/**
 * Handles bridge building by enemy units.
 * 
 * Bridge building rules:
 * - Ork → Wooden bridge (1 river tile, HP = ork HP)
 * - Ogre → Stone bridge (1-2 river tiles, HP = ogre HP)
 * - Evil Wizard/Ewhad → Magical bridge (1 river tile, no HP, 3 turns duration, costs 1 level, must have level 2+)
 * 
 * Bridge-building units are destroyed (or lose level) when creating bridge.
 * Bridges don't count toward enemy count for winning.
 * Units on bridges when destroyed are also destroyed.
 */
class BridgeSystem(private val state: GameState) {
    
    private var nextBridgeId = 1
    
    /**
     * Check if an attacker should build a bridge based on strategic considerations.
     * - For Wizards (Evil Wizard/Ewhad): Always build if possible
     * - For others (Ork/Ogre): Only build if blocked OR creates shortcut of 10+ tiles
     */
    fun shouldAutoBuildBridge(attacker: Attacker): Boolean {
        if (!attacker.type.canBuildBridge) return false
        if (attacker.isDefeated.value || attacker.isBuildingBridge.value) return false
        
        // For Evil Wizard/Ewhad, must have level 2+
        if ((attacker.type == AttackerType.EVIL_WIZARD || attacker.type == AttackerType.EWHAD) && 
            attacker.level.value < 2) {
            return false
        }
        
        // Wizards always build bridges when at a river
        if (attacker.type == AttackerType.EVIL_WIZARD || attacker.type == AttackerType.EWHAD) {
            return canBuildBridge(attacker).isNotEmpty()
        }
        
        // For Orks and Ogres, check if bridge is strategically beneficial
        val bridgeablePositions = canBuildBridge(attacker)
        if (bridgeablePositions.isEmpty()) return false
        
        return isBridgeStrategic(attacker, bridgeablePositions)
    }
    
    /**
     * Determine if building a bridge is strategic for Orks/Ogres.
     * Bridge is strategic if:
     * 1. No path exists to target without the bridge (blocked), OR
     * 2. Bridge creates a shortcut of at least 10 tiles
     */
    private fun isBridgeStrategic(attacker: Attacker, bridgePositions: List<Position>): Boolean {
        val pathfinding = de.egril.defender.game.PathfindingSystem(state)
        val currentPos = attacker.position.value
        val target = attacker.currentTarget?.value ?: state.level.targetPositions.first()
        
        // Calculate current path length without bridge
        val pathWithoutBridge = pathfinding.findPath(currentPos, target, attacker)
        
        // If no path exists or path is very short (just 1-2 steps), we're blocked
        if (pathWithoutBridge.size <= 2) {
            return true  // Blocked - need bridge
        }
        
        // Temporarily add bridge to check new path length
        val tempBridge = Bridge(
            id = -1,  // Temporary ID
            type = if (attacker.type == AttackerType.OGRE) BridgeType.STONE else BridgeType.WOODEN,
            positions = bridgePositions,
            currentHealth = mutableStateOf(attacker.currentHealth.value),
            createdByAttackerId = attacker.id,
            createdOnTurn = state.turnNumber.value
        )
        state.bridges.add(tempBridge)
        
        // Calculate path with bridge
        val pathWithBridge = pathfinding.findPath(currentPos, target, attacker)
        
        // Remove temporary bridge
        state.bridges.remove(tempBridge)
        
        // Check if bridge creates a shortcut of at least 10 tiles
        val pathLengthWithout = pathWithoutBridge.size - 1  // -1 because path includes start position
        val pathLengthWith = pathWithBridge.size - 1
        val shortcut = pathLengthWithout - pathLengthWith
        
        return shortcut >= 10
    }
    
    /**
     * For wizards at multiple rivers, select which river to bridge based on proximity to target.
     */
    private fun selectBestRiverForWizard(attacker: Attacker, adjacentRivers: List<Position>): List<Position> {
        if (adjacentRivers.size <= 1) {
            return adjacentRivers.take(1)
        }
        
        val target = attacker.currentTarget?.value ?: state.level.targetPositions.first()
        
        // Select the river position closest to the target
        val bestRiver = adjacentRivers.minByOrNull { riverPos ->
            riverPos.distanceTo(target)
        }
        
        return listOfNotNull(bestRiver)
    }
    
    /**
     * Check if an attacker can build a bridge.
     * Returns the positions that can be bridged, or empty list if cannot build.
     * For wizards, selects best river if multiple are adjacent.
     */
    fun canBuildBridge(attacker: Attacker): List<Position> {
        if (!attacker.type.canBuildBridge) return emptyList()
        if (attacker.isDefeated.value || attacker.isBuildingBridge.value) return emptyList()
        
        // For Evil Wizard/Ewhad, must have level 2+ to sacrifice a level
        if ((attacker.type == AttackerType.EVIL_WIZARD || attacker.type == AttackerType.EWHAD) && 
            attacker.level.value < 2) {
            return emptyList()
        }
        
        val attackerPos = attacker.position.value
        
        // Find adjacent river tiles
        val adjacentRivers = attackerPos.getHexNeighbors().filter { pos ->
            state.level.isRiverTile(pos) && !state.isBridgeAt(pos)
        }
        
        if (adjacentRivers.isEmpty()) return emptyList()
        
        // For Wizards, select best river if multiple adjacent
        if (attacker.type == AttackerType.EVIL_WIZARD || attacker.type == AttackerType.EWHAD) {
            return selectBestRiverForWizard(attacker, adjacentRivers)
        }
        
        // For Ogres (stone bridge), check for 1-2 river tile spans
        if (attacker.type == AttackerType.OGRE) {
            // Check 1-tile spans
            val oneTileSpans = adjacentRivers
            
            // Check 2-tile spans (adjacent river → another adjacent river in same direction)
            val twoTileSpans = adjacentRivers.flatMap { firstRiver ->
                firstRiver.getHexNeighbors().filter { secondRiver ->
                    state.level.isRiverTile(secondRiver) && 
                    !state.isBridgeAt(secondRiver) &&
                    secondRiver != attackerPos &&
                    // Check if direction is roughly maintained (no sharp turns)
                    isRoughlyInSameDirection(attackerPos, firstRiver, secondRiver)
                }.map { listOf(firstRiver, it) }
            }
            
            return if (twoTileSpans.isNotEmpty()) {
                // Prefer 2-tile spans
                twoTileSpans.first()
            } else {
                // Fall back to 1-tile span
                oneTileSpans.take(1)
            }
        }
        
        // For Orks, Evil Wizards, Ewhad: only 1-tile spans
        return adjacentRivers.take(1)
    }
    
    /**
     * Check if three positions are roughly in the same direction (for ogre 2-tile bridges)
     */
    private fun isRoughlyInSameDirection(start: Position, mid: Position, end: Position): Boolean {
        // Check if the angle from start→mid and mid→end is relatively straight
        // Simple check: the end position should be further from start than mid is
        val startToMidDist = start.distanceTo(mid)
        val startToEndDist = start.distanceTo(end)
        return startToEndDist > startToMidDist
    }
    
    /**
     * Build a bridge at the specified positions.
     * Returns true if successful.
     */
    fun buildBridge(attacker: Attacker, positions: List<Position>): Boolean {
        if (positions.isEmpty()) return false
        if (!attacker.type.canBuildBridge) return false
        
        // Validate all positions are rivers, not already bridged, and no rafts present
        if (!positions.all { 
            state.level.isRiverTile(it) && 
            !state.isBridgeAt(it) &&
            !state.isRaftAt(it)  // Cannot build bridge where a raft is
        }) {
            return false
        }
        
        // Determine bridge type and create bridge
        when (attacker.type) {
            AttackerType.ORK -> {
                // Wooden bridge: 1 tile, HP = ork HP
                if (positions.size != 1) return false
                
                val bridge = Bridge(
                    id = nextBridgeId++,
                    type = BridgeType.WOODEN,
                    positions = positions,
                    currentHealth = mutableStateOf(attacker.currentHealth.value),
                    createdByAttackerId = attacker.id,
                    createdOnTurn = state.turnNumber.value
                )
                state.bridges.add(bridge)
                
                // Destroy the ork
                attacker.isBuildingBridge.value = true
                attacker.isDefeated.value = true
                println("Ork ${attacker.id} built wooden bridge at ${positions[0]} with ${bridge.currentHealth.value} HP")
                return true
            }
            
            AttackerType.OGRE -> {
                // Stone bridge: 1-2 tiles, HP = ogre HP
                if (positions.size !in 1..2) return false
                
                val bridge = Bridge(
                    id = nextBridgeId++,
                    type = BridgeType.STONE,
                    positions = positions,
                    currentHealth = mutableStateOf(attacker.currentHealth.value),
                    createdByAttackerId = attacker.id,
                    createdOnTurn = state.turnNumber.value
                )
                state.bridges.add(bridge)
                
                // Destroy the ogre
                attacker.isBuildingBridge.value = true
                attacker.isDefeated.value = true
                println("Ogre ${attacker.id} built stone bridge at $positions with ${bridge.currentHealth.value} HP")
                return true
            }
            
            AttackerType.EVIL_WIZARD, AttackerType.EWHAD -> {
                // Magical bridge: 1 tile, no HP, 3 turns, costs 1 level
                if (positions.size != 1) return false
                if (attacker.level.value < 2) return false  // Must have level 2+ to sacrifice
                
                val bridge = Bridge(
                    id = nextBridgeId++,
                    type = BridgeType.MAGICAL,
                    positions = positions,
                    currentHealth = mutableStateOf(0),  // No HP
                    turnsRemaining = mutableStateOf(3),
                    createdByAttackerId = attacker.id,
                    createdOnTurn = state.turnNumber.value
                )
                state.bridges.add(bridge)
                
                // Reduce level by 1
                attacker.level.value--
                // Update max health based on new level
                val newMaxHealth = attacker.type.health * attacker.level.value
                attacker.currentHealth.value = minOf(attacker.currentHealth.value, newMaxHealth)
                
                attacker.isBuildingBridge.value = true  // Mark as building (but NOT defeated - can still move)
                
                // Try to move onto the bridge if the wizard is adjacent to it
                val bridgePos = positions[0]
                if (attacker.position.value.getHexNeighbors().contains(bridgePos)) {
                    // Move wizard onto the bridge
                    attacker.position.value = bridgePos
                    println("${attacker.type} ${attacker.id} built magical bridge at ${bridgePos} and moved onto it, level ${attacker.level.value + 1} → ${attacker.level.value}")
                } else {
                    println("${attacker.type} ${attacker.id} built magical bridge at ${bridgePos}, level ${attacker.level.value + 1} → ${attacker.level.value}")
                }
                return true
            }
            
            else -> return false
        }
    }
    
    /**
     * Process bridges at the end of enemy turn:
     * - Decrement magical bridge turns
     * - Destroy expired/defeated bridges
     * - Destroy units on destroyed bridges
     */
    fun processBridges() {
        val bridgesToRemove = mutableListOf<Bridge>()
        
        for (bridge in state.bridges) {
            if (!bridge.isActive) {
                bridgesToRemove.add(bridge)
                continue
            }
            
            // Decrement magical bridge turns
            if (bridge.type == BridgeType.MAGICAL) {
                val expired = bridge.decrementTurn()
                if (expired) {
                    println("Magical bridge ${bridge.id} expired at ${bridge.positions}")
                    bridgesToRemove.add(bridge)
                    destroyUnitsOnBridge(bridge)
                }
            }
            
            // Check if bridge is destroyed by damage
            if (bridge.currentHealth.value <= 0 && bridge.type != BridgeType.MAGICAL) {
                bridge.isDestroyed.value = true
                println("${bridge.type} bridge ${bridge.id} destroyed at ${bridge.positions}")
                bridgesToRemove.add(bridge)
                destroyUnitsOnBridge(bridge)
            }
        }
        
        // Remove destroyed bridges
        state.bridges.removeAll(bridgesToRemove)
    }
    
    /**
     * Destroy all units standing on a bridge when it's destroyed
     */
    private fun destroyUnitsOnBridge(bridge: Bridge) {
        for (attacker in state.attackers) {
            if (!attacker.isDefeated.value && bridge.coversPosition(attacker.position.value)) {
                println("Unit ${attacker.type} ${attacker.id} destroyed by bridge collapse at ${attacker.position.value}")
                attacker.isDefeated.value = true
            }
        }
    }
    
    /**
     * Damage a bridge at a specific position.
     * Used when towers attack bridge positions.
     * All tiles of the bridge share the same health.
     */
    fun damageBridge(position: Position, damage: Int) {
        val bridge = state.getBridgeAt(position)
        if (bridge != null) {
            val destroyed = bridge.takeDamage(damage)
            println("Bridge ${bridge.id} at $position took $damage damage, remaining HP: ${bridge.currentHealth.value}")
            
            if (destroyed) {
                println("Bridge ${bridge.id} destroyed!")
                // Destroy any units on the bridge
                val unitsOnBridge = state.attackers.filter { attacker ->
                    bridge.coversPosition(attacker.position.value) && !attacker.isDefeated.value
                }
                for (unit in unitsOnBridge) {
                    unit.isDefeated.value = true
                    println("Unit ${unit.id} on bridge ${bridge.id} was destroyed!")
                }
            }
        }
    }
    
    /**
     * Automatically build a bridge for an attacker.
     * This is called when the attacker's path is blocked by a river.
     */
    fun autoBuildBridge(attacker: Attacker): Boolean {
        val bridgeablePositions = canBuildBridge(attacker)
        if (bridgeablePositions.isEmpty()) return false
        
        return buildBridge(attacker, bridgeablePositions)
    }
}
