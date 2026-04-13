package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import de.egril.defender.utils.isPlatformMobile
import de.egril.defender.ui.common.SelectableText
import defender_of_egril.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.text.selection.SelectionContainer

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
        val isMobile = isPlatformMobile
        val titleFontSize = when {
            isMobile && type == NarrativeMessageType.EWHAD -> 16.sp
            isMobile -> 15.sp
            type == NarrativeMessageType.EWHAD -> 22.sp
            else -> 20.sp
        }
        val bodyFontSize = if (isMobile) 12.sp else MaterialTheme.typography.bodyMedium.fontSize
        val iconSize = if (isMobile) 56.dp else 80.dp

        val backgroundPainter = when (type) {
            NarrativeMessageType.STORY -> painterResource(Res.drawable.story_message_background)
            NarrativeMessageType.EWHAD -> painterResource(Res.drawable.ewhad_message_background)
        }
        val buttonColor = if (type == NarrativeMessageType.EWHAD) Color(0xFF4A2060) else Color(0xFF5C3A1E)

        // Both background images are square (500×500 and 1024×1024).
        // Padding keeps text inside the frame border, computed as a fixed fraction of
        // the dialog dimensions:
        //   story_message_background.png: inner parchment starts at px ≈ 165/500 per side (h)
        //                                 and px ≈ 135/500 per side (v).
        //   ewhad_message_background.png: inner area at ≈ 280/1024 per side — smaller, so the
        //                                 story fractions cover both.
        // On mobile, BoxWithConstraints fills the available popup width so the dialog scales to
        // the actual device screen size rather than using a fixed narrow value.
        BoxWithConstraints(
            modifier = if (isMobile) Modifier.fillMaxWidth() else Modifier.width(700.dp),
            contentAlignment = Alignment.Center
        ) {
            // dialogWidth equals the actual rendered width on all platforms.
            // Keep the dialog square to match the square source images.
            val dialogWidth = maxWidth
            val dialogHeight = dialogWidth
            val horizontalPadding = dialogWidth * (165f / 500f)
            val verticalPadding = dialogHeight * (135f / 500f)

            Box(
                modifier = Modifier
                    .width(dialogWidth)
                    .height(dialogHeight),
                contentAlignment = Alignment.Center
            ) {
                // Background image
                Image(
                    painter = backgroundPainter,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.FillBounds
                )

                // Content overlaid on background – scrollable so long texts never overflow the frame
                Column(
                    modifier = Modifier
                        .padding(
                            horizontal = horizontalPadding,
                            vertical = verticalPadding
                        )
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // For Ewhad type: show Ewhad icon at top center
                    if (type == NarrativeMessageType.EWHAD) {
                        Box(
                            modifier = Modifier.size(iconSize),
                            contentAlignment = Alignment.Center
                        ) {
                            EnemyTypeIcon(
                                attackerType = AttackerType.EWHAD,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    // Title
                    SelectableText(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        textAlign = TextAlign.Center,
                        fontSize = titleFontSize
                    )

                    // Body text
                    SelectableText(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Center,
                        fontSize = bodyFontSize
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Dismiss button
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
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
}
