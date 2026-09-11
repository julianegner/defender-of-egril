package de.egril.defender.ui.editor.level

import kotlin.test.Test
import kotlin.test.assertEquals

class LevelEditorTabIndicesTest {
    @Test
    fun sandboxTabIndicesStayContiguousWhenEnemyAndEventTabsAreHidden() {
        val indices = levelEditorTabIndices(isSandbox = true)

        assertEquals(null, indices.enemySpawns)
        assertEquals(null, indices.events)
        assertEquals(0, indices.levelInfo)
        assertEquals(1, indices.designPreview)
        assertEquals(2, indices.towers)
        assertEquals(3, indices.waypoints)
        assertEquals(4, indices.initialSetup)
        assertEquals(5, indices.supports)
    }

    @Test
    fun regularTabIndicesMatchFullEditorLayout() {
        val indices = levelEditorTabIndices(isSandbox = false)

        assertEquals(0, indices.levelInfo)
        assertEquals(1, indices.designPreview)
        assertEquals(2, indices.enemySpawns)
        assertEquals(3, indices.towers)
        assertEquals(4, indices.waypoints)
        assertEquals(5, indices.initialSetup)
        assertEquals(6, indices.supports)
        assertEquals(7, indices.events)
    }
}
