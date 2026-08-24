package gr.thrylos.news.reader.media

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f

/** Pinch-to-zoom + pan + double-tap to reset, matching the e-reader's "open the photo,
 *  zoom in" requirement without pulling in an external zoom-image dependency.
 *
 *  Only consumes touch input for a genuine pinch (2+ pointers) or once the image is
 *  already zoomed in — a plain single-finger swipe at 1x scale is left unconsumed so
 *  it still reaches the surrounding HorizontalPager to swipe between media items.
 *  (detectTransformGestures consumes every single-finger pan too, which silently
 *  blocked paging past the first photo in an article's gallery.) */
@Composable
fun ZoomableImage(url: String, contentDescription: String?, modifier: Modifier = Modifier) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY,
            )
            .pointerInput(url) {
                awaitEachGesture {
                    var event = awaitPointerEvent()
                    while (event.changes.any { it.pressed }) {
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val isPinch = event.changes.size > 1
                        if (isPinch || zoomChange != 1f || scale > MIN_SCALE) {
                            val newScale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
                            scale = newScale
                            if (newScale <= MIN_SCALE) {
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                offsetX += panChange.x
                                offsetY += panChange.y
                            }
                            event.changes.forEach { change -> if (change.positionChanged()) change.consume() }
                        }
                        event = awaitPointerEvent()
                    }
                }
            },
    )
}
