package gr.thrylos.news.reader.media

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun MediaViewerScreen(
    onBack: () -> Unit,
    viewModel: MediaViewerViewModel = hiltViewModel(),
) {
    val media by viewModel.media.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (media.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val pagerState = rememberPagerState(initialPage = viewModel.startIndex.coerceIn(0, media.lastIndex)) { media.size }
    var pendingSaveUrl by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val url = pendingSaveUrl
        pendingSaveUrl = null
        if (granted && url != null) {
            scope.launch {
                val result = MediaSaver.saveToGallery(context, url, "thrylos_${System.currentTimeMillis()}")
                Toast.makeText(context, if (result.isSuccess) "Αποθηκεύτηκε στη Συλλογή" else "Αποτυχία αποθήκευσης", Toast.LENGTH_SHORT).show()
            }
        } else if (!granted) {
            Toast.makeText(context, "Χρειάζεται άδεια αποθήκευσης", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestSave(url: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            pendingSaveUrl = url
            permissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            scope.launch {
                val result = MediaSaver.saveToGallery(context, url, "thrylos_${System.currentTimeMillis()}")
                Toast.makeText(context, if (result.isSuccess) "Αποθηκεύτηκε στη Συλλογή" else "Αποτυχία αποθήκευσης", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (val item = media[page]) {
                is MediaItem.Photo -> ZoomableImage(item.url, item.caption, Modifier.fillMaxSize())
                is MediaItem.Clip -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (item.thumbnailUrl != null) {
                            AsyncImage(model = item.thumbnailUrl, contentDescription = item.caption, modifier = Modifier.fillMaxWidth())
                        }
                        IconButton(
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) },
                            modifier = Modifier.padding(top = 16.dp),
                        ) {
                            Icon(Icons.Filled.PlayCircle, contentDescription = "Αναπαραγωγή video", tint = Color.White, modifier = Modifier.padding(4.dp))
                        }
                        item.caption?.let { Text(it, color = Color.White, modifier = Modifier.padding(top = 8.dp)) }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().systemBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Πίσω", tint = Color.White) }
            val current = media.getOrNull(pagerState.currentPage)
            if (current is MediaItem.Photo) {
                IconButton(onClick = { requestSave(current.url) }) {
                    Icon(Icons.Filled.Download, contentDescription = "Αποθήκευση", tint = Color.White)
                }
            } else {
                androidx.compose.foundation.layout.Spacer(Modifier)
            }
        }
    }
}
