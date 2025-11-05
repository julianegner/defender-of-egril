package com.defenderofegril.ui.gameplay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defenderofegril.model.*
import com.defenderofegril.ui.*

@Composable
fun CompactDefenderButton(
    type: DefenderType,
    isSelected: Boolean,
    canAfford: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = canAfford,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF1976D2) else MaterialTheme.colorScheme.primary
        ),
        modifier = modifier,
        contentPadding = PaddingValues(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Tower icon
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                TowerTypeIcon(defenderType = type, modifier = Modifier.size(30.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                type.displayName
                    .replace(" Tower", ""),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Cost
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoneyIcon(size = 14.dp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "${type.baseCost}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun DefenderButton(
    type: DefenderType,
    isSelected: Boolean,
    canAfford: Boolean,
    coinsState: State<Int>,  // Accept State instead of Int
    onClick: () -> Unit
) {
    // Recalculate canAfford based on current coins.value to ensure reactivity
    val actuallyCanAfford = coinsState.value >= type.baseCost

    Button(
        onClick = onClick,
        enabled = actuallyCanAfford,  // Use recalculated value
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF1976D2) else MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.fillMaxWidth().height(70.dp),
        contentPadding = PaddingValues(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Tower icon on the left
            Box(
                modifier = Modifier.size(30.dp),
                contentAlignment = Alignment.Center
            ) {
                TowerTypeIcon(defenderType = type, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Stats on the right
            Row {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        type.displayName
                            .replace(" Tower", ""),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )

                    Text(
                        type.attackType.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = Color(0xFFFFEB3B)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimerIcon(size = 15.dp)
                        Text(
                            "${type.buildTime}T",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    TowerStats(type.minRange, type.baseDamage, type.baseRange, type.actionsPerTurn)
                }
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MoneyIcon(size = 14.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${type.baseCost}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TowerStats(minRange: Int, damage: Int, range: Int, actionsPerTurn: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ExplosionIcon(size = 12.dp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            "${damage}",
            style = MaterialTheme.typography.bodySmall
        )
    }
    if (minRange > 0) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TargetIcon(size = 12.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "${minRange}-${range}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TargetIcon(size = 12.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "${range}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        LightningIcon(size = 12.dp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            actionsPerTurn.toString(),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
