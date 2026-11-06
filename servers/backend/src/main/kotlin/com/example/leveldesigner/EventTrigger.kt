package com.example.leveldesigner

import com.example.leveldesigner.model.Alternar

class EventTrigger {
    fun triggerEvent(alternar1: Alternar, alternar2: Alternar) {
        if (alternar1.isActive && alternar2.isActive) {
            displayLineBetweenHeads(alternar1, alternar2)
        }
    }

    private fun displayLineBetweenHeads(alternar1: Alternar, alternar2: Alternar) {
        // Code to display a line between the heads of the altars
    }
}