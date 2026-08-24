package gr.thrylos.news.reader

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gr.thrylos.news.feed.stripSourceSuffix
import gr.thrylos.news.model.ContentBlock
import gr.thrylos.news.reader.media.mediaItems
import gr.thrylos.news.theme.READER_BASE_HEADING_SP
import gr.thrylos.news.theme.colorsFor
import gr.thrylos.news.theme.fontFamilyFor
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    onOpenMedia: (articleId: String, index: Int) -> Unit,
    onOpenSourceProfile: (sourceName: String) -> Unit,
    onOpenAuthorProfile: (author: String) -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val prefs by viewModel.readerPrefs.collectAsStateWithLifecycle()
    val colors = colorsFor(prefs.theme)
    val pagerState = rememberPagerState(initialPage = viewModel.startIndex) { viewModel.idList.size }
    var showSettings by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val currentId = viewModel.idList.getOrElse(pagerState.currentPage) { viewModel.idList.first() }
    val currentArticle by viewModel.articleFlow(currentId).collectAsStateWithLifecycle()
    val mediaCount = currentArticle?.mediaItems()?.size ?: 0

    LaunchedEffect(currentId) {
        delay(3000)
        viewModel.markRead(currentId)
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().systemBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Πίσω", tint = colors.text) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(enabled = currentArticle != null) { currentArticle?.let { onOpenSourceProfile(it.sourceName) } }
                        .padding(horizontal = 4.dp),
                ) {
                    Text(currentArticle?.sourceName?.let(::stripSourceSuffix).orEmpty(), color = colors.secondaryText)
                    Icon(Icons.Filled.ExpandMore, contentDescription = "Πηγή", tint = colors.secondaryText, modifier = Modifier.padding(start = 2.dp))
                }
                Box {
                    val bookmarked = currentArticle?.isBookmarked == true
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Επιλογές", tint = colors.text)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (bookmarked) "Αφαίρεση bookmark" else "Bookmark") },
                            leadingIcon = { Icon(if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.toggleBookmark(currentId, bookmarked)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Ρυθμίσεις ανάγνωσης") },
                            leadingIcon = { Icon(Icons.Filled.TextFields, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showSettings = true
                            },
                        )
                    }
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                val id = viewModel.idList[page]
                val article by viewModel.articleFlow(id).collectAsStateWithLifecycle()
                val a = article
                if (a == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    val mediaIndexByContentIndex = remember(a.content) {
                        val map = mutableMapOf<Int, Int>()
                        var counter = 0
                        a.content.forEachIndexed { i, block ->
                            if (block is ContentBlock.Image || block is ContentBlock.Video) {
                                map[i] = counter
                                counter++
                            }
                        }
                        map
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = (12 + prefs.marginWidth * 12).dp, vertical = 10.dp),
                    ) {
                        item {
                            Text(
                                a.title,
                                color = colors.text,
                                style = TextStyle(
                                    fontFamily = fontFamilyFor(prefs.fontFamily),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (READER_BASE_HEADING_SP * 1.15f * prefs.fontScale).sp,
                                    lineHeight = (READER_BASE_HEADING_SP * 1.15f * 1.2f * prefs.fontScale).sp,
                                ),
                            )
                            val author = a.author
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
                            ) {
                                if (author != null) {
                                    Text(
                                        author,
                                        color = colors.secondaryText,
                                        modifier = Modifier
                                            .border(1.dp, colors.secondaryText, RoundedCornerShape(50))
                                            .clickable { onOpenAuthorProfile(author) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                }
                                Text(
                                    formatDateTime(a.publishedAt ?: a.fetchedAt),
                                    color = colors.secondaryText,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                        itemsIndexed(a.content) { index, block ->
                            ContentBlockView(
                                block = block,
                                prefs = prefs,
                                colors = colors,
                                onMediaClick = mediaIndexByContentIndex[index]?.let { mediaIndex -> { onOpenMedia(id, mediaIndex) } },
                            )
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().systemBarsPadding().padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(onClick = {
                    val a = currentArticle ?: return@IconButton
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "${a.title}\n${a.url}")
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                }) {
                    Icon(Icons.Filled.Share, contentDescription = "Κοινοποίηση", tint = colors.text)
                }
                IconButton(onClick = {
                    val a = currentArticle ?: return@IconButton
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.url)))
                }) {
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = "Άνοιγμα στον browser", tint = colors.text)
                }
                if (mediaCount > 0) {
                    IconButton(onClick = { onOpenMedia(currentId, 0) }) {
                        Icon(Icons.Filled.PermMedia, contentDescription = "Media ($mediaCount)", tint = colors.text)
                    }
                }
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }) {
            ReadingSettingsSheet(prefs = prefs, onUpdate = viewModel::updateReaderPrefs)
        }
    }
}

private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale("el", "GR")).format(Date(millis))
