package de.egril.defender.ui.editor.level.placement

import androidx.compose.runtime.Composable
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.InitialData
import de.egril.defender.editor.TileType
import de.egril.defender.model.BridgeType
import de.egril.defender.model.Position
import de.egril.defender.ui.editor.level.initialsetup.getLocalizedName
import de.egril.defender.ui.getLocalizedName
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.barricade
import defender_of_egril.composeapp.generated.resources.dwarven_trap
import defender_of_egril.composeapp.generated.resources.gargoyle_name
import defender_of_egril.composeapp.generated.resources.gate
import defender_of_egril.composeapp.generated.resources.hp_with_level
import defender_of_egril.composeapp.generated.resources.initial_setup_portal_entry
import defender_of_egril.composeapp.generated.resources.initial_setup_portal_exit
import defender_of_egril.composeapp.generated.resources.level_label
import defender_of_egril.composeapp.generated.resources.magical_bridge
import defender_of_egril.composeapp.generated.resources.magical_trap
import defender_of_egril.composeapp.generated.resources.mushroom
import defender_of_egril.composeapp.generated.resources.spawn_point
import defender_of_egril.composeapp.generated.resources.stone_bridge
import defender_of_egril.composeapp.generated.resources.target_text
import defender_of_egril.composeapp.generated.resources.wooden_bridge

/**
 * Describes everything placed on [pos] for the placement-area tooltip, one entry per line.
 * Returns null when the tile is empty so no tooltip is shown.
 */
@Composable
internal fun placementTooltipText(
    initialData: InitialData,
    map: EditorMap,
    pos: Position,
): String? {
    val levelLabel = stringResource(Res.string.level_label)
    val lines = mutableListOf<String>()

    when (map.getTileType(pos.x, pos.y)) {
        TileType.SPAWN_POINT -> lines.add(stringResource(Res.string.spawn_point))
        TileType.TARGET -> lines.add(stringResource(Res.string.target_text))
        else -> Unit
    }
    for (d in initialData.defenders) {
        if (d.position == pos) lines.add("${d.type.getLocalizedName()} ($levelLabel ${d.level})")
    }
    for (a in initialData.attackers) {
        if (a.position == pos) lines.add("${a.type.getLocalizedName()} ($levelLabel ${a.level})")
    }
    for (t in initialData.traps) {
        if (t.position == pos) {
            val name = if (t.type == "MAGICAL") stringResource(Res.string.magical_trap) else stringResource(Res.string.dwarven_trap)
            lines.add(name)
        }
    }
    for (b in initialData.barricades) {
        if (b.position == pos) {
            val name = if (b.isGate) stringResource(Res.string.gate) else stringResource(Res.string.barricade)
            lines.add("$name (${stringResource(Res.string.hp_with_level, b.healthPoints)})")
        }
    }
    for (b in initialData.bridges) {
        if (b.position == pos) {
            val name =
                when (b.type) {
                    BridgeType.WOODEN -> stringResource(Res.string.wooden_bridge)
                    BridgeType.STONE -> stringResource(Res.string.stone_bridge)
                    BridgeType.MAGICAL -> stringResource(Res.string.magical_bridge)
                }
            lines.add("$name (${stringResource(Res.string.hp_with_level, b.healthPoints)})")
        }
    }
    for (f in initialData.fiefs) {
        if (f.position == pos) lines.add(f.type.getLocalizedName())
    }
    for (m in initialData.mushrooms) {
        if (m.position == pos) lines.add(stringResource(Res.string.mushroom))
    }
    for (g in initialData.gargoyles) {
        if (g.position == pos) lines.add("${stringResource(Res.string.gargoyle_name)} ($levelLabel ${g.level})")
    }
    for (p in initialData.portals) {
        if (p.entryPosition == pos) lines.add(stringResource(Res.string.initial_setup_portal_entry))
        if (p.exitPosition == pos) lines.add(stringResource(Res.string.initial_setup_portal_exit))
    }

    return lines.takeIf { it.isNotEmpty() }?.joinToString("\n")
}
