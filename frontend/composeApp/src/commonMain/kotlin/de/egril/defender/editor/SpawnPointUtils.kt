package de.egril.defender.editor

import de.egril.defender.model.Position

/**
 * Utilities for spawn point remapping operations in the level editor.
 */
object SpawnPointUtils {
    
    /**
     * Computes the order in which remappings should be applied to avoid conflicts.
     * 
     * Example: If we have (7,15) -> (10,20) and (22,0) -> (7,15), we need to apply
     * (7,15) -> (10,20) first, otherwise all positions would end up at (10,20).
     * 
     * The algorithm uses topological sorting to determine the correct order.
     * 
     * @param remappings Map of position transformations (from -> to)
     * @return Ordered map of remappings that can be safely applied sequentially
     */
    fun computeRemappingOrder(remappings: Map<Position, Position>): Map<Position, Position> {
        // Filter out identity mappings (no change)
        val filteredMappings = remappings.filter { (from, to) -> from != to }
        
        if (filteredMappings.isEmpty()) {
            return emptyMap()
        }
        
        // Build dependency graph: if A -> B and C -> A, then A must be processed before C
        // (we need to move A away from its position before C can take it)
        val fromPositions = filteredMappings.keys.toSet()
        val toPositions = filteredMappings.values.toSet()
        
        // Find positions that are both source and target (these create dependencies)
        val conflictPositions = fromPositions.intersect(toPositions)
        
        if (conflictPositions.isEmpty()) {
            // No conflicts, can apply in any order
            return filteredMappings
        }
        
        // Topological sort: process nodes that are not targets first
        val result = mutableMapOf<Position, Position>()
        val processed = mutableSetOf<Position>()
        val remaining = filteredMappings.toMutableMap()
        
        // Keep processing until all mappings are handled
        while (remaining.isNotEmpty()) {
            // Find mappings where the "from" position is not a "to" position in remaining mappings
            val safeToProcess = remaining.filter { (from, _) ->
                from !in remaining.values || from in processed
            }
            
            if (safeToProcess.isEmpty()) {
                // Circular dependency detected - this shouldn't happen in our use case
                // but handle it by processing remaining mappings in arbitrary order
                result.putAll(remaining)
                break
            }
            
            // Add safe mappings to result
            result.putAll(safeToProcess)
            safeToProcess.keys.forEach { 
                processed.add(it)
                remaining.remove(it)
            }
        }
        
        return result
    }
}
