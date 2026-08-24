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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import gr.thrylos.news.model.ContentBlock
import gr.thrylos.news.reader.media.mediaItems
import gr.thrylos.news.theme.READER_BASE_HEADING_SP
import gr.thrylos.news.theme.colorsFor
import gr.thrylos.news.theme.fontFamilyFor
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    onOpenMedia: (articleId: String, index: Int) -> Unit,
    onOpenArticle: (articleId: String) -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val prefs by viewModel.readerPrefs.collectAsStateWithLifecycle()
    val colors = colorsFor(prefs.theme)
    val pagerState = rememberPagerState(initialPage = viewModel.startIndex) { viewModel.idList.size }
    var showSettings by remember { mutableStateOf(false) }
    var showSourceSheet by remember { mutableStateOf(false) }
    var authorSheetFor by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val currentId = viewModel.idList.getOrElse(pagerState.currentPage) { viewModel.idList.first() }
    val currentArticle by viewModel.articleFlow(currentId).collectAsStateWithLifecycle()

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
                    modifier = Modifier.clickable(enabled = currentArticle != null) { showSourceSheet = true }.padding(horizontal = 4.dp),
                ) {
                    Text(currentArticle?.sourceName.orEmpty(), color = colors.secondaryText)
                    Icon(Icons.Filled.ExpandMore, contentDescription = "Πηγή", tint = colors.secondaryText, modifier = Modifier.padding(start = 2.dp))
                }
                Spacer(Modifier)
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
                    val mediaCount = a.mediaItems().size

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = (24 + prefs.marginWidth * 12).dp, vertical = 16.dp),
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
                            if (author != null) {
                                Text(
                                    author,
                                    color = colors.secondaryText,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 16.dp).clickable { authorSheetFor = author },
                                )
                            } else {
                                Spacer(Modifier.padding(top = 8.dp))
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
                        if (mediaCount > 0) {
                            item {
                                Card(
                                    onClick = { onOpenMedia(id, 0) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Filled.PermMedia, contentDescription = null)
                                        Text("Media ($mediaCount)", modifier = Modifier.padding(start = 10.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val bookmarked = currentArticle?.isBookmarked == true
            Row(
                Modifier.fillMaxWidth().systemBarsPadding().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(onClick = { viewModel.toggleBookmark(currentId, bookmarked) }) {
                    Icon(
                        if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = colors.text,
                    )
                }
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
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Filled.TextFields, contentDescription = "Ρυθμίσεις ανάγνωσης", tint = colors.text)
                }
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }) {
            ReadingSettingsSheet(prefs = prefs, onUpdate = viewModel::updateReaderPrefs)
        }
    }

    val sourceArticle = currentArticle
    if (showSourceSheet && sourceArticle != null) {
        ModalBottomSheet(onDismissRequest = { showSourceSheet = false }) {
            SourceBannerSheet(
                sourceId = sourceArticle.sourceId,
                sourceName = sourceArticle.sourceName,
                viewModel = viewModel,
                onOpenArticle = { id ->
                    showSourceSheet = false
                    if (id != currentId) onOpenArticle(id)
                },
                onIgnored = {
                    showSourceSheet = false
                    onBack()
                },
            )
        }
    }

    val author = authorSheetFor
    if (author != null) {
        ModalBottomSheet(onDismissRequest = { authorSheetFor = null }) {
            AuthorBannerSheet(
                author = author,
                viewModel = viewModel,
                onOpenArticle = { id ->
                    authorSheetFor = null
                    if (id != currentId) onOpenArticle(id)
                },
            )
        }
    }
}

@Composable
private fun AuthorBannerSheet(
    author: String,
    viewModel: ReaderViewModel,
    onOpenArticle: (String) -> Unit,
) {
    val articles by viewModel.articlesForAuthor(author).collectAsStateWithLifecycle(initialValue = emptyList())

    Column(Modifier.fillMaxWidth().fillMaxHeight(0.66f).padding(16.dp)) {
        Text(author, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 4.dp))
        Text(
            "Άρθρα από αυτόν τον συντάκτη",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(articles) { _, article ->
                Column(
                    Modifier.fillMaxWidth().clickable {
                        viewModel.setCursorContext(articles.map { it.id })
                        onOpenArticle(article.id)
                    }.padding(vertical = 10.dp),
                ) {
                    Text(article.sourceName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(article.title, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun SourceBannerSheet(
    sourceId: String,
    sourceName: String,
    viewModel: ReaderViewModel,
    onOpenArticle: (String) -> Unit,
    onIgnored: () -> Unit,
) {
    val important by viewModel.isSourceImportant(sourceId).collectAsStateWithLifecycle(initialValue = false)
    val articles by viewModel.articlesForSource(sourceId).collectAsStateWithLifecycle(initialValue = emptyList())

    Column(Modifier.fillMaxWidth().fillMaxHeight(0.66f).padding(16.dp)) {
        Text(sourceName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { viewModel.toggleSourceImportant(sourceId, sourceName) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text(if (important) "Αφαίρεση σημαντικού" else "Σημαντική πηγή")
            }
            OutlinedButton(onClick = { viewModel.ignoreSource(sourceId); onIgnored() }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.VisibilityOff, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text("Αγνόησε")
            }
        }
        Text(
            "Άρθρα από αυτή την πηγή",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
        )
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(articles) { _, article ->
                Text(
                    article.title,
                    modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.setCursorContext(articles.map { it.id })
                        onOpenArticle(article.id)
                    }.padding(vertical = 10.dp),
                    maxLines = 2,
                )
            }
        }
    }
}
