package gr.thrylos.news.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign as ComposeTextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import gr.thrylos.news.model.ContentBlock
import gr.thrylos.news.model.ReaderPrefs
import gr.thrylos.news.model.TextAlign
import gr.thrylos.news.theme.READER_BASE_BODY_SP
import gr.thrylos.news.theme.READER_BASE_HEADING_SP
import gr.thrylos.news.theme.ReaderColors
import gr.thrylos.news.theme.fontFamilyFor

@Composable
fun ContentBlockView(block: ContentBlock, prefs: ReaderPrefs, colors: ReaderColors, onMediaClick: (() -> Unit)? = null) {
    val fontFamily = fontFamilyFor(prefs.fontFamily)
    val bodySize = (READER_BASE_BODY_SP * prefs.fontScale).sp
    val align = if (prefs.textAlign == TextAlign.JUSTIFY) ComposeTextAlign.Justify else ComposeTextAlign.Start

    when (block) {
        is ContentBlock.Heading -> Text(
            text = block.text,
            style = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (READER_BASE_HEADING_SP * prefs.fontScale).sp,
                lineHeight = (READER_BASE_HEADING_SP * prefs.fontScale * 1.2f * prefs.lineHeightScale).sp,
                color = colors.text,
            ),
            modifier = Modifier.padding(bottom = 8.dp),
        )

        is ContentBlock.Paragraph -> Text(
            text = linkify(block.text),
            textAlign = align,
            style = TextStyle(
                fontFamily = fontFamily,
                fontSize = bodySize,
                lineHeight = (READER_BASE_BODY_SP * prefs.fontScale * 1.55f * prefs.lineHeightScale).sp,
                color = colors.text,
            ),
            modifier = Modifier.padding(bottom = 14.dp),
        )

        // Framed like a card: covers both an editorial pull-quote and an embedded
        // social post (Twitter/X, Facebook, Instagram embeds all extract as a plain
        // <blockquote> today, with no way to tell them apart) — a border reads as
        // "quoted from elsewhere" either way, instead of blending into body text.
        is ContentBlock.Quote -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(BorderStroke(1.dp, colors.secondaryText.copy(alpha = 0.35f)), RoundedCornerShape(10.dp))
                .padding(12.dp),
        ) {
            Text(
                text = linkify("“${block.text}”"),
                style = TextStyle(
                    fontFamily = fontFamily,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontSize = bodySize,
                    lineHeight = (READER_BASE_BODY_SP * prefs.fontScale * 1.55f * prefs.lineHeightScale).sp,
                    color = colors.secondaryText,
                ),
            )
        }

        is ContentBlock.ListBlock -> Column(Modifier.padding(bottom = 14.dp)) {
            block.items.forEachIndexed { index, item ->
                val bullet = if (block.ordered) "${index + 1}." else "•"
                Text(
                    text = "$bullet $item",
                    style = TextStyle(fontFamily = fontFamily, fontSize = bodySize, color = colors.text),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }

        is ContentBlock.Image -> Column(Modifier.padding(bottom = 14.dp)) {
            AsyncImage(
                model = block.url,
                contentDescription = block.caption,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .let { if (onMediaClick != null) it.clickable(onClick = onMediaClick) else it },
            )
            val caption = block.caption
            if (caption != null) {
                Text(
                    text = caption,
                    style = TextStyle(fontFamily = fontFamily, fontSize = 12.sp, color = colors.secondaryText),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        is ContentBlock.Video -> Column(Modifier.padding(bottom = 14.dp)) {
            Box(
                Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))
                    .let { if (onMediaClick != null) it.clickable(onClick = onMediaClick) else it },
                contentAlignment = Alignment.Center,
            ) {
                if (block.thumbnailUrl != null) {
                    AsyncImage(model = block.thumbnailUrl, contentDescription = block.caption, modifier = Modifier.fillMaxWidth().height(200.dp))
                } else {
                    Box(Modifier.fillMaxWidth().height(200.dp).background(colors.secondaryText.copy(alpha = 0.15f)))
                }
                Icon(Icons.Filled.PlayCircle, contentDescription = "Video", tint = Color.White, modifier = Modifier.padding(4.dp))
            }
            val caption = block.caption
            if (caption != null) {
                Text(
                    text = caption,
                    style = TextStyle(fontFamily = fontFamily, fontSize = 12.sp, color = colors.secondaryText),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Extraction strips out real `<a href>` links (only their visible text survives, e.g.
 *  a tweet's "pic.twitter.com/xxx" media link becomes plain text), so bare URLs read
 *  as inert text. Detect them at render time and make them tappable instead. */
private val URL_REGEX = Regex(
    "(https?://[\\w.-]+\\.[a-zA-Z]{2,}(?:/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?)" +
        "|(www\\.[\\w.-]+\\.[a-zA-Z]{2,}(?:/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?)" +
        "|((?:pic\\.twitter\\.com|t\\.co|instagram\\.com|fb\\.me|bit\\.ly)/[\\w-]+)",
    RegexOption.IGNORE_CASE,
)

/** A fixed accent (not derived from the reader theme) so a link is recognizable as
 *  tappable by color, not just the underline, across every reader theme (light/
 *  sepia/dark/black/high-contrast). */
private val LinkColor = Color(0xFF3B82F6)

private fun linkify(text: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (match in URL_REGEX.findAll(text)) {
        append(text.substring(last, match.range.first))
        val url = match.value
        val href = if (url.startsWith("http", ignoreCase = true)) url else "https://$url"
        withLink(
            LinkAnnotation.Url(
                href,
                TextLinkStyles(style = SpanStyle(color = LinkColor, textDecoration = TextDecoration.Underline)),
            ),
        ) {
            append(url)
        }
        last = match.range.last + 1
    }
    append(text.substring(last))
}
