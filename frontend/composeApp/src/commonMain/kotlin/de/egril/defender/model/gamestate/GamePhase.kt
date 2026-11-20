package de.egril.defender.model

enum class GamePhase {
    INITIAL_BUILDING, // Initial building phase - towers build instantly
    PLAYER_TURN, // Player can place/upgrade towers and attack
    ENEMY_TURN, // Enemies move
}
