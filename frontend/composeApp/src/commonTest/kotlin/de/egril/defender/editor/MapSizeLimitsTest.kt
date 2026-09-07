package de.egril.defender.editor

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MapSizeLimitsTest {
    @Test
    fun oversizedHeaderMapIsMarkedInvalid() {
        val json = """{
  "metadata": {"program": "Defender of Egril", "type": "map"},
  "data": {
    "id": "oversized_header",
    "name": "Oversized Header",
    "width": 501,
    "height": 500,
    "readyToUse": true,
    "isOfficial": false,
    "tiles": {
      "0,0": "SPAWN_POINT",
      "500,499": "TARGET"
    }
  }
}"""

        val map = EditorJsonSerializer.deserializeMap(json)

        assertNotNull(map)
        assertFalse(map.isValid)
        assertFalse(map.readyToUse)
        assertTrue(map.tiles.isEmpty())
    }

    @Test
    fun outOfBoundsTileMarksMapInvalidAndStopsParsing() {
        val json = """{
  "metadata": {"program": "Defender of Egril", "type": "map"},
  "data": {
    "id": "out_of_bounds_tile",
    "name": "Out Of Bounds Tile",
    "width": 3,
    "height": 3,
    "readyToUse": true,
    "isOfficial": false,
    "tiles": {
      "0,0": "SPAWN_POINT",
      "3,0": "PATH",
      "2,2": "TARGET"
    },
    "targetInfo": {
      "2,2": {"name": "Should Not Be Parsed", "type": "STANDARD"}
    }
  }
}"""

        val map = EditorJsonSerializer.deserializeMap(json)

        assertNotNull(map)
        assertFalse(map.isValid)
        assertFalse(map.readyToUse)
        assertTrue(map.tiles.isEmpty())
        assertTrue(map.targetInfoMap.isEmpty())
    }
}
