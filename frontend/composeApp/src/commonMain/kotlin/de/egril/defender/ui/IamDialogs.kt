package de.egril.defender.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalUriHandler
import kotlin.math.ceil
import kotlin.math.floor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.egril.defender.iam.DeviceAuthState
import de.egril.defender.ui.common.QrCodeGenerator
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

/**
 * Dialog shown during the Device Authorization Grant (RFC 8628) login flow.
 *
 * Displays the [DeviceAuthState.verificationUriComplete] (or [DeviceAuthState.verificationUri]
 * as fallback) and [DeviceAuthState.userCode] so the user can complete login on a phone or
 * second device without needing a browser on the machine running the game (e.g. Steam Deck
 * gaming mode). The URL is a clickable link that opens the browser when tapped, and a QR code
 * is shown below it so users on another device can scan it directly.
 */
@Composable
fun DeviceAuthLoginDialog(
    deviceAuthState: DeviceAuthState,
    onCancel: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val clickUrl = deviceAuthState.verificationUriComplete ?: deviceAuthState.verificationUri

    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier.widthIn(max = 420.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.iam_device_login_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                CircularProgressIndicator(modifier = Modifier.size(32.dp))

                Text(
                    text = stringResource(Res.string.iam_device_login_instruction),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Verification URL – shows the complete URL (pre-filled with user code) as a
                // clickable link that opens the browser.
                Text(
                    text = clickUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { uriHandler.openUri(clickUrl) }
                )

                // QR code so the user can scan the complete URL on another device.
                val qrMatrix = remember(clickUrl) { QrCodeGenerator.generate(clickUrl) }
                val darkColor = MaterialTheme.colorScheme.onSurface
                Canvas(modifier = Modifier.size(180.dp)) {
                    val modules = qrMatrix.size
                    for (row in qrMatrix.indices) {
                        for (col in qrMatrix[row].indices) {
                            if (qrMatrix[row][col]) {
                                // Use floor/ceil to pixel-align each cell edge, preventing
                                // sub-pixel gaps (light gray lines) between adjacent dark modules.
                                val left = floor(col * size.width / modules)
                                val top = floor(row * size.height / modules)
                                val right = ceil((col + 1) * size.width / modules)
                                val bottom = ceil((row + 1) * size.height / modules)
                                drawRect(
                                    color = darkColor,
                                    topLeft = Offset(left, top),
                                    size = Size(right - left, bottom - top)
                                )
                            }
                        }
                    }
                }

                // User code label + code
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.iam_device_login_code_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SelectionContainer {
                        Text(
                            text = deviceAuthState.userCode,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 4.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.iam_device_login_cancel))
                }
            }
        }
    }
}

