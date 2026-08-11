package de.egril.defender.ui.editor.level

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import com.hyperether.resources.stringResource
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.EditorHorizontalScrollbar
import de.egril.defender.ui.getLocalizedName
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.apply_level_template
import defender_of_egril.composeapp.generated.resources.arrival_overlap_turns
import defender_of_egril.composeapp.generated.resources.arrival_window
import defender_of_egril.composeapp.generated.resources.consistency_checks
import defender_of_egril.composeapp.generated.resources.design_preview
import defender_of_egril.composeapp.generated.resources.economy_rating
import defender_of_egril.composeapp.generated.resources.focused_playtests
import defender_of_egril.composeapp.generated.resources.invalid_event_positions
import defender_of_egril.composeapp.generated.resources.invalid_initial_positions
import defender_of_egril.composeapp.generated.resources.invalid_spawn_assignments
import defender_of_egril.composeapp.generated.resources.invalid_waypoints_count
import defender_of_egril.composeapp.generated.resources.missing_spawn_types
import defender_of_egril.composeapp.generated.resources.needed_counters
import defender_of_egril.composeapp.generated.resources.no_consistency_issues
import defender_of_egril.composeapp.generated.resources.pacing_rating
import defender_of_egril.composeapp.generated.resources.peak_arrival
import defender_of_egril.composeapp.generated.resources.playtest_climax
import defender_of_egril.composeapp.generated.resources.playtest_first_villain
import defender_of_egril.composeapp.generated.resources.playtest_full
import defender_of_egril.composeapp.generated.resources.playtest_peak_pressure
import defender_of_egril.composeapp.generated.resources.quiet_turns
import defender_of_egril.composeapp.generated.resources.rating_calm
import defender_of_egril.composeapp.generated.resources.rating_covered
import defender_of_egril.composeapp.generated.resources.rating_early
import defender_of_egril.composeapp.generated.resources.rating_good
import defender_of_egril.composeapp.generated.resources.rating_harsh
import defender_of_egril.composeapp.generated.resources.rating_late
import defender_of_egril.composeapp.generated.resources.rating_mid
import defender_of_egril.composeapp.generated.resources.rating_none
import defender_of_egril.composeapp.generated.resources.rating_spiky
import defender_of_egril.composeapp.generated.resources.rating_steady
import defender_of_egril.composeapp.generated.resources.rating_tight
import defender_of_egril.composeapp.generated.resources.template_endurance
import defender_of_egril.composeapp.generated.resources.template_river_pressure
import defender_of_egril.composeapp.generated.resources.template_steady_pressure
import defender_of_egril.composeapp.generated.resources.template_tutorial
import defender_of_egril.composeapp.generated.resources.template_villain_duel
import defender_of_egril.composeapp.generated.resources.total_target_damage
import defender_of_egril.composeapp.generated.resources.turn_label
import defender_of_egril.composeapp.generated.resources.villain_timing
import defender_of_egril.composeapp.generated.resources.wave_pacing_timeline
import defender_of_egril.composeapp.generated.resources.wave_simulator

