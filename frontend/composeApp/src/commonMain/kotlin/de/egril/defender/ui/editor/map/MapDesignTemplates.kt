package de.egril.defender.ui.editor.map

import de.egril.defender.editor.DEFAULT_MAP_TOOLING_INFO
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.MapTemplateDefinition

internal fun createMapFromTemplate(
    id: String,
    name: String,
    width: Int,
    height: Int,
    author: String,
    template: MapTemplateDefinition?,
): EditorMap {
    if (template == null) {
        val blankMap =
            EditorMap(
                id = id,
                name = name,
                width = width,
                height = height,
                tiles = emptyMap(),
                author = author,
                mapToolingInfo = DEFAULT_MAP_TOOLING_INFO,
            )
        return blankMap.copy(readyToUse = blankMap.validateReadyToUse())
    }

    val templateMap = template.templateMap
    val map =
        templateMap.copy(
            id = id,
            name = name,
            author = author,
            isOfficial = false,
            isCommunity = false,
            communityAuthorUsername = "",
            readyToUse = false,
            mapToolingInfo = DEFAULT_MAP_TOOLING_INFO,
        )
    return map.copy(readyToUse = map.validateReadyToUse())
}
