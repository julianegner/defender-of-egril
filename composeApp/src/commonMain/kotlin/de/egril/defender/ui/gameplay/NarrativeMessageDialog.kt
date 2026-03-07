package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hyperether.resources.stringResource
import de.egril.defender.model.AttackerType
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon
import defender_of_egril.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * The type of narrative message popup.
 */
enum class NarrativeMessageType {
    STORY,  // Story message with wooden frame background
    EWHAD   // Ewhad message with dark gargoyle frame background
}

/**
 * A popup dialog for narrative messages (story events and Ewhad events).
 *
 * Displays an image as background with centered text (black/dark gray).
 * For EWHAD type: shows the Ewhad icon in the upper center, a large title below it, and text below the title.
 * For STORY type: shows a centered title and text on the background.
 *
 * @param type     The type of narrative message (STORY or EWHAD).
 * @param title    The title text to display.
 * @param text     The body text to display below the title.
 * @param onDismiss Called when the dialog should be closed.
 */
@Composable
fun NarrativeMessageDialog(
    type: NarrativeMessageType,
    title: String,
    text: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .wrapContentHeight(),
            contentAlignment = Alignment.Center
        ) {
            // Background image
            val backgroundPainter = when (type) {
                NarrativeMessageType.STORY -> painterResource(Res.drawable.story_message_background)
                NarrativeMessageType.EWHAD -> painterResource(Res.drawable.ewhad_message_background)
            }
            Image(
                painter = backgroundPainter,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds
            )

            // Content overlaid on background
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // For Ewhad type: show Ewhad icon at top center
                if (type == NarrativeMessageType.EWHAD) {
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EnemyTypeIcon(
                            attackerType = AttackerType.EWHAD,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    textAlign = TextAlign.Center,
                    fontSize = if (type == NarrativeMessageType.EWHAD) 22.sp else 20.sp
                )

                // Body text
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF333333),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Dismiss button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == NarrativeMessageType.EWHAD) {
                            Color(0xFF4A2060)
                        } else {
                            Color(0xFF5C3A1E)
                        }
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.ok),
                        color = Color.White
                    )
                }
            }
        }
    }
}
