package gr.thrylos.news.reader.media

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/** Downloads an already-displayed article image and writes it into the device's
 *  public Pictures gallery, so "save this photo" works like it would from any
 *  other app. */
object MediaSaver {

    suspend fun saveToGallery(context: Context, imageUrl: String, displayName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = URL(imageUrl).openStream().use { it.readBytes() }
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Thrylos News")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("Αποτυχία δημιουργίας αρχείου")
                resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Αποτυχία εγγραφής")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                    resolver.update(uri, done, null, null)
                }
            }
        }
}
