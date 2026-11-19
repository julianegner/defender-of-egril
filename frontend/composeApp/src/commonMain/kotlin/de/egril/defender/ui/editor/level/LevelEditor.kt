package de.egril.defender.ui.editor.level

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.*
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.editor.ConfirmationDialog
import de.egril.defender.ui.editor.CreateLevelDialog
import de.egril.defender.ui.editor.getDefaultAuthorName
import de.egril.defender.ui.editor.level.generator.LevelGenerator
import de.egril.defender.ui.editor.level.generator.LevelGeneratorConfig
import de.egril.defender.ui.editor.level.generator.LevelGeneratorDialog
import defender_of_egril.composeapp.generated.resources.*
import kotlinx.coroutines.delay
import kotlin.random.Random

internal data class LevelEditorTabIndices(
    val levelInfo: Int,
    val designPreview: Int,
    val enemySpawns: Int?,
    val towers: Int,
    val waypoints: Int,
    val initialSetup: Int,
    val placementArea: Int,
    val supports: Int,
    val events: Int?,
)

internal fun levelEditorTabIndices(isSandbox: Boolean): LevelEditorTabIndices {
    var nextIndex = 0
    val levelInfo = nextIndex++
    val designPreview = nextIndex++
    val enemySpawns = if (isSandbox) null else nextIndex++
    val towers = nextIndex++
    val waypoints = nextIndex++
    val initialSetup = nextIndex++
    val placementArea = nextIndex++
    val supports = nextIndex++
    val events = if (isSandbox) null else nextIndex++

    return LevelEditorTabIndices(
        levelInfo = levelInfo,
        designPreview = designPreview,
        enemySpawns = enemySpawns,
        towers = towers,
        waypoints = waypoints,
        initialSetup = initialSetup,
        placementArea = placementArea,
        supports = supports,
        events = events,
    )
}

internal data class EnemySpawnEditorSnapshot(
    val enemySpawns: MutableList<EditorEnemySpawn>,
    val maxTurnNumber: Int,
    val spawnLoops: List<EditorSpawnLoop>,
)

/**
 * Main content for the Level Editor tab
 */