@Composable
internal fun LevelDesignOverview(
    summary: LevelDesignSummary,
    arrivals: List<WaveArrivalBucket>,
    consistency: LevelConsistencySummary,
    onApplyTemplate: (EditorLevelTemplate) -> Unit,
    onStartPlaytest: (FocusedPlaytestType) -> Unit,
    playtestEnabled: Boolean,
    onOpenEnemySpawnTurn: (Int) -> Unit,
) {
    val locale = currentLanguage.value
    val pacingPreviews = remember(summary.turnPreviews) { summary.turnPreviews.filter { it.enemyCount > 0 } }
    val pacingScrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.design_preview),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    MetricColumn(
                        label = stringResource(Res.string.total_target_damage),
                        value = summary.totalTargetDamage.toString(),
                    )
                    MetricColumn(
                        label = stringResource(Res.string.arrival_window),
                        value =
                            summary.turnPreviews
                                .mapNotNull { it.latestArrivalTurn }
                                .maxOrNull()
                                ?.toString() ?: "-",
                    )
                    MetricColumn(
                        label = stringResource(Res.string.needed_counters),
                        value =
                            if (summary.missingCounters.isEmpty()) {
                                bandLabel(CounterState.COVERED)
                            } else {
                                summary.missingCounters.joinToString(", ") { it.getLocalizedName(locale) }
                            },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    MetricColumn(
                        label = stringResource(Res.string.economy_rating),
                        value = summary.economyBand.localizedLabel(),
                    )
                    MetricColumn(
                        label = stringResource(Res.string.pacing_rating),
                        value = summary.pacingBand.localizedLabel(),
                    )
                    MetricColumn(
                        label = stringResource(Res.string.villain_timing),
                        value = summary.villainTimingBand.localizedLabel(),
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.wave_simulator),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                MetricColumn(
                    label = stringResource(Res.string.peak_arrival),
                    value =
                        summary.peakArrivalTurn?.let { turn ->
                            "${stringResource(Res.string.turn_label)} $turn (${summary.peakArrivalCount})"
                        } ?: "-",
                )
                MetricColumn(
                    label = stringResource(Res.string.arrival_overlap_turns),
                    value = summary.arrivalOverlapTurns.joinToStringPreview(),
                )
                MetricColumn(
                    label = stringResource(Res.string.quiet_turns),
                    value = summary.quietTurns.joinToStringPreview(),
                )
                arrivals.take(5).forEach { bucket ->
                    Text(
                        text =
                            "${stringResource(Res.string.turn_label)} ${bucket.turn}: ${bucket.enemyCount} (${bucket.spawnTurns.joinToStringPreview()})" +
                                if (bucket.villainNames.isNotEmpty()) " - ${bucket.villainNames.joinToString(", ")}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (bucket.enemyCount >= 3) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.consistency_checks),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (!consistency.hasIssues) {
                    Text(
                        text = stringResource(Res.string.no_consistency_issues),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    if (consistency.invalidSpawnAssignments.isNotEmpty()) {
                        Text(
                            text = stringResource(Res.string.invalid_spawn_assignments, consistency.invalidSpawnAssignments.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (consistency.missingCompatibleSpawnTypes.isNotEmpty()) {
                        Text(
                            text =
                                stringResource(
                                    Res.string.missing_spawn_types,
                                    consistency.missingCompatibleSpawnTypes.joinToString(", ") { it.localizedEnemyName(locale) },
                                ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (consistency.invalidWaypoints.isNotEmpty()) {
                        Text(
                            text = stringResource(Res.string.invalid_waypoints_count, consistency.invalidWaypoints.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (consistency.invalidInitialPlacementCount > 0) {
                        Text(
                            text = stringResource(Res.string.invalid_initial_positions, consistency.invalidInitialPlacementCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (consistency.invalidEventPositionCount > 0) {
                        Text(
                            text = stringResource(Res.string.invalid_event_positions, consistency.invalidEventPositionCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.wave_pacing_timeline),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(pacingScrollState)
                                .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        pacingPreviews.forEach { preview ->
                            TurnPreviewChip(
                                preview = preview,
                                peakScore = summary.peakPressureScore,
                                onClick = { onOpenEnemySpawnTurn(preview.turn) },
                            )
                        }
                    }
                    EditorHorizontalScrollbar(scrollState = pacingScrollState)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.apply_level_template),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EditorLevelTemplate.entries.toList()) { template ->
                        Button(onClick = { onApplyTemplate(template) }) {
                            Text(template.localizedLabel())
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.focused_playtests),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FocusedPlaytestType.entries.toList()) { type ->
                        Button(
                            onClick = { onStartPlaytest(type) },
                            enabled = playtestEnabled,
                        ) {
                            Text(type.localizedLabel())
                        }
                    }
                }
            }
        }
    }
}

private enum class CounterState {
    COVERED,
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TurnPreviewChip(
    preview: TurnPressurePreview,
    peakScore: Double,
    onClick: () -> Unit,
) {
    val intensity =
        when {
            peakScore <= 0.0 -> 0.15f
            preview.pressureScore >= peakScore * 0.9 -> 0.9f
            preview.pressureScore >= peakScore * 0.6 -> 0.55f
            else -> 0.25f
        }
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        colors =
            CardDefaults.cardColors(
                containerColor = Color.Red.copy(alpha = intensity),
            ),
    ) {
        Column(
            modifier = Modifier.width(120.dp).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${stringResource(Res.string.turn_label)} ${preview.turn}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(text = "${preview.enemyCount}", style = MaterialTheme.typography.bodySmall)
            Text(
                text =
                    if (preview.earliestArrivalTurn != null && preview.latestArrivalTurn != null) {
                        "${preview.earliestArrivalTurn}-${preview.latestArrivalTurn}"
                    } else {
                        "-"
                    },
                style = MaterialTheme.typography.bodySmall,
            )
            if (preview.villainNames.isNotEmpty()) {
                Text(
                    text = preview.villainNames.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7F0000),
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                Box(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun FocusedPlaytestType.localizedLabel(): String =
    when (this) {
        FocusedPlaytestType.FULL -> stringResource(Res.string.playtest_full)
        FocusedPlaytestType.FIRST_VILLAIN -> stringResource(Res.string.playtest_first_villain)
        FocusedPlaytestType.PEAK_PRESSURE -> stringResource(Res.string.playtest_peak_pressure)
        FocusedPlaytestType.CLIMAX -> stringResource(Res.string.playtest_climax)
    }

@Composable
private fun EditorLevelTemplate.localizedLabel(): String =
    when (this) {
        EditorLevelTemplate.TUTORIAL -> stringResource(Res.string.template_tutorial)
        EditorLevelTemplate.STEADY_PRESSURE -> stringResource(Res.string.template_steady_pressure)
        EditorLevelTemplate.VILLAIN_DUEL -> stringResource(Res.string.template_villain_duel)
        EditorLevelTemplate.RIVER_PRESSURE -> stringResource(Res.string.template_river_pressure)
        EditorLevelTemplate.ENDURANCE -> stringResource(Res.string.template_endurance)
    }

@Composable
private fun EconomyBand.localizedLabel(): String = bandLabel(this)

private fun List<Int>.joinToStringPreview(): String =
    if (isEmpty()) {
        "-"
    } else {
        take(6).joinToString(", ").let { prefix ->
            if (size > 6) "$prefix..." else prefix
        }
    }

private fun AttackerType.localizedEnemyName(locale: AppLocale): String = getLocalizedName(locale)

@Composable
private fun PacingBand.localizedLabel(): String = bandLabel(this)

@Composable
private fun TimingBand.localizedLabel(): String = bandLabel(this)

@Composable
private fun bandLabel(band: Any): String =
    when (band) {
        EconomyBand.HARSH -> stringResource(Res.string.rating_harsh)
        EconomyBand.TIGHT -> stringResource(Res.string.rating_tight)
        EconomyBand.GOOD -> stringResource(Res.string.rating_good)
        PacingBand.CALM -> stringResource(Res.string.rating_calm)
        PacingBand.STEADY -> stringResource(Res.string.rating_steady)
        PacingBand.SPIKY -> stringResource(Res.string.rating_spiky)
        TimingBand.NONE -> stringResource(Res.string.rating_none)
        TimingBand.EARLY -> stringResource(Res.string.rating_early)
        TimingBand.MID -> stringResource(Res.string.rating_mid)
        TimingBand.LATE -> stringResource(Res.string.rating_late)
        CounterState.COVERED -> stringResource(Res.string.rating_covered)
        else -> ""
    }
