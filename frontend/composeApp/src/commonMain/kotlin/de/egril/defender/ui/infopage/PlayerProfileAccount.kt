@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.infopage

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.iam.IamState
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import de.egril.defender.ui.icon.LockIcon
import de.egril.defender.ui.icon.ToolsIcon
import de.egril.defender.ui.icon.UnlockIcon
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.*

/**
 * Card showing the IAM / user account state (logged-in info, login/logout button,
 * and the "always log in" toggle).
 */
@Composable
internal fun UserAccountCard(
    iamState: IamState,
    iamLoginInProgress: Boolean,
    onIamLogin: () -> Unit,
    onIamLogout: () -> Unit,
    onManageAccount: () -> Unit = {},
    alwaysLogin: Boolean = false,
    onAlwaysLoginChanged: (Boolean) -> Unit = {},
    useRemoteSettings: Boolean = true,
    onUseRemoteSettingsChanged: (Boolean) -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.user_account),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (iamState.isAuthenticated) {
                // Three-column layout when wide enough: names | action buttons | toggles
                // Stacked layout on narrow widths
                val fullName =
                    listOfNotNull(iamState.firstName, iamState.lastName)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .takeIf { it.isNotBlank() }
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isWide = maxWidth >= 560.dp
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            // Column 1: User names
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                iamState.username?.let { username ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        UnlockIcon(size = 14.dp)
                                        Text(
                                            text = username,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                                if (fullName != null) {
                                    Text(
                                        text = fullName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                iamState.email?.let { email ->
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            // Column 2: Action buttons
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                OutlinedButton(
                                    onClick = onManageAccount,
                                    modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                                ) {
                                    ToolsIcon(size = 14.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(Res.string.iam_manage_account),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    if (AppSettings.showButtonShortcutHints.value) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        ShortcutKeyChip(text = "M", color = LocalContentColor.current.copy(alpha = 0.75f))
                                    }
                                }
                                OutlinedButton(
                                    onClick = onIamLogout,
                                    modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                                ) {
                                    LockIcon(size = 14.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(Res.string.iam_logout),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    if (AppSettings.showButtonShortcutHints.value) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        ShortcutKeyChip(text = "L", color = LocalContentColor.current.copy(alpha = 0.75f))
                                    }
                                }
                            }
                            // Column 3: Toggles
                            AccountSettingToggles(
                                alwaysLogin = alwaysLogin,
                                onAlwaysLoginChanged = onAlwaysLoginChanged,
                                useRemoteSettings = useRemoteSettings,
                                onUseRemoteSettingsChanged = onUseRemoteSettingsChanged,
                                spreadAcrossWidth = false,
                            )
                        }
                    } else {
                        // Narrow: stacked layout
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            iamState.username?.let { username ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    UnlockIcon(size = 14.dp)
                                    Text(
                                        text = username,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            if (fullName != null) {
                                Text(
                                    text = fullName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            iamState.email?.let { email ->
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = onManageAccount,
                                    modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                                ) {
                                    ToolsIcon(size = 14.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(Res.string.iam_manage_account),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    if (AppSettings.showButtonShortcutHints.value) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        ShortcutKeyChip(text = "M", color = LocalContentColor.current.copy(alpha = 0.75f))
                                    }
                                }
                                OutlinedButton(
                                    onClick = onIamLogout,
                                    modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                                ) {
                                    LockIcon(size = 14.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(Res.string.iam_logout),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    if (AppSettings.showButtonShortcutHints.value) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        ShortcutKeyChip(text = "L", color = LocalContentColor.current.copy(alpha = 0.75f))
                                    }
                                }
                            }
                            HorizontalDivider()
                            AccountSettingToggles(
                                alwaysLogin = alwaysLogin,
                                onAlwaysLoginChanged = onAlwaysLoginChanged,
                                useRemoteSettings = useRemoteSettings,
                                onUseRemoteSettingsChanged = onUseRemoteSettingsChanged,
                                spreadAcrossWidth = true,
                            )
                        }
                    }
                }
            } else if (iamLoginInProgress) {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.height(36.dp),
                    enabled = false,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(Res.string.iam_login_waiting),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                HorizontalDivider()
                AccountSettingToggles(
                    alwaysLogin = alwaysLogin,
                    onAlwaysLoginChanged = onAlwaysLoginChanged,
                    useRemoteSettings = useRemoteSettings,
                    onUseRemoteSettingsChanged = onUseRemoteSettingsChanged,
                    spreadAcrossWidth = true,
                )
            } else {
                OutlinedButton(
                    onClick = onIamLogin,
                    modifier = Modifier.height(36.dp),
                ) {
                    UnlockIcon(size = 14.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(Res.string.iam_login),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                HorizontalDivider()
                AccountSettingToggles(
                    alwaysLogin = alwaysLogin,
                    onAlwaysLoginChanged = onAlwaysLoginChanged,
                    useRemoteSettings = useRemoteSettings,
                    onUseRemoteSettingsChanged = onUseRemoteSettingsChanged,
                    spreadAcrossWidth = true,
                )
            }
        }
    }
}

/**
 * Reusable composable for the "Always log in" and "Use remote settings" toggle rows.
 *
 * @param spreadAcrossWidth When true, each row fills the available width with SpaceBetween
 *   arrangement (used in the stacked / non-authenticated layout). When false, label and switch
 *   are placed compactly side by side (used as the third column in the wide authenticated layout).
 */
@Composable
private fun AccountSettingToggles(
    alwaysLogin: Boolean,
    onAlwaysLoginChanged: (Boolean) -> Unit,
    useRemoteSettings: Boolean,
    onUseRemoteSettingsChanged: (Boolean) -> Unit,
    spreadAcrossWidth: Boolean = false,
) {
    val rowModifier = if (spreadAcrossWidth) Modifier.fillMaxWidth() else Modifier
    val rowArrangement = if (spreadAcrossWidth) Arrangement.SpaceBetween else Arrangement.spacedBy(8.dp)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = rowArrangement,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(Res.string.always_log_in),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                ShortcutKeyChip(text = "A")
            }
            Switch(
                checked = alwaysLogin,
                onCheckedChange = { onAlwaysLoginChanged(it) },
            )
        }
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = rowArrangement,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(Res.string.use_remote_settings),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                ShortcutKeyChip(text = "R")
            }
            Switch(
                checked = useRemoteSettings,
                onCheckedChange = { onUseRemoteSettingsChanged(it) },
            )
        }
    }
}
