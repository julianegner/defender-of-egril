package com.defenderofegril.model

data class Position(val x: Int, val y: Int) {
    fun distanceTo(other: Position): Int {
        // Use hexagonal distance for the game
        return hexDistanceTo(other)
    }
}
