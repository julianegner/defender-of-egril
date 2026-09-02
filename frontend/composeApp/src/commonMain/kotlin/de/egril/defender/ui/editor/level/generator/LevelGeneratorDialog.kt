@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package de.egril.defender.ui.editor.level.generator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorMap
import de.egril.defender.model.AttackerType
import de.egril.defender.ui.getLocalizedDescription
import de.egril.defender.ui.getLocalizedName
import defender_of_egril.composeapp.generated.resources.*
import kotlin.math.roundToInt

private const val MIN_MAP_SIZE = 5
private const val MAX_MAP_SIZE = 100

/**
 * Dialog of the Level Generator. All generation inputs (difficulty, villains and the map to use)
 * are collected here and have to be chosen before the level is generated.
 */
@Composable
internal fun LevelGeneratorDialog(
    availableMaps: List<EditorMap>,
    defaultAuthor: String = "",
    isGenerating: Boolean = false,
    onDismiss: () -> Unit,
    onGenerate: (LevelGeneratorConfig) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf(defaultAuthor) }
    var difficulty by remember { mutableStateOf(GeneratorDifficulty.MEDIUM) }
    var difficultyExpanded by remember { mutableStateOf(false) }
    var selectedVillains by remember { mutableStateOf(emptySet<AttackerType>()) }
    var primaryRoster by remember { mutableStateOf(GeneratorEnemyRoster.HORDE) }
    var primaryRosterExpanded by remember { mutableStateOf(false) }
    var secondaryRoster by remember { mutableStateOf<GeneratorEnemyRoster?>(null) }
    var secondaryRosterExpanded by remember { mutableStateOf(false) }
    var mapSource by remember { mutableStateOf(GeneratorMapSource.GENERATED_MAP) }
    var mapSize by remember { mutableStateOf(GeneratedMapSize.MEDIUM) }
    var mapSizeExpanded by remember { mutableStateOf(false) }
    var mapWidth by remember { mutableStateOf(GeneratedMapSize.MEDIUM.width.toString()) }
    var mapHeight by remember { mutableStateOf(GeneratedMapSize.MEDIUM.height.toString()) }
    var landSpawnCountInput by remember { mutableStateOf("2") }
    var targetCountInput by remember { mutableStateOf("1") }
    var waterSpawnCountInput by remember { mutableStateOf("0") }
    var minPathWidthInput by remember { mutableStateOf("3") }
    var pathWindingFactor by remember { mutableStateOf(0.35f) }
    var waterLevel by remember { mutableStateOf(0.2f) }
    var requirePath by remember { mutableStateOf(true) }
    var selectedMap by remember { mutableStateOf(availableMaps.firstOrNull()) }
    var mapExpanded by remember { mutableStateOf(false) }

    val villainTypes = remember { AttackerType.entries.filter { it.isSelectableGeneratorVillain } }

    // Selecting villains pre-sets the rosters with their themes. They stay editable afterwards.
    LaunchedEffect(selectedVillains) {
        if (selectedVillains.isNotEmpty()) {
            val (primary, secondary) = rostersForVillains(selectedVillains)
            primaryRoster = primary
            secondaryRoster = secondary
        }
    }
    val width = mapWidth.toIntOrNull()
    val height = mapHeight.toIntOrNull()
    val landSpawnCount = landSpawnCountInput.toIntOrNull()
    val targetCount = targetCountInput.toIntOrNull()
    val waterSpawnCount = waterSpawnCountInput.toIntOrNull()
    val sizeIsValid = width != null && height != null && width in MIN_MAP_SIZE..MAX_MAP_SIZE && height in MIN_MAP_SIZE..MAX_MAP_SIZE
    val landSpawnCountIsValid = landSpawnCount != null && landSpawnCount >= 0
    val targetCountIsValid = targetCount != null && targetCount >= 1
    val waterSpawnCountIsValid = waterSpawnCount != null && waterSpawnCount >= 0
    val totalSpawnCountIsValid = (landSpawnCount ?: 0) + (waterSpawnCount ?: 0) >= 1
    val canGenerate =
        !isGenerating &&
            title.isNotBlank() &&
            if (mapSource == GeneratorMapSource.GENERATED_MAP) {
                sizeIsValid && landSpawnCountIsValid && targetCountIsValid && waterSpawnCountIsValid && totalSpawnCountIsValid
            } else {
                selectedMap != null
            }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.level_generator)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (isGenerating) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(Res.string.level_generator_generating),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Text(
                    text = stringResource(Res.string.level_generator_description),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(Res.string.level_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(Res.string.author_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )

                ExposedDropdownMenuBox(
                    expanded = difficultyExpanded,
                    onExpandedChange = { difficultyExpanded = it },
                ) {
                    OutlinedTextField(
                        value = difficulty.localizedLabel(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.difficulty)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = difficultyExpanded) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true),
                    )
                    ExposedDropdownMenu(
                        expanded = difficultyExpanded,
                        onDismissRequest = { difficultyExpanded = false },
                    ) {
                        GeneratorDifficulty.entries.forEach { entry ->
                            DropdownMenuItem(
                                text = { Text(entry.localizedLabel()) },
                                onClick = {
                                    difficulty = entry
                                    difficultyExpanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.villains),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(Res.string.level_generator_villains_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Column(
                    modifier = Modifier.height(150.dp).verticalScroll(rememberScrollState()),
                ) {
                    villainTypes.forEach { type ->
                        val checked = type in selectedVillains
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedVillains =
                                            if (checked) selectedVillains - type else selectedVillains + type
                                    }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    selectedVillains =
                                        if (checked) selectedVillains - type else selectedVillains + type
                                },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(type.getLocalizedName())
                                Text(
                                    text = type.getLocalizedDescription(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // Enemy rosters decide which regular enemies the waves are made of. Selecting
                // villains pre-sets them, but they stay editable.
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.level_generator_rosters_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                ExposedDropdownMenuBox(
                    expanded = primaryRosterExpanded,
                    onExpandedChange = { primaryRosterExpanded = it },
                ) {
                    OutlinedTextField(
                        value = primaryRoster.localizedLabel(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.level_generator_primary_roster)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = primaryRosterExpanded) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true),
                    )
                    ExposedDropdownMenu(
                        expanded = primaryRosterExpanded,
                        onDismissRequest = { primaryRosterExpanded = false },
                    ) {
                        GeneratorEnemyRoster.entries.forEach { entry ->
                            DropdownMenuItem(
                                text = { Text(entry.localizedLabel()) },
                                onClick = {
                                    primaryRoster = entry
                                    if (secondaryRoster == entry) secondaryRoster = null
                                    primaryRosterExpanded = false
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = secondaryRosterExpanded,
                    onExpandedChange = { secondaryRosterExpanded = it },
                ) {
                    OutlinedTextField(
                        value = secondaryRoster?.localizedLabel() ?: stringResource(Res.string.level_generator_roster_none),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.level_generator_secondary_roster)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = secondaryRosterExpanded) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true),
                    )
                    ExposedDropdownMenu(
                        expanded = secondaryRosterExpanded,
                        onDismissRequest = { secondaryRosterExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.level_generator_roster_none)) },
                            onClick = {
                                secondaryRoster = null
                                secondaryRosterExpanded = false
                            },
                        )
                        GeneratorEnemyRoster.entries.filter { it != primaryRoster }.forEach { entry ->
                            DropdownMenuItem(
                                text = { Text(entry.localizedLabel()) },
                                onClick = {
                                    secondaryRoster = entry
                                    secondaryRosterExpanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.level_generator_map_source),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { mapSource = GeneratorMapSource.GENERATED_MAP },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = mapSource == GeneratorMapSource.GENERATED_MAP,
                        onClick = { mapSource = GeneratorMapSource.GENERATED_MAP },
                    )
                    Text(stringResource(Res.string.level_generator_map_generated))
                }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { mapSource = GeneratorMapSource.EXISTING_MAP },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = mapSource == GeneratorMapSource.EXISTING_MAP,
                        onClick = { mapSource = GeneratorMapSource.EXISTING_MAP },
                        enabled = availableMaps.isNotEmpty(),
                    )
                    Text(stringResource(Res.string.level_generator_map_existing))
                }

                Spacer(modifier = Modifier.height(8.dp))
                if (mapSource == GeneratorMapSource.GENERATED_MAP) {
                    ExposedDropdownMenuBox(
                        expanded = mapSizeExpanded,
                        onExpandedChange = { mapSizeExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = mapSize.localizedLabel(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.level_generator_map_size)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mapSizeExpanded) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true),
                        )
                        ExposedDropdownMenu(
                            expanded = mapSizeExpanded,
                            onDismissRequest = { mapSizeExpanded = false },
                        ) {
                            GeneratedMapSize.entries.forEach { entry ->
                                DropdownMenuItem(
                                    text = { Text(entry.localizedLabel()) },
                                    onClick = {
                                        mapSize = entry
                                        mapWidth = entry.width.toString()
                                        mapHeight = entry.height.toString()
                                        mapSizeExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = mapWidth,
                            onValueChange = { mapWidth = it.filter { char -> char.isDigit() } },
                            label = { Text(stringResource(Res.string.width)) },
                            isError = !sizeIsValid,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = mapHeight,
                            onValueChange = { mapHeight = it.filter { char -> char.isDigit() } },
                            label = { Text(stringResource(Res.string.height)) },
                            isError = !sizeIsValid,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = stringResource(Res.string.level_generator_map_size_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.level_generator_parameters),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(Res.string.level_generator_count_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = landSpawnCountInput,
                            onValueChange = { landSpawnCountInput = it.filter(Char::isDigit) },
                            label = { Text(stringResource(Res.string.level_generator_land_spawn_count)) },
                            isError = !landSpawnCountIsValid || !totalSpawnCountIsValid,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = targetCountInput,
                            onValueChange = { targetCountInput = it.filter(Char::isDigit) },
                            label = { Text(stringResource(Res.string.level_generator_target_count)) },
                            isError = !targetCountIsValid,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedTextField(
                        value = waterSpawnCountInput,
                        onValueChange = { waterSpawnCountInput = it.filter(Char::isDigit) },
                        label = { Text(stringResource(Res.string.level_generator_water_spawn_count)) },
                        isError = !waterSpawnCountIsValid || !totalSpawnCountIsValid,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = minPathWidthInput,
                        onValueChange = { input ->
                            val digitsOnly = input.filter(Char::isDigit)
                            minPathWidthInput = digitsOnly.ifEmpty { "" }
                            if (digitsOnly.isNotEmpty() && digitsOnly.toInt() < 1) {
                                minPathWidthInput = "1"
                            }
                        },
                        label = { Text(stringResource(Res.string.level_generator_min_path_width)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${stringResource(Res.string.level_generator_winding_factor)}: ${(pathWindingFactor * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Slider(
                        value = pathWindingFactor,
                        onValueChange = { pathWindingFactor = it },
                        valueRange = 0f..1f,
                    )
                    Text(
                        text = "${stringResource(Res.string.level_generator_water_level)}: ${(waterLevel * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Slider(
                        value = waterLevel,
                        onValueChange = { waterLevel = it },
                        valueRange = 0f..1f,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(Res.string.level_generator_require_path),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Switch(
                            checked = requirePath,
                            onCheckedChange = { requirePath = it },
                        )
                    }
                } else {
                    ExposedDropdownMenuBox(
                        expanded = mapExpanded,
                        onExpandedChange = { mapExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedMap?.name.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.level_generator_select_map)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mapExpanded) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true),
                        )
                        ExposedDropdownMenu(
                            expanded = mapExpanded,
                            onDismissRequest = { mapExpanded = false },
                        ) {
                            availableMaps.forEach { map ->
                                DropdownMenuItem(
                                    text = { Text(map.name) },
                                    onClick = {
                                        selectedMap = map
                                        mapExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canGenerate,
                onClick = {
                    onGenerate(
                        LevelGeneratorConfig(
                            title = title.trim(),
                            author = author.trim(),
                            difficulty = difficulty,
                            villains = selectedVillains,
                            primaryRoster = primaryRoster,
                            secondaryRoster = secondaryRoster,
                            mapSource = mapSource,
                            existingMap = selectedMap.takeIf { mapSource == GeneratorMapSource.EXISTING_MAP },
                            mapSize = mapSize,
                            mapWidth = width ?: mapSize.width,
                            mapHeight = height ?: mapSize.height,
                            landSpawnCount = (landSpawnCountInput.toIntOrNull() ?: 0).coerceAtLeast(0),
                            targetCount = (targetCountInput.toIntOrNull() ?: 1).coerceAtLeast(1),
                            waterSpawnCount = (waterSpawnCountInput.toIntOrNull() ?: 0).coerceAtLeast(0),
                            minPathWidth = (minPathWidthInput.toIntOrNull() ?: 3).coerceAtLeast(1),
                            pathWindingFactor = pathWindingFactor,
                            waterLevel = waterLevel,
                            requirePath = requirePath,
                            seed = kotlin.random.Random.nextInt(),
                        ),
                    )
                },
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(Res.string.level_generator_generate))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss, enabled = !isGenerating) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

@Composable
private fun GeneratorDifficulty.localizedLabel(): String =
    when (this) {
        GeneratorDifficulty.EASY -> stringResource(Res.string.difficulty_easy)
        GeneratorDifficulty.MEDIUM -> stringResource(Res.string.difficulty_medium)
        GeneratorDifficulty.HARD -> stringResource(Res.string.difficulty_hard)
        GeneratorDifficulty.NIGHTMARE -> stringResource(Res.string.difficulty_nightmare)
    }

@Composable
private fun GeneratorEnemyRoster.localizedLabel(): String =
    when (this) {
        GeneratorEnemyRoster.HORDE -> stringResource(Res.string.level_generator_roster_horde)
        GeneratorEnemyRoster.UNDEAD -> stringResource(Res.string.level_generator_roster_undead)
        GeneratorEnemyRoster.DEMONS -> stringResource(Res.string.level_generator_roster_demons)
        GeneratorEnemyRoster.WITCHES -> stringResource(Res.string.level_generator_roster_witches)
        GeneratorEnemyRoster.PIRATES -> stringResource(Res.string.level_generator_roster_pirates)
        GeneratorEnemyRoster.SPIDERS -> stringResource(Res.string.level_generator_roster_spiders)
        GeneratorEnemyRoster.WILDS -> stringResource(Res.string.level_generator_roster_wilds)
    }

@Composable
private fun GeneratedMapSize.localizedLabel(): String {
    val name =
        when (this) {
            GeneratedMapSize.SMALL -> stringResource(Res.string.level_generator_size_small)
            GeneratedMapSize.MEDIUM -> stringResource(Res.string.level_generator_size_medium)
            GeneratedMapSize.LARGE -> stringResource(Res.string.level_generator_size_large)
            GeneratedMapSize.GIGANTIC -> stringResource(Res.string.level_generator_size_gigantic)
        }
    return "$name (${width}x$height)"
}