@Composable
internal fun LevelEditorContent(
    initialEditingLevelId: String? = null,
    onStartPlaytest: ((EditorLevel, FocusedPlaytestType) -> Unit)? = null,
) {
    val levels = remember { mutableStateOf(EditorStorage.getAllLevels()) }
    var selectedLevelId by remember { mutableStateOf<String?>(null) }
    var editingLevel by remember { mutableStateOf<EditorLevel?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showGeneratorDialog by remember { mutableStateOf(false) }
    var generatorConfig by remember { mutableStateOf<LevelGeneratorConfig?>(null) }
    var showVillainUsage by remember { mutableStateOf(false) }
    var showUnusedPairing by remember { mutableStateOf(false) }
    var levelToDelete by remember { mutableStateOf<EditorLevel?>(null) }
    val iamState by de.egril.defender.iam.IamService.state

    LaunchedEffect(initialEditingLevelId, levels.value) {
        if (initialEditingLevelId != null) {
            val levelToOpen = levels.value.find { it.id == initialEditingLevelId }
            if (levelToOpen != null) {
                selectedLevelId = levelToOpen.id
                editingLevel = levelToOpen
            }
        }
    }

    if (editingLevel != null) {
        // Level editing view
        LevelEditorView(
            level = editingLevel!!,
            onSave = { updatedLevel ->
                EditorStorage.saveLevel(updatedLevel)
                levels.value = EditorStorage.getAllLevels()
                // Reload the level from storage to trigger UI updates
                editingLevel = EditorStorage.getLevel(updatedLevel.id)
            },
            onCancel = { editingLevel = null },
            onStartPlaytest = { playtestLevel, type -> onStartPlaytest?.invoke(playtestLevel, type) },
        )
    } else if (showVillainUsage) {
        VillainUsagePage(
            levels = levels.value,
            onBack = { showVillainUsage = false },
        )
    } else if (showUnusedPairing) {
        UnusedMapsVillainsPage(
            levels = levels.value,
            maps = EditorStorage.getAllMaps().filter { it.readyToUse },
            onBack = { showUnusedPairing = false },
            onCreateLevel = { map, villain, name ->
                val newLevel = createPairedLevel(map, villain, name, getDefaultAuthorName(iamState))
                EditorStorage.saveLevel(newLevel)
                levels.value = EditorStorage.getAllLevels()
                showUnusedPairing = false
                editingLevel = newLevel
            },
        )
    } else {
        // Level list view
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.levels),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = { showVillainUsage = true }) {
                        Text(stringResource(Res.string.villain_usage))
                    }
                    Button(onClick = { showUnusedPairing = true }) {
                        Text(stringResource(Res.string.unused_maps_villains))
                    }
                    Button(onClick = { showGeneratorDialog = true }) {
                        Text(stringResource(Res.string.level_generator))
                    }
                    Button(onClick = { showCreateDialog = true }) {
                        Text(stringResource(Res.string.create_new_level))
                    }
                }
            }

            Text(
                text = stringResource(Res.string.select_level_to_edit),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(levels.value) { level ->
                    LevelCard(
                        level = level,
                        isSelected = selectedLevelId == level.id,
                        onSelect = {
                            selectedLevelId = level.id
                            editingLevel = level
                        },
                        onCopy = {
                            // Copy the level with a new ID and " - Copy" suffix
                            val copyTitle = "${level.title} - Copy"
                            val sanitizedTitle =
                                copyTitle
                                    .trim()
                                    .lowercase()
                                    .replace(" ", "_")
                                    .replace(Regex("[^a-z0-9_]"), "")
                                    .replace(Regex("_+"), "_") // Collapse consecutive underscores
                            val newId = "${sanitizedTitle}_${Random.nextInt(1000, 9999)}"
                            val copiedLevel =
                                level.copy(
                                    id = newId,
                                    title = copyTitle,
                                    isOfficial = false, // Copied levels are always user levels
                                )
                            EditorStorage.saveLevel(copiedLevel)
                            levels.value = EditorStorage.getAllLevels()
                        },
                        onDelete = {
                            levelToDelete = level
                        },
                    )
                }
            }
        }
    }

    // Confirmation dialog for delete level
    if (levelToDelete != null) {
        ConfirmationDialog(
            title = stringResource(Res.string.delete),
            message = stringResource(Res.string.confirm_delete_level),
            onDismiss = { levelToDelete = null },
            onConfirm = {
                EditorStorage.deleteLevel(levelToDelete!!.id)
                levels.value = EditorStorage.getAllLevels()
                if (selectedLevelId == levelToDelete!!.id) {
                    selectedLevelId = null
                }
                levelToDelete = null
            },
        )
    }

    if (showGeneratorDialog) {
        val defaultAuthor = getDefaultAuthorName(iamState)
        LevelGeneratorDialog(
            availableMaps = EditorStorage.getAllMaps().filter { it.readyToUse },
            defaultAuthor = defaultAuthor,
            isGenerating = generatorConfig != null,
            onDismiss = {
                showGeneratorDialog = false
                generatorConfig = null
            },
            onGenerate = { config -> generatorConfig = config },
        )
    }

    // Generating a big map takes a moment, so the generation runs after the dialog had a chance to
    // render its loading indicator. Always clear the generation state in a finally block so the
    // spinner can't get stuck if generation fails or the dialog is dismissed mid-flight.
    LaunchedEffect(generatorConfig) {
        val config = generatorConfig ?: return@LaunchedEffect
        try {
            // Keep the heavy map generation off the UI thread so the spinner can animate while the
            // editor is working.
            delay(50)
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { LevelGenerator.generate(config) }
            result.generatedMap?.let { EditorStorage.saveMap(it) }
            EditorStorage.saveLevel(result.level)
            levels.value = EditorStorage.getAllLevels()
            showGeneratorDialog = false
            editingLevel = result.level
        } finally {
            if (generatorConfig == config) {
                generatorConfig = null
            }
        }
    }

    if (showCreateDialog) {
        val defaultAuthor = getDefaultAuthorName(iamState)
        CreateLevelDialog(
            onDismiss = { showCreateDialog = false },
            defaultAuthor = defaultAuthor,
            onCreate = { title, author, template ->
                // Generate ID from title with underscores (lowercase, no "level_" prefix)
                val sanitizedTitle =
                    title
                        .trim()
                        .lowercase()
                        .replace(" ", "_")
                        .replace(Regex("[^a-z0-9_]"), "")
                        .replace(Regex("_+"), "_") // Collapse consecutive underscores
                val newId =
                    if (sanitizedTitle.isNotEmpty()) {
                        sanitizedTitle
                    } else {
                        "custom_${Random.nextInt(10000, 99999)}"
                    }
                // Get first ready-to-use map
                val firstReadyMap = EditorStorage.getAllMaps().filter { it.readyToUse }.firstOrNull()
                val baseLevel =
                    EditorLevel(
                        id = newId,
                        mapId = firstReadyMap?.id ?: "map_30x8",
                        title = title,
                        subtitle = "",
                        startCoins = 100,
                        startHealthPoints = 10,
                        enemySpawns = emptyList(),
                        availableTowers =
                            DefenderType.entries
                                .filter {
                                    it != DefenderType.DRAGONS_LAIR && it != DefenderType.ALTAR
                                }.toSet(),
                        author = author,
                    )
                val newLevel = applyLevelTemplate(baseLevel, firstReadyMap, template)
                EditorStorage.saveLevel(newLevel)
                levels.value = EditorStorage.getAllLevels()
                showCreateDialog = false
                editingLevel = newLevel
            },
        )
    }
}
