package gr.thrylos.news.reader.media

import gr.thrylos.news.model.Article
import gr.thrylos.news.model.ContentBlock

sealed class MediaItem {
    data class Photo(val url: String, val caption: String?) : MediaItem()
    data class Clip(val url: String, val thumbnailUrl: String?, val caption: String?) : MediaItem()
}

fun Article.mediaItems(): List<MediaItem> = content.mapNotNull { block ->
    when (block) {
        is ContentBlock.Image -> MediaItem.Photo(block.url, block.caption)
        is ContentBlock.Video -> MediaItem.Clip(block.url, block.thumbnailUrl, block.caption)
        else -> null
    }
}
