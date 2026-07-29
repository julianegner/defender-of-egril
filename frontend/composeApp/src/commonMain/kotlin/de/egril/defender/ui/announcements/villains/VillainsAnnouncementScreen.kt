package de.egril.defender.ui.announcements.villains

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.utils.DeepLink
import de.egril.defender.utils.observeBrowserPathChanges
import de.egril.defender.utils.parseDeepLink
import de.egril.defender.utils.updateBrowserUrl
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.banner_villains_dark
import defender_of_egril.composeapp.generated.resources.banner_villains_light
import defender_of_egril.composeapp.generated.resources.back
import defender_of_egril.composeapp.generated.resources.villains_announcement_content_description
import defender_of_egril.composeapp.generated.resources.villains_announcement_page_text
import defender_of_egril.composeapp.generated.resources.villains_announcement_page_title
import org.jetbrains.compose.resources.painterResource

/** Parses `**bold**` and `*italic*` inline markers into an [AnnotatedString]. */
private fun parseInlineMarkdown(text: String): AnnotatedString =
    buildAnnotatedString {
        // Combined pattern: ***bold+italic*** | **bold** | *italic*
        val pattern = Regex("""\*\*\*(.*?)\*\*\*|\*\*(.*?)\*\*|\*(.*?)\*""")
        var cursor = 0
        pattern.findAll(text).forEach { match ->
            append(text.substring(cursor, match.range.first))
            val boldItalic = match.groupValues[1]
            val bold = match.groupValues[2]
            val italic = match.groupValues[3]
            when {
                boldItalic.isNotEmpty() ->
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        append(boldItalic)
                    }
                bold.isNotEmpty() ->
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
                else ->
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(italic) }
            }
            cursor = match.range.last + 1
        }
        append(text.substring(cursor))
    }

/** Renders a markdown string with support for ### headings, **bold**, *italic*, * bullets, and ---. */
@Composable
private fun MarkdownContent(markdown: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        markdown.split("\n").forEach { line ->
            when {
                line.startsWith("### ") -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                line == "---" ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                line.startsWith("* ") -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "\u2022",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Text(
                            text = parseInlineMarkdown(line.removePrefix("* ")),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                line.isEmpty() -> Spacer(Modifier.height(4.dp))

                else ->
                    Text(
                        text = parseInlineMarkdown(line),
                        style = MaterialTheme.typography.bodyLarge,
                    )
            }
        }
    }
}

@Composable
fun VillainsAnnouncementScreen(
    onBack: () -> Unit,
) {
    val isDarkMode = AppSettings.isDarkMode.value
    val painter =
        if (isDarkMode) {
            painterResource(Res.drawable.banner_villains_light)
        } else {
            painterResource(Res.drawable.banner_villains_dark)
        }

    val currentOnBack by rememberUpdatedState(onBack)
    DisposableEffect(Unit) {
        val unsubscribe =
            observeBrowserPathChanges { path ->
                if (parseDeepLink(path) !is DeepLink.VillainsAnnouncement) {
                    currentOnBack()
                }
            }
        onDispose {
            unsubscribe()
            updateBrowserUrl("/")
        }
    }
    LaunchedEffect(Unit) {
        updateBrowserUrl("/announcement/villains")
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier.align(Alignment.Start),
            ) {
                Text(stringResource(Res.string.back))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painter,
                contentDescription = stringResource(Res.string.villains_announcement_content_description),
                modifier = Modifier.fillMaxWidth(0.7f).widthIn(max = 460.dp),
                contentScale = ContentScale.Fit,
            )

            Spacer(modifier = Modifier.height(24.dp))

            SelectionContainer {
                Column(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 740.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.villains_announcement_page_title),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    )
                    MarkdownContent(markdown = stringResource(Res.string.villains_announcement_page_text))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

