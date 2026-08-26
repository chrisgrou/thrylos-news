package gr.thrylos.news.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ArticleCard(item: FeedItem, onClick: () -> Unit) {
    val article = item.article
    Box(Modifier.fillMaxWidth()) {
        val surface = MaterialTheme.colorScheme.surface
        val containerColor = when {
            // Opaque (pre-blended) colors instead of .copy(alpha = ...): a translucent
            // container forces Compose to composite this card on an offscreen layer
            // beneath its elevation shadow on every frame it's on screen, which is a
            // real, measurable cost when a dozen cards are visible at once during a
            // fling — this was part of the reported scroll jank.
            item.isImportant -> lerp(surface, MaterialTheme.colorScheme.primaryContainer, 0.35f)
            article.isRead -> lerp(surface, MaterialTheme.colorScheme.surfaceVariant, 0.4f)
            else -> surface
        }
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                if (article.leadImageUrl != null) {
                    AsyncImage(
                        model = article.leadImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(14.dp)),
                    )
                } else {
                    Box(Modifier.size(80.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer))
                }

                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!article.isRead) {
                            Box(
                                Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = stripSourceSuffix(article.sourceName).uppercase() + " · " + formatRelativeTime(article.publishedAt ?: article.fetchedAt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                    val author = article.author
                    if (!author.isNullOrBlank()) {
                        Text(
                            text = author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    if (item.extraSourceCount > 0) {
                        Text(
                            text = "+${item.extraSourceCount} πηγές",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                }
            }
        }

        if (item.isImportant) {
            ImportantBadge(modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp))
        }
    }
}

@Composable
private fun ImportantBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(12.dp),
        )
        Text(
            "Featured",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** Also used by the home-screen widget, which can't pull in the reader's own
 *  date-formatting helpers (those live in a Compose screen, not a shared util). */
fun formatRelativeTime(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    val minutes = diff / 60_000
    return when {
        minutes < 1 -> "τώρα"
        minutes < 60 -> "${minutes}′"
        minutes < 24 * 60 -> "${minutes / 60}ω"
        minutes < 7 * 24 * 60 -> "${minutes / (24 * 60)}μ"
        else -> SimpleDateFormat("d MMM", Locale("el", "GR")).format(Date(millis))
    }
}
