package de.egril.defender.model

/**
 * Pending barge (raft + defender) deletion from Roderich's Broadside attack.
 * The barge is removed after the cannonball animation completes.
 */
data class PendingBargeDeletion(
    val raftId: Int,
    val defenderId: Int,
    val towerCost: Int, // Cost to add to Roderich's treasure
    val bargePosition: Position, // For logging
)
