package gr.thrylos.news.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign as ComposeTextAlign
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
fun ContentBlockView(block: ContentBlock, prefs: ReaderPrefs, colors: ReaderColors) {
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
            text = block.text,
            textAlign = align,
            style = TextStyle(
                fontFamily = fontFamily,
                fontSize = bodySize,
                lineHeight = (READER_BASE_BODY_SP * prefs.fontScale * 1.55f * prefs.lineHeightScale).sp,
                color = colors.text,
            ),
            modifier = Modifier.padding(bottom = 14.dp),
        )

        is ContentBlock.Quote -> Text(
            text = "“${block.text}”",
            style = TextStyle(
                fontFamily = fontFamily,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = bodySize,
                lineHeight = (READER_BASE_BODY_SP * prefs.fontScale * 1.55f * prefs.lineHeightScale).sp,
                color = colors.secondaryText,
            ),
            modifier = Modifier.padding(bottom = 14.dp, start = 8.dp),
        )

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
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            )
            if (block.caption != null) {
                Text(
                    text = block.caption,
                    style = TextStyle(fontFamily = fontFamily, fontSize = 12.sp, color = colors.secondaryText),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
