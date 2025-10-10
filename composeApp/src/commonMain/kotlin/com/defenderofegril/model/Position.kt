package com.defenderofegril.model

data class Position(val x: Int, val y: Int) {
    fun distanceTo(other: Position): Int {
        return kotlin.math.abs(x - other.x) + kotlin.math.abs(y - other.y)
    }
}
