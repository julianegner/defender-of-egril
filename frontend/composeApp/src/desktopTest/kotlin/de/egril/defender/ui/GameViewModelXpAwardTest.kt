package de.egril.defender.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GameViewModelXpAwardTest {
    @Test
    fun calculateAwardedXpForLevelCompletion_returnsFullXpWhenWon() {
        assertEquals(120, GameViewModel.calculateAwardedXpForLevelCompletion(rawXpEarned = 120, won = true))
    }

    @Test
    fun calculateAwardedXpForLevelCompletion_returnsTwentyPercentWhenLost() {
        assertEquals(24, GameViewModel.calculateAwardedXpForLevelCompletion(rawXpEarned = 120, won = false))
    }
}
