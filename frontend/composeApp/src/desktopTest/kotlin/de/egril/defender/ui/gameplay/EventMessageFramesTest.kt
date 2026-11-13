package de.egril.defender.ui.gameplay

import de.egril.defender.model.AttackerType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies that every `message_background_*` drawable can be selected as an event message frame and
 * that the frames reuse the existing villain message configuration.
 */
class EventMessageFramesTest {
    private val drawableDir: File =
        run {
            val currentDir = File(System.getProperty("user.dir"))
            val root = if (currentDir.name == "composeApp") currentDir.parentFile else currentDir
            File(root, "composeApp/src/commonMain/composeResources/drawable")
        }

    private fun frameIdsFromDrawables(): List<String> =
        drawableDir
            .listFiles()
            .orEmpty()
            .map { it.nameWithoutExtension }
            .filter { it.startsWith("message_background_") }
            .map { it.removePrefix("message_background_") }
            // Compose resource keys normalize characters that are invalid in identifiers.
            .map { it.replace('-', '_') }
            .sorted()

    @Test
    fun testAllMessageBackgroundDrawablesAreOffered() {
        val expected = frameIdsFromDrawables()
        assertTrue(expected.isNotEmpty(), "There should be message background drawables")
        assertEquals(expected, EventMessageFrames.frameIds.sorted(), "All message frames should be selectable")
        assertEquals(
            EventMessageFrames.STANDARD_FRAME_ID,
            EventMessageFrames.frameIds.first(),
            "The standard story frame is listed first",
        )
    }

    @Test
    fun testStandardFrameUsesStoryLayoutAndDialogDefaults() {
        listOf(null, "", EventMessageFrames.STANDARD_FRAME_ID, "does_not_exist").forEach { frameId ->
            assertTrue(EventMessageFrames.isStandard(frameId), "'$frameId' should resolve to the standard frame")
            assertNull(EventMessageFrames.background(frameId), "The standard frame uses the dialog's own background")
            assertNull(EventMessageFrames.accentColor(frameId))
            assertNull(EventMessageFrames.styleAttackerType(frameId))
            assertEquals(NarrativeMessageType.STORY, EventMessageFrames.narrativeType(frameId))
        }
    }

    @Test
    fun testVillainFrameReusesVillainConfiguration() {
        assertEquals(NarrativeMessageType.EWHAD, EventMessageFrames.narrativeType("kraken"))
        assertNotNull(EventMessageFrames.background("kraken"))
        assertEquals(AttackerType.THE_KRAKEN, EventMessageFrames.styleAttackerType("kraken"))
        assertEquals(
            villainMessageButtonColor(AttackerType.THE_KRAKEN.name),
            EventMessageFrames.accentColor("kraken"),
            "The frame reuses the villain's message button colour",
        )
        assertEquals("Kraken", EventMessageFrames.label("kraken"))
    }

    @Test
    fun testFrameWithoutVillainStillUsable() {
        assertNotNull(EventMessageFrames.background("waaagh"), "Non-villain frames are usable too")
        assertNull(EventMessageFrames.styleAttackerType("waaagh"))
        assertEquals("Waaagh", EventMessageFrames.label("waaagh"))
        assertEquals("Obsidian Protector", EventMessageFrames.label("obsidian_protector"))
    }
}
